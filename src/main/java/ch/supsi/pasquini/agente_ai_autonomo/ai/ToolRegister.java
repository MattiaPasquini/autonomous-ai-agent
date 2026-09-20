package ch.supsi.pasquini.agente_ai_autonomo.ai;

import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.util.*;

public class ToolRegister {

    private final Map<String, ToolDefinition> registry = new HashMap<>();

    public void registerTools(Object... classesWithTools) {
        for (Object classWithTool : classesWithTools) {
            Class<?> clazz = AopUtils.getTargetClass(classWithTool);
            for (Method method : clazz.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(AiTool.class)) continue;

                method.setAccessible(true);

                HumanInTheLoop hitlMethod = method.getAnnotation(HumanInTheLoop.class);
                String messageHumanInTheLoop = hitlMethod != null ? hitlMethod.description() : null;

                registry.put(method.getName(), new ToolDefinition(classWithTool, method, messageHumanInTheLoop));
                System.out.println("Method detected: " + method.getName());
            }
        }
    }

    public ToolDefinition get(String functionName) {
        return registry.get(functionName);
    }

    public Map<String, ToolDefinition> getAll() {
        Map<String, ToolDefinition> copy = new HashMap<>();
        for (Map.Entry<String, ToolDefinition> entry : this.registry.entrySet()) {
            copy.put(entry.getKey(), new ToolDefinition(entry.getValue()));
        }
        return copy;
    }


}