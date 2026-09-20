package ch.supsi.pasquini.agente_ai_autonomo.tool;

import ch.supsi.pasquini.agente_ai_autonomo.ai.AiTool;
import ch.supsi.pasquini.agente_ai_autonomo.ai.AiToolParam;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.PathValidator;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.SandboxViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ReadTools implements FilesystemTools{

    private final PathValidator pathValidator;

    public ReadTools(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @AiTool(description = "Read the content of a file at the specified path")
    public String readFile(
            @AiToolParam(description = "Path of the file to read") String targetFilePath
    ) {
        Path targetFile = Paths.get(targetFilePath);
        try {
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        if (Files.isDirectory(targetFile)) {
            return "This is a directory and not a file.";
        }

        try {
            return Files.readString(targetFile);
        } catch (NoSuchFileException e) {
            return String.format("File '%s' does not exist", targetFilePath);
        } catch (IOException e) {
            return "Something went wrong while reading the file";
        }
    }

    @AiTool(description = "Read the first N lines of a file at the specified path")
    public String readFirstLines(
            @AiToolParam(description = "Path of the file to read") String targetFilePath,
            @AiToolParam(description = "Number of lines to read from the beginning of the file") int lineCount
    ) {
        Path targetFile = Paths.get(targetFilePath);
        try {
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        try (Stream<String> lines = Files.lines(targetFile)) {
            return lines.limit(lineCount).collect(Collectors.joining(System.lineSeparator()));
        } catch (NoSuchFileException e) {
            return String.format("File '%s' does not exist", targetFilePath);
        } catch (IOException e) {
            return "Something went wrong while reading the file";
        }
    }

    @AiTool(description = "Read the last N lines from a file")
    public String readLastNLines(
            @AiToolParam(description = "Path of the file to read") String targetFilePath,
            @AiToolParam(description = "Number of lines to read from the end of the file") int lineCount
    ) {
        Path targetFile = Paths.get(targetFilePath);
        try {
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        try {

            Deque<String> lastLines = new ArrayDeque<>(lineCount);
            try (Stream<String> lines = Files.lines(targetFile)) {
                for (Iterator<String> it = lines.iterator(); it.hasNext(); ) {
                    String line = it.next();
                    if (lastLines.size() == lineCount) {
                        lastLines.removeFirst();
                    }
                    lastLines.addLast(line);
                }
            }
            return String.join(System.lineSeparator(), lastLines);

        } catch (NoSuchFileException e) {
            return String.format("File '%s' does not exist", targetFilePath);
        } catch (IOException e) {
            return "Something went wrong while reading the file";
        }
    }
}
