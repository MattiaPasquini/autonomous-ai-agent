package ch.supsi.pasquini.agente_ai_autonomo.service;

import ch.supsi.pasquini.agente_ai_autonomo.ai.AgentClient;
import ch.supsi.pasquini.agente_ai_autonomo.ai.AgentResult;
import ch.supsi.pasquini.agente_ai_autonomo.ai.gemini.AgentClientGemini;
import ch.supsi.pasquini.agente_ai_autonomo.dto.ChatResponse;
import ch.supsi.pasquini.agente_ai_autonomo.dto.ChatStatus;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

    private final AgentClient agentClient;

    public GeminiService(AgentClientGemini agentClientGemini) {
        this.agentClient = agentClientGemini;
    }

    public ChatResponse chat(String prompt) {
        AgentResult agentResult = this.agentClient.chat(prompt);
        return this.handleAgentResult(agentResult);
    }

    public ChatResponse confirm(boolean confirmation) {
        AgentResult agentResult = this.agentClient.confirm(confirmation);
        return this.handleAgentResult(agentResult);
    }

    private ChatResponse handleAgentResult(AgentResult agentResult) {
        if (agentResult.needsConfirmation()) {
            return new ChatResponse(
                    ChatStatus.CONFIRMATION_NEEDED,
                    agentResult.getText()
            );
        }
        return new ChatResponse(
                ChatStatus.COMPLETED,
                agentResult.getText()
        );
    }
}
