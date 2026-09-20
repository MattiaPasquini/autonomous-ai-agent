package ch.supsi.pasquini.agente_ai_autonomo.tool;

import ch.supsi.pasquini.agente_ai_autonomo.ai.AiTool;
import ch.supsi.pasquini.agente_ai_autonomo.ai.AiToolParam;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.PathValidator;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.SandboxViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

@Component
public class CopyTools implements FilesystemTools {

    private final PathValidator pathValidator;

    public CopyTools(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @AiTool(description = "Copy a file or directory recursively from a source path to a destination path")
    public String copyFile(
            @AiToolParam(description = "Path of the file or directory to copy") String sourceFilePath,
            @AiToolParam(description = "Destination path for the copied file or directory") String targetFilePath
    ) {
        Path sourceFile = Paths.get(sourceFilePath);
        Path targetFile = Paths.get(targetFilePath);

        try {
            sourceFile = this.pathValidator.resolveSandboxPath(sourceFile);
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        try {
            if (Files.isDirectory(targetFile)) {
                targetFile = targetFile.resolve(sourceFile.getFileName());
                targetFile = this.pathValidator.resolveSandboxPath(targetFile);
            }

            if (Files.isDirectory(sourceFile)) {
                copyDirectoryRecursively(sourceFile, targetFile);
            } else {
                Files.copy(sourceFile, targetFile);
            }

        } catch (NoSuchFileException e) {
            return String.format(
                    "Cannot copy: source '%s' does not exist or parent directory of destination does not exist",
                    sourceFilePath
            );
        } catch (FileAlreadyExistsException e) {
            return String.format(
                    "Cannot copy: destination '%s' already exists",
                    targetFilePath
            );
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        } catch (IOException e) {
            return "Something went wrong while copying the file or directory";
        }

        return String.format(
                "Copied '%s' to '%s'",
                sourceFilePath,
                targetFilePath
        );
    }

    @AiTool(description = "Move a file or directory from a source path to a destination path")
    public String moveFileOrDirectory(
            @AiToolParam(description = "Path of the file to move") String sourceFilePath,
            @AiToolParam(description = "Destination path for the moved file") String targetFilePath
    ) {
        Path sourceFile = Paths.get(sourceFilePath);
        Path targetFile = Paths.get(targetFilePath);

        try {
            sourceFile = this.pathValidator.resolveSandboxPath(sourceFile);
            targetFile = this.pathValidator.resolveSandboxPath(targetFile);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        try {
            Files.move(sourceFile, targetFile);
        } catch (NoSuchFileException e) {
            return String.format(
                    "Cannot move file: source file '%s' does not exist or parent directory of destination does not exist",
                    sourceFilePath
            );
        } catch (FileAlreadyExistsException e) {
            return String.format(
                    "Cannot move file: destination '%s' already exists",
                    targetFilePath
            );
        } catch (IOException e) {
            return "Something went wrong while moving the file";
        }

        return String.format(
                "File moved from '%s' to '%s'",
                sourceFilePath,
                targetFilePath
        );
    }

    private void copyDirectoryRecursively(Path source, Path target)
            throws IOException, SandboxViolationException {

        try (Stream<Path> paths = Files.walk(source)) {
            for (Path sourcePath : paths.toList()) {
                Path relativePath = source.relativize(sourcePath);
                Path targetPath = target.resolve(relativePath);

                targetPath = this.pathValidator.resolveSandboxPath(targetPath);

                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath);
                }
            }
        }
    }

}
