package ch.supsi.pasquini.agente_ai_autonomo.tool;

import ch.supsi.pasquini.agente_ai_autonomo.ai.AiTool;
import ch.supsi.pasquini.agente_ai_autonomo.ai.AiToolParam;
import ch.supsi.pasquini.agente_ai_autonomo.ai.HumanInTheLoop;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.PathValidator;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.SandboxViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
public class RemoveTools implements FilesystemTools {

    private final PathValidator pathValidator;

    public RemoveTools(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @AiTool(description = "Delete a file or an empty directory at the specified path")
    @HumanInTheLoop(description = "The file/directory `{targetFilePath}` is about to be removed.")
    public String deleteFileOrDirectory(
            @AiToolParam(description = "Path of the file to delete") String targetFilePath
    ) {

        Path targetFile = Paths.get(targetFilePath);
        try {
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        try {
            Files.delete(targetFile);
        } catch (NoSuchFileException e) {
            return String.format("File '%s' does not exist", targetFilePath);
        } catch (DirectoryNotEmptyException e) {
            return String.format("Cannot delete '%s': directory is not empty", targetFilePath);
        } catch (IOException e) {
            return "Something went wrong while deleting the file";
        }

        return String.format("File '%s' deleted", targetFilePath);
    }

    @AiTool(description = "Delete a directory and all its contents, even if not empty")
    @HumanInTheLoop(description = "The directoy `{targetFilePath}` is about to be removed recursively.")
    public String deleteDirectoryRecursively(
            @AiToolParam(description = "Path of the directory to delete") String targetFilePath
    ) {

        Path targetDir = Paths.get(targetFilePath);
        try {
            targetDir = this.pathValidator.resolveSandboxPath(targetDir);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        if (!Files.exists(targetDir)) {
            return String.format("File '%s' does not exist", targetFilePath);
        }

        if (!Files.isDirectory(targetDir)) {
            return String.format("'%s' is not a directory", targetFilePath);
        }

        try (Stream<Path> walk = Files.walk(targetDir)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.delete(path);
            }
        } catch (IOException e) {
            return "Something went wrong while deleting the directory";
        }

        return String.format("Directory '%s' and its contents deleted", targetFilePath);
    }

}
