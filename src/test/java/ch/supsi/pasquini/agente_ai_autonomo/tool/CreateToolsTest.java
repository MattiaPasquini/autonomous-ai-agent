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
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateToolsTest {

    @Mock
    private PathValidator pathValidator;

    private CreateTools createTools;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setup() {
        createTools = new CreateTools(pathValidator);
    }

    @Test
    void createFileSuccess() {
        String fileName = "test.txt";
        Path targetFile = tempDir.resolve(fileName);
        when(pathValidator.resolveSandboxPath(any())).thenReturn(targetFile);

        String result = createTools.createFile(fileName);

        assertEquals("File '" + fileName + "' created", result);
        assertTrue(Files.exists(targetFile));
    }

    @Test
    void createFileAlreadyExists() throws IOException {
        String fileName = "existing.txt";
        Path targetFile = tempDir.resolve(fileName);
        Files.createFile(targetFile);
        when(pathValidator.resolveSandboxPath(any())).thenReturn(targetFile);

        String result = createTools.createFile(fileName);

        assertEquals("File '" + fileName + "' already exists", result);
    }

    @Test
    void createFilePermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = createTools.createFile("../etc/passwd");

        assertEquals("Permission denied: path outside sandbox", result);
    }

    @Test
    void createDirectorySuccess() {
        String filename = "newDir";
        Path targetDir = tempDir.resolve(filename);
        when(pathValidator.resolveSandboxPath(any())).thenReturn(targetDir);

        String result = createTools.createDirectory(filename);

        assertEquals("Directory '" + filename + "' created", result);
        assertTrue(Files.exists(targetDir));
    }

    @Test
    void createDirectoryAlreadyExists() throws IOException {
        String filename = "existingDir";
        Path targetDir = tempDir.resolve("existingDir");
        Files.createDirectory(targetDir);
        when(pathValidator.resolveSandboxPath(any())).thenReturn(targetDir);

        String result = createTools.createDirectory(filename);

        assertEquals("Directory '" + filename + "' already exists", result);
    }

}
