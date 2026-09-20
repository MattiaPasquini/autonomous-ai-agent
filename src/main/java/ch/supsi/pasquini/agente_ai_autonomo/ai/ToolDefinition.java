package ch.supsi.pasquini.agente_ai_autonomo.ai;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ToolDefinition {

    private final Object instance;
    private final Method method;
    private final String humanInTheLoopMessage;

    public ToolDefinition(Object beanInstance, Method method, String humanInTheLoopMessage) {
        this.instance = Objects.requireNonNull(beanInstance, "beanInstance cannot be null");
        this.method = Objects.requireNonNull(method, "method cannot be null");
        this.humanInTheLoopMessage = humanInTheLoopMessage;
    }

    public ToolDefinition(ToolDefinition td) {
        this.instance = td.instance;
        this.method = td.method;
        this.humanInTheLoopMessage = td.humanInTheLoopMessage;
    }

    public Method getMethod() {
        return method;
    }

    public Optional<String> getHumanInTheLoopMessage() {
        return Optional.ofNullable(humanInTheLoopMessage);
    }

    public Object invoke(Map<String, Object> args) throws InvocationTargetException, IllegalAccessException {
        Object[] valueParameter = this.resolveParameters(args);
        return this.method.invoke(this.instance, valueParameter);
    }

    private Object[] resolveParameters(Map<String, Object> args) {
        Parameter[] parameters = this.method.getParameters();
        Object[] valueParameter = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            valueParameter[i] = args.get(paramName);
        }

        return valueParameter;
    }

    @Override
    public String toString() {
        return "ToolDefinition{" +
                "instance=" + instance +
                ", method=" + method +
                ", humanInTheLoop=" + humanInTheLoopMessage +
                '}';
    }
}
