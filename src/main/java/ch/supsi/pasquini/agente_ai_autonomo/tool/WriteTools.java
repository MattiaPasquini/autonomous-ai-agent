package ch.supsi.pasquini.agente_ai_autonomo.tool;

import ch.supsi.pasquini.agente_ai_autonomo.ai.AiTool;
import ch.supsi.pasquini.agente_ai_autonomo.ai.AiToolParam;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.PathValidator;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.SandboxViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class WriteTools implements FilesystemTools{

    private final PathValidator pathValidator;

    public WriteTools(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @AiTool(description = "Overwrite a file with the given content, replacing any existing content")
    public String overwriteFile(
            @AiToolParam(description = "Path of the file to write") String targetFilePath,
            @AiToolParam(description = "Content to write to the file") String content
    ) {
        Path targetFile = Paths.get(targetFilePath);
        try {
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        try {
            Files.writeString(
                    targetFile,
                    content,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            return String.format("Successfully wrote %d characters to '%s'", content.length(), targetFilePath);

        } catch (NoSuchFileException e) {
            return String.format("File '%s' does not exist", targetFilePath);
        } catch (IOException e) {
            return "Something went wrong while writing the file";
        }
    }

    @AiTool(description = "Append the text content in a file")
    public String appendContent(
            @AiToolParam(description = "Path of the file to write") String targetFilePath,
            @AiToolParam(description = "Content to write to the file") String content
    ) {
        Path targetFile = Paths.get(targetFilePath);
        try {
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        try {
            Files.writeString(
                    targetFile,
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );

            return String.format("Successfully appended %d characters to '%s'", content.length(), targetFilePath);

        } catch (NoSuchFileException e) {
            return String.format("File '%s' does not exist", targetFilePath);
        } catch (IOException e) {
            return "Something went wrong while appending to the file";
        }
    }

    @AiTool(description = "Insert a new line of content BEFORE the line at the given number, shifting existing lines down. " +
            "Example: if the file has lines [A, B, C] and you call writeAtLine(2, 'X'), the result is [A, X, B, C]. " +
            "Use replaceLine instead if you want to overwrite an existing line rather than insert a new one.")
    public String insertAtLine(
            @AiToolParam(description = "Path of the file to write") String targetFilePath,
            @AiToolParam(description = "Line number where to insert the content (1-based)") int lineNumber,
            @AiToolParam(description = "Content to write at the specified line") String content
    ) {
        Path targetFile = Paths.get(targetFilePath);
        try {
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        if (lineNumber < 1) {
            return "Line number must be >= 1";
        }

        List<String> lines;
        try {
            lines = new ArrayList<>(Files.readAllLines(targetFile));
        } catch (NoSuchFileException e) {
            return String.format("File '%s' does not exist", targetFilePath);
        } catch (IOException e) {
            return "Something went wrong while reading the file";
        }

        int index = lineNumber - 1;
        if (index > lines.size()) {
            return String.format(
                    "Line number %d is out of bounds (file has %d lines)",
                    lineNumber, lines.size()
            );
        }

        lines.add(index, content);

        try {
            Files.write(targetFile, lines, StandardOpenOption.TRUNCATE_EXISTING);
            return String.format("Successfully wrote content at line %d of '%s'", lineNumber, targetFilePath);
        } catch (IOException e) {
            return "Something went wrong while writing to the file";
        }
    }

}
