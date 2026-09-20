package ch.supsi.pasquini.agente_ai_autonomo.controller;

import ch.supsi.pasquini.agente_ai_autonomo.dto.SandboxRequest;
import ch.supsi.pasquini.agente_ai_autonomo.dto.SandboxResponse;
import ch.supsi.pasquini.agente_ai_autonomo.service.SandboxService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sandbox")
public class SandboxController {

    private final SandboxService sandboxService;

    public SandboxController(SandboxService sandboxService) {
        this.sandboxService = sandboxService;
    }

    @GetMapping
    public SandboxResponse getWorkspace() {
        return new SandboxResponse(sandboxService.getCurrentSandbox());
    }

    @PostMapping("/new")
    public void setSandbox(@RequestBody SandboxRequest sandboxRequest) {
        sandboxService.createSandbox(sandboxRequest.path());
    }
}
