package ch.supsi.pasquini.agente_ai_autonomo.ai;

//This is old version, the new version is AgentClientGemini
public class ChatClient {

    /*private static final Logger log = LoggerFactory.getLogger(ChatClientGemini.class);

    private final Chat chatSession;

    private final ToolRegister toolRegister;

    private FunctionPending functionPending;

    private ChatClientGemini(Chat chat, ToolRegister toolRegister) {
        this.chatSession = chat;
        this.toolRegister = toolRegister;
    }

    public static ChatClientGemini createChat(Client client, String model, Object... tools) {
        ToolRegister toolRegister = new ToolRegister();
        toolRegister.registerTools(tools);

        List<FunctionDeclaration> functionDeclarations = toolRegister.getDeclarations();
        Tool tool = Tool.builder().functionDeclarations(functionDeclarations).build();

        ThinkingConfig thinkingConfig = ThinkingConfig.builder()
                .thinkingLevel(ThinkingLevel.Known.MEDIUM)
                .includeThoughts(true)
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .thinkingConfig(thinkingConfig)
                .tools(tool)
                .build();

        Chat chatSession = client.chats.create(model, config);
        return new ChatClientGemini(chatSession, toolRegister);
    }

    public ChatResponse chat(String prompt) {
        log.info("Sending the prompt to LLM: {}", prompt);
        GenerateContentResponse response = this.chatSession.sendMessage(prompt);
        log.info("Response received");
        logThoughts(response);
        return handleResponse(response);
    }

    public ChatResponse confirmation(boolean confirmed) {
        if (this.functionPending == null) {
            return new ChatResponse(
                    ChatStatus.COMPLETED,
                    "No confirmation needed... There is no function pending."
            );
        }

        FunctionPending pending = this.functionPending;
        this.functionPending = null;

        FunctionCall functionCallPending = pending.functionCall();
        //List<Part> functionResponseParts = pending.collectedResponse();
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
            return processFunctionCalls(functionResponseParts, calls);
        }

        String functionName = functionNameOptional.get();
        Part functionResponsePart;

        if (confirmed) {
            log.info(
                    "User confirmed to execute a destructive tool: {}",
                    functionName
            );
            ToolDefinition tool = this.toolRegister.get(functionName);
            if (tool == null) {
                log.warn("Tool does not exist: {}", functionName);
                functionResponsePart =
                        createFunctionErrorResponsePart(
                                functionName,
                                new IllegalArgumentException(
                                        "Tool '" + functionName + "' does not exist"
                                )
                        );
            } else {
                functionResponsePart = invokeAndWrap(tool, functionCallPending);
            }
        } else {
            log.info("User denied execution of destructive tool {}", functionName);
            functionResponsePart = createDeniedResponsePart(functionName);
        }
        functionResponseParts.add(functionResponsePart);
        return processFunctionCalls(functionResponseParts, calls);
    }

    private ChatResponse handleResponse(GenerateContentResponse response) {
        ImmutableList<FunctionCall> responseCalls = response.functionCalls();

        if (responseCalls == null || responseCalls.isEmpty()) {
            log.info("No function calls required. Full response to user: {}", response.text());

            return new ChatResponse(
                    ChatStatus.COMPLETED,
                    response.text()
            );
        }

        Queue<FunctionCall> calls = new ArrayDeque<>(responseCalls);
        List<Part> functionResponseParts = new ArrayList<>();

        return processFunctionCalls(functionResponseParts, calls);
    }

    private ChatResponse processFunctionCalls(List<Part> functionResponseParts, Queue<FunctionCall> calls) {
        while (true) {
            while (!calls.isEmpty()) {
                FunctionCall functionCall = calls.poll();

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
                log.info("ACT: invoking the function {}", functionName);

                if (toolDefinition == null) {
                    log.warn("Tool does not exist: {}", functionName);

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
                            calls,
                            functionCall
                    );

                    return new ChatResponse(
                            ChatStatus.CONFIRMATION_NEEDED, message
                    );
                }

                functionResponseParts.add(
                        invokeAndWrap(toolDefinition, functionCall)
                );

            }

            log.info("Sending function results to the LLM");
            GenerateContentResponse response =
                    this.chatSession.sendMessage(
                            Content.fromParts(
                                    functionResponseParts.toArray(Part[]::new)
                            )
                    );

            log.info("Response received");
            logThoughts(response);

            if (response.functionCalls() == null || response.functionCalls().isEmpty()) {
                log.info("No function calls required anymore. Full response to user: {}", response.text());
                return new ChatResponse(
                        ChatStatus.COMPLETED,
                        response.text()
                );
            }

            calls = new ArrayDeque<>(response.functionCalls());
            functionResponseParts.clear();
        }
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
        return buildFunctionResponsePart(functionName, ImmutableMap.of("result", result));
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

    private void logThoughts(GenerateContentResponse response) {
        response.candidates().ifPresent(candidates -> {
            for (Candidate candidate : candidates) {
                candidate.content().flatMap(Content::parts).ifPresent(parts -> {
                    for (Part part : parts) {
                        if (part.thought().orElse(false) && part.text().isPresent()) {
                            log.info("THOUGHT: {}", part.text().get());
                        }
                    }
                });
            }
        });
    }*/
}
