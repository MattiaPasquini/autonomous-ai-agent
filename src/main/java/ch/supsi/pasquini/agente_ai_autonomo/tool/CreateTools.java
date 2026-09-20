package ch.supsi.pasquini.agente_ai_autonomo.tool;

import ch.supsi.pasquini.agente_ai_autonomo.ai.AiTool;
import ch.supsi.pasquini.agente_ai_autonomo.ai.AiToolParam;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.PathValidator;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.SandboxViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;

@Component
public class CreateTools implements FilesystemTools{

    private final PathValidator pathValidator;

    public CreateTools(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @AiTool(description = "Create a file at the specified path")
    public String createFile(
            @AiToolParam(description = "Path of the file to be created") String targetFilePath
    ) {
        Path targetFile = Paths.get(targetFilePath);
        try {
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        try {
            Files.createFile(targetFile);
        } catch (FileAlreadyExistsException e) {
            return String.format("File '%s' already exists", targetFilePath);
        } catch (NoSuchFileException e) {
            return String.format("Cannot create file: parent directory does not exist for '%s'", targetFilePath);
        } catch (IOException e) {
            return "Something went wrong while creating the file";
        }

        return String.format("File '%s' created", targetFilePath);
    }

    @AiTool(description = "Create a directory at the specified path")
    public String createDirectory(
            @AiToolParam(description = "Path of the directory to be created") String targetDirPath
    ) {
        Path targetDir = Paths.get(targetDirPath);
        try {
            targetDir = this.pathValidator.resolveSandboxPath(targetDir);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        try {
            Files.createDirectory(targetDir);
        } catch (FileAlreadyExistsException e) {
            return String.format("Directory '%s' already exists", targetDirPath);
        } catch (NoSuchFileException e) {
            return String.format("Cannot create directory: parent directory does not exist for '%s'", targetDirPath);
        } catch (IOException e) {
            return "Something went wrong while creating the directory";
        }

        return String.format("Directory '%s' created", targetDirPath);
    }
}
