package ch.supsi.pasquini.agente_ai_autonomo.tool;

import ch.supsi.pasquini.agente_ai_autonomo.ai.AiTool;
import ch.supsi.pasquini.agente_ai_autonomo.ai.AiToolParam;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.PathValidator;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.SandboxViolationException;
import com.google.genai.types.BatchJobOutputInfo;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Component
public class FindTools implements FilesystemTools {

    private PathValidator pathValidator;

    public FindTools(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
    }

    @AiTool(description = "List the files and subdirectories contained in a directory")
    public String listDirectory(
            @AiToolParam(description = "Path of the directory to list") String targetDirPath
    ) {

        Path targetDir = Paths.get(targetDirPath);
        try {
            targetDir = this.pathValidator.resolveSandboxPath(targetDir);
        } catch (SandboxViolationException e) {
            return String.format("Permission denied: %s", e.getMessage());
        }

        if (!Files.exists(targetDir)) {
            return String.format("File '%s' does not exist", targetDirPath);
        }

        if (!Files.isDirectory(targetDir)) {
            return String.format("'%s' is not a directory", targetDirPath);
        }

        try (Stream<Path> entries = Files.list(targetDir)) {
            List<String> names = entries
                    .sorted()
                    .map(path -> {
                        String name = path.getFileName().toString();
                        return Files.isDirectory(path) ? name + "/" : name;
                    })
                    .toList();

            if (names.isEmpty()) {
                return String.format("Directory '%s' is empty", targetDirPath);
            }
            return String.join("\n", names);
        } catch (IOException e) {
            return "Something went wrong while listing the directory";
        }
    }
}
