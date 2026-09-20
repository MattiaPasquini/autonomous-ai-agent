package ch.supsi.pasquini.agente_ai_autonomo.controller;

import ch.supsi.pasquini.agente_ai_autonomo.dto.ChatRequest;
import ch.supsi.pasquini.agente_ai_autonomo.dto.ChatResponse;
import ch.supsi.pasquini.agente_ai_autonomo.dto.ConfirmRequest;
import ch.supsi.pasquini.agente_ai_autonomo.service.GeminiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AgentAIController {

    private final GeminiService geminiService;

    public AgentAIController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/ask")
    public ChatResponse getMessage(@RequestBody ChatRequest request) {
        return this.geminiService.chat(request.prompt());
    }

    @PostMapping("/confirm")
    public ChatResponse confirm(@RequestBody ConfirmRequest request) {
        return this.geminiService.confirm(request.confirmation());
    }

}
