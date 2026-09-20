package ch.supsi.pasquini.agente_ai_autonomo.tool;

import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.PathValidator;
import ch.supsi.pasquini.agente_ai_autonomo.tool.helper.SandboxViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FindToolsTest {

    @Mock
    private PathValidator pathValidator;

    private FindTools findTools;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setup() {
        findTools = new FindTools(pathValidator);
    }

    @Test
    void listDirectorySuccess() throws IOException {
        Files.createFile(tempDir.resolve("file.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));
        when(pathValidator.resolveSandboxPath(any())).thenReturn(tempDir);

        String result = findTools.listDirectory(".");

        assertTrue(result.contains("file.txt"));
        assertTrue(result.contains("subdir/"));
    }

    @Test
    void listDirectoryEmpty() {
        when(pathValidator.resolveSandboxPath(any())).thenReturn(tempDir);

        String result = findTools.listDirectory(".");

        assertEquals("Directory '.' is empty", result);
    }

    @Test
    void listDirectoryNotFound() {
        Path nonExistent = tempDir.resolve("ghost");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(nonExistent);

        String result = findTools.listDirectory("ghost");

        assertEquals("File 'ghost' does not exist", result);
    }

    @Test
    void listDirectoryNotADirectory() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = findTools.listDirectory("file.txt");

        assertEquals("'file.txt' is not a directory", result);
    }

    @Test
    void listDirectoryPermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = findTools.listDirectory("../etc");

        assertEquals("Permission denied: path outside sandbox", result);
    }

}
