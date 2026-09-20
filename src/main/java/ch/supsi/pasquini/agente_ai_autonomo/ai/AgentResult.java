package ch.supsi.pasquini.agente_ai_autonomo.ai;

public class AgentResult {

    public enum Status {
        COMPLETED,
        NEED_CONFIRMATION
    }

    private final Status status;
    private final String text;

    public AgentResult(Status status, String text) {
        this.status = status;
        this.text = text;
    }

    public boolean isCompleted() {
        return this.status == Status.COMPLETED;
    }

    public boolean needsConfirmation() {
        return this.status == Status.NEED_CONFIRMATION;
    }

    public String getText() {
        return text;
    }
}
