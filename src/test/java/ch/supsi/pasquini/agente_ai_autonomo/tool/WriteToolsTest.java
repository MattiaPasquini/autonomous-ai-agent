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
public class WriteToolsTest {

    @Mock
    private PathValidator pathValidator;

    private WriteTools writeTools;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setup() {
        writeTools = new WriteTools(pathValidator);
    }

    @Test
    void overwriteFileSuccess() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "old content");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = writeTools.overwriteFile("file.txt", "new content");

        assertEquals("Successfully wrote 11 characters to 'file.txt'", result);
        assertEquals("new content", Files.readString(file));
    }

    @Test
    void overwriteFileNotFound() {
        Path nonExistent = tempDir.resolve("ghost.txt");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(nonExistent);

        String result = writeTools.overwriteFile("ghost.txt", "content");

        assertEquals("File 'ghost.txt' does not exist", result);
    }

    @Test
    void overwriteFilePermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = writeTools.overwriteFile("../etc/passwd", "content");

        assertEquals("Permission denied: path outside sandbox", result);
    }

    @Test
    void appendContentSuccess() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "hello");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = writeTools.appendContent("file.txt", " world");

        assertEquals("Successfully appended 6 characters to 'file.txt'", result);
        assertEquals("hello world", Files.readString(file));
    }

    @Test
    void appendContentCreatesFileIfNotExists() throws IOException {
        Path file = tempDir.resolve("new.txt");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = writeTools.appendContent("new.txt", "created");

        assertEquals("Successfully appended 7 characters to 'new.txt'", result);
        assertTrue(Files.exists(file));
        assertEquals("created", Files.readString(file));
    }

    @Test
    void appendContentPermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = writeTools.appendContent("../etc/passwd", "content");

        assertEquals("Permission denied: path outside sandbox", result);
    }

    @Test
    void insertAtLineSuccess() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.write(file, java.util.List.of("A", "B", "C"));
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = writeTools.insertAtLine("file.txt", 2, "X");

        assertEquals("Successfully wrote content at line 2 of 'file.txt'", result);
        assertEquals(java.util.List.of("A", "X", "B", "C"), Files.readAllLines(file));
    }

    @Test
    void insertAtLineAtBeginning() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.write(file, java.util.List.of("A", "B"));
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = writeTools.insertAtLine("file.txt", 1, "X");

        assertEquals("Successfully wrote content at line 1 of 'file.txt'", result);
        assertEquals(java.util.List.of("X", "A", "B"), Files.readAllLines(file));
    }

    @Test
    void insertAtLineAtEnd() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.write(file, java.util.List.of("A", "B"));
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = writeTools.insertAtLine("file.txt", 3, "X");

        assertEquals("Successfully wrote content at line 3 of 'file.txt'", result);
        assertEquals(java.util.List.of("A", "B", "X"), Files.readAllLines(file));
    }

    @Test
    void insertAtLineOutOfBounds() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.write(file, java.util.List.of("A", "B"));
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = writeTools.insertAtLine("file.txt", 10, "X");

        assertEquals("Line number 10 is out of bounds (file has 2 lines)", result);
    }

    @Test
    void insertAtLineInvalidLineNumber() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.createFile(file);
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = writeTools.insertAtLine("file.txt", 0, "X");

        assertEquals("Line number must be >= 1", result);
    }

    @Test
    void insertAtLineNotFound() {
        Path nonExistent = tempDir.resolve("ghost.txt");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(nonExistent);

        String result = writeTools.insertAtLine("ghost.txt", 1, "X");

        assertEquals("File 'ghost.txt' does not exist", result);
    }

    @Test
    void insertAtLinePermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = writeTools.insertAtLine("../etc/passwd", 1, "X");

        assertEquals("Permission denied: path outside sandbox", result);
    }

}
