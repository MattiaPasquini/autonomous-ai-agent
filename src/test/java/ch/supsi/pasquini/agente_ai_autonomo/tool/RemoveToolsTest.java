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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RemoveToolsTest {

    @Mock
    private PathValidator pathValidator;

    private RemoveTools removeTools;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setup() {
        removeTools = new RemoveTools(pathValidator);
    }

    @Test
    void deleteFileSuccess() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = removeTools.deleteFileOrDirectory("file.txt");

        assertEquals("File 'file.txt' deleted", result);
        assertFalse(Files.exists(file));
    }

    @Test
    void deleteEmptyDirectorySuccess() throws IOException {
        Path dir = tempDir.resolve("emptyDir");
        Files.createDirectory(dir);
        when(pathValidator.resolveSandboxPath(any())).thenReturn(dir);

        String result = removeTools.deleteFileOrDirectory("emptyDir");

        assertEquals("File 'emptyDir' deleted", result);
        assertFalse(Files.exists(dir));
    }

    @Test
    void deleteFileNotFound() {
        Path nonExistent = tempDir.resolve("ghost.txt");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(nonExistent);

        String result = removeTools.deleteFileOrDirectory("ghost.txt");

        assertEquals("File 'ghost.txt' does not exist", result);
    }

    @Test
    void deleteDirectoryNotEmpty() throws IOException {
        Path dir = tempDir.resolve("nonempty");
        Files.createDirectory(dir);
        Files.createFile(dir.resolve("child.txt"));
        when(pathValidator.resolveSandboxPath(any())).thenReturn(dir);

        String result = removeTools.deleteFileOrDirectory("nonempty");

        assertEquals("Cannot delete 'nonempty': directory is not empty", result);
        assertTrue(Files.exists(dir));
    }

    @Test
    void deleteFileOrDirectoryPermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = removeTools.deleteFileOrDirectory("../etc/passwd");

        assertEquals("Permission denied: path outside sandbox", result);
    }

    @Test
    void deleteDirectoryRecursivelySuccess() throws IOException {
        Path dir = tempDir.resolve("tree");
        Path sub = dir.resolve("sub");
        Files.createDirectories(sub);
        Files.createFile(sub.resolve("file.txt"));
        when(pathValidator.resolveSandboxPath(any())).thenReturn(dir);

        String result = removeTools.deleteDirectoryRecursively("tree");

        assertEquals("Directory 'tree' and its contents deleted", result);
        assertFalse(Files.exists(dir));
    }

    @Test
    void deleteDirectoryRecursivelyNotFound() {
        Path nonExistent = tempDir.resolve("ghost");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(nonExistent);

        String result = removeTools.deleteDirectoryRecursively("ghost");

        assertEquals("File 'ghost' does not exist", result);
    }

    @Test
    void deleteDirectoryRecursivelyNotADirectory() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = removeTools.deleteDirectoryRecursively("file.txt");

        assertEquals("'file.txt' is not a directory", result);
        assertTrue(Files.exists(file));
    }

    @Test
    void deleteDirectoryRecursivelyPermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = removeTools.deleteDirectoryRecursively("../etc");

        assertEquals("Permission denied: path outside sandbox", result);
    }

}
