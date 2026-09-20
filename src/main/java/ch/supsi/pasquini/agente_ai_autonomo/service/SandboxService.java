package ch.supsi.pasquini.agente_ai_autonomo.service;

import ch.supsi.pasquini.agente_ai_autonomo.exception.BadRequestException;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.PathValidator;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class SandboxService {

    private final PathValidator pathValidator;

    public SandboxService(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    public void createSandbox(String pathString) {
        Path path = Paths.get(pathString);
        if (pathValidator.getRootSandbox().isPresent()) {
            throw new BadRequestException("Sandbox already exists, please restart the application");
        }
        if (!Files.exists(path)) {
            throw new BadRequestException("Path does not exists");
        }
        if (!Files.isDirectory(path)) {
            throw new BadRequestException("The path is not a directory");
        }
        if (!Files.isReadable(path)) {
            throw new BadRequestException("The system do not have read permission in this directory");
        }
        if (!Files.isWritable(path)) {
            throw new BadRequestException("The system do not have write permission in this directory");
        }
        this.pathValidator.setRootSandbox(path);
    }

    public String getCurrentSandbox() {
        return this.pathValidator.getRootSandbox().map(Path::toString).orElse(null);
    }
}
