package ch.supsi.pasquini.agente_ai_autonomo.ai;

import java.util.List;

public interface AgentClient {

    AgentResult chat(String prompt);

    AgentResult confirm(boolean executeAction);


}
