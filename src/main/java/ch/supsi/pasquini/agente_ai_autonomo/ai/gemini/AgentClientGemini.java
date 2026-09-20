package ch.supsi.pasquini.agente_ai_autonomo.ai.gemini;

import ch.supsi.pasquini.agente_ai_autonomo.ai.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.genai.Client;
import com.google.genai.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class AgentClientGemini implements AgentClient {

    private static final Logger log = LoggerFactory.getLogger(AgentClientGemini.class);

    private final Client client;

    private final String model;

    private final List<Content> history;

    private final ToolRegister toolRegister;

    private final GenerateContentConfig config;

    private FunctionPending functionPending = null;

    private AgentClientGemini(Client client, String model, GenerateContentConfig config, List<Content> history, ToolRegister toolRegister) {
        this.client = client;
        this.model = model;
        this.config = config;
        this.history = history;
        this.toolRegister = toolRegister;
    }

    //TODO: maybe do Builder instead of ts
    public static AgentClientGemini create(String model, String apiKey, Object... tools) {
        ToolRegister toolRegister = new ToolRegister();
        toolRegister.registerTools(tools);

        List<FunctionDeclaration> functionDeclarations = new LinkedList<>();
        for (ToolDefinition toolDefinition : toolRegister.getAll().values()) {
            functionDeclarations.add(buildFunctionDeclaration(toolDefinition.getMethod()));
        }
        Tool tool = Tool.builder().functionDeclarations(functionDeclarations).build();

        ThinkingConfig thinkingConfig = ThinkingConfig.builder()
                .thinkingLevel(ThinkingLevel.Known.MEDIUM)
                .includeThoughts(true)
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .thinkingConfig(thinkingConfig)
                .tools(tool)
                .build();

//        List<Content> contentHistory = history.stream()
//                .map( message ->
//                    Content.builder()
//                        .role(message.role() == Message.Role.User ? "user" : "model")
//                        .parts(List.of(Part.fromText(message.message())))
//                        .build())
//                .toList();

        List<Content> contentHistory = new ArrayList<>();

        Client client = Client.builder().apiKey(apiKey).build();

        return new AgentClientGemini(client, model, config, contentHistory, toolRegister);
    }

    @Override
    public AgentResult chat(String prompt) {
        history.add(Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(prompt)))
                .build());

        log.info("Sending the prompt to LLM: {}", prompt);
        GenerateContentResponse response = client.models.generateContent(this.model, this.history, this.config);

        Content content = response
                .candidates()
                .flatMap(candidates -> candidates.stream().findFirst())
                .flatMap(Candidate::content)
                .orElseThrow(
                        () -> new IllegalStateException("No content in model response")
                );

        this.history.add(content);

        log.info("Response received");

        ImmutableList<FunctionCall> functionCalls = response.functionCalls();

        if (functionCalls == null || functionCalls.isEmpty()) {
            log.info("No function calls required");
            return new AgentResult(AgentResult.Status.COMPLETED, response.text());
        }

        Queue<FunctionCall> functionCallsQueue = new ArrayDeque<>(functionCalls);

        return processFunctionCall(functionCallsQueue, new LinkedList<Part>());
    }

    private AgentResult processFunctionCall(Queue<FunctionCall> functionCallsQueue, List<Part> functionResponseParts) {
        while (true) {
            while (!functionCallsQueue.isEmpty()) {
                FunctionCall functionCall = functionCallsQueue.poll();

                Optional<String> functionNameOptional = functionCall.name();
                if (functionNameOptional.isEmpty()) {
                    functionResponseParts.add(
                            createFunctionErrorResponsePart(
                                    "unknown",
                                    new IllegalArgumentException("Function call without a name")
                            )
                    );
                    continue;
                }
                String functionName = functionNameOptional.get();

                ToolDefinition toolDefinition = toolRegister.get(functionName);
                if (toolDefinition == null) {
                    functionResponseParts.add(
                            createFunctionErrorResponsePart(
                                    functionName,
                                    new IllegalArgumentException(
                                            "Tool '" + functionName + "' does not exist"
                                    )
                            )
                    );
                    continue;
                }

                if (toolDefinition.getHumanInTheLoopMessage().isPresent()) {
                    log.info("Function {} requires Human In The Loop", functionName);

                    String message = toolDefinition.getHumanInTheLoopMessage().get();
                    Map<String, Object> args = functionCall.args().orElseGet(ImmutableMap::of);

                    for (Map.Entry<String, Object> entry : args.entrySet()) {
                        String placeholder = "{" + entry.getKey() + "}";
                        String value = String.valueOf(entry.getValue());
                        message = message.replace(placeholder, value);
                    }

                    this.functionPending = new FunctionPending(
                            functionResponseParts,
                            functionCall,
                            functionCallsQueue
                    );

                    return new AgentResult(
                            AgentResult.Status.NEED_CONFIRMATION, message
                    );
                }

                functionResponseParts.add(
                        invokeAndWrap(toolDefinition, functionCall)
                );

            }

            if (!functionResponseParts.isEmpty()) {
                this.history.add(
                        Content.builder()
                                .role("user")
                                .parts(functionResponseParts)
                                .build()
                );
            }

            log.info("Sending function results to the LLM");
            GenerateContentResponse response = client.models.generateContent(this.model, this.history, this.config);

            if (response.functionCalls() == null || response.functionCalls().isEmpty()) {
                return new AgentResult(AgentResult.Status.COMPLETED, response.text());
            }

            functionCallsQueue = new ArrayDeque<>(response.functionCalls());
            functionResponseParts.clear();
        }
    }

    @Override
    public AgentResult confirm(boolean executeAction) {
        if (this.functionPending == null) {
            throw new IllegalStateException("No pending state");
        }

        FunctionPending pending = this.functionPending;

        FunctionCall functionCallPending = pending.functionCall();
        List<Part> functionResponseParts = pending.collectedPart();
        Queue<FunctionCall> calls = pending.remainingCalls();

        Optional<String> functionNameOptional = functionCallPending.name();
        if (functionNameOptional.isEmpty()) {
            functionResponseParts.add(
                    createFunctionErrorResponsePart(
                            "-",
                            new IllegalArgumentException(
                                    "Function call without a name"
                            )
                    )
            );
            return processFunctionCall(calls, functionResponseParts);
        }

        String functionName = functionNameOptional.get();

        if (executeAction) {

            log.info(
                    "User confirmed to execute a destructive tool: {}",
                    functionName
            );

            ToolDefinition tool = this.toolRegister.get(functionName);
            if (tool == null) {
                log.warn("Tool does not exist: {}", functionName);
                functionResponseParts.add(
                        createFunctionErrorResponsePart(
                                functionName,
                                new IllegalArgumentException(
                                        "Tool '" + functionName + "' does not exist"
                                )
                        )
                );
            } else {
                functionResponseParts.add(invokeAndWrap(tool, functionCallPending));
            }

        } else {

            log.info("User denied execution of destructive tool {}", functionName);
            functionResponseParts.add(createDeniedResponsePart(functionName));

        }

        return processFunctionCall(calls, functionResponseParts);
    }

    private static FunctionDeclaration buildFunctionDeclaration(Method method) {
        AiTool annMethod = method.getAnnotation(AiTool.class);
        String descriptionMethod = annMethod.description();

        Map<String, Schema> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter parameter : method.getParameters()) {
            String parameterName = parameter.getName();

            Schema.Builder schemaBuilder = Schema
                    .builder()
                    .type(mapJavaTypeToSchemaType(parameter.getType()));

            if (parameter.isAnnotationPresent(AiToolParam.class)) {
                AiToolParam annParam = parameter.getAnnotation(AiToolParam.class);
                schemaBuilder.description(annParam.description());
            }

            required.add(parameterName);
            properties.put(parameterName, schemaBuilder.build());
        }

        Schema parametersSchema = Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(properties)
                .required(required)
                .build();

        return FunctionDeclaration.builder()
                .name(method.getName())
                .description(descriptionMethod)
                .parameters(parametersSchema)
                .build();
    }

    private static Type.Known mapJavaTypeToSchemaType(Class<?> type) {
        if (type == String.class) {
            return Type.Known.STRING;
        }

        if (type == int.class || type == Integer.class ||
                type == long.class || type == Long.class) {
            return Type.Known.INTEGER;
        }

        if (type == float.class || type == Float.class ||
                type == double.class || type == Double.class) {
            return Type.Known.NUMBER;
        }

        if (type == boolean.class || type == Boolean.class) {
            return Type.Known.BOOLEAN;
        }

        if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            return Type.Known.ARRAY;
        }

        return Type.Known.OBJECT;
    }

    private Part invokeAndWrap(ToolDefinition tool, FunctionCall fc) {
        String functionName = fc.name().orElseThrow();
        try {
            Map<String, Object> args = fc.args().orElseGet(ImmutableMap::of);
            Object result = tool.invoke(args);
            log.info("OBSERVE: function: {} - Returns: {}", functionName, result);
            return createFunctionResponsePart(functionName, result);
        } catch (InvocationTargetException e) { //eccezione quando nei metodi invocati lanciano un eccezione
            Throwable cause = e.getCause();
            return createFunctionErrorResponsePart(functionName, cause != null ? cause : e);
        } catch (IllegalAccessException e) {
            return createFunctionErrorResponsePart(functionName, new RuntimeException("Impossible to execute the tool"));
        }
    }

    private Part buildFunctionResponsePart(String functionName, Map<String, Object> response) {
        return Part.builder()
                .functionResponse(
                        FunctionResponse.builder()
                                .name(functionName != null ? functionName : "unknown")
                                .response(response)
                                .build()
                )
                .build();
    }

    private Part createFunctionResponsePart(String functionName, Object result) {
        return buildFunctionResponsePart(functionName, ImmutableMap.of("Result of the function call: ", result));
    }

    private Part createFunctionErrorResponsePart(String functionName, Throwable error) {
        return buildFunctionResponsePart(functionName, ImmutableMap.of(
                "errorMessage", error.getMessage(),
                "errorType", error.getClass().getSimpleName()
        ));
    }

    private Part createDeniedResponsePart(String functionName) {
        return buildFunctionResponsePart(functionName, ImmutableMap.of(
                "status", "denied",
                "message", "There is a human-in-the-loop mechanism and before the function is being invoked" +
                        "the system asked the user if can be invoked or not. User decided to not execute this function this time."

        ));
    }
}
