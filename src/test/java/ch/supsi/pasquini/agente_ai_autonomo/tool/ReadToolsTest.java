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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadToolsTest {

    @Mock
    private PathValidator pathValidator;

    private ReadTools readTools;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setup() {
        readTools = new ReadTools(pathValidator);
    }

    @Test
    void readFileSuccess() throws IOException {
        Path file = tempDir.resolve("hello.txt");
        Files.writeString(file, "hello world");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = readTools.readFile("hello.txt");

        assertEquals("hello world", result);
    }

    @Test
    void readFileNotFound() {
        Path nonExistent = tempDir.resolve("ghost.txt");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(nonExistent);

        String result = readTools.readFile("ghost.txt");

        assertEquals("File 'ghost.txt' does not exist", result);
    }

    @Test
    void readFileIsDirectory() {
        when(pathValidator.resolveSandboxPath(any())).thenReturn(tempDir);

        String result = readTools.readFile(".");

        assertEquals("This is a directory and not a file.", result);
    }

    @Test
    void readFilePermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = readTools.readFile("../etc/passwd");

        assertEquals("Permission denied: path outside sandbox", result);
    }

    @Test
    void readFirstLinesSuccess() throws IOException {
        Path file = tempDir.resolve("multi.txt");
        Files.writeString(file, "line1\nline2\nline3\nline4");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = readTools.readFirstLines("multi.txt", 2);

        assertEquals("line1" + System.lineSeparator() + "line2", result);
    }

    @Test
    void readFirstLinesCountExceedsFile() throws IOException {
        Path file = tempDir.resolve("short.txt");
        Files.writeString(file, "only\ntwo");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = readTools.readFirstLines("short.txt", 10);

        assertEquals("only" + System.lineSeparator() + "two", result);
    }

    @Test
    void readFirstLinesNotFound() {
        Path nonExistent = tempDir.resolve("ghost.txt");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(nonExistent);

        String result = readTools.readFirstLines("ghost.txt", 5);

        assertEquals("File 'ghost.txt' does not exist", result);
    }

    @Test
    void readFirstLinesPermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = readTools.readFirstLines("../etc/passwd", 3);

        assertEquals("Permission denied: path outside sandbox", result);
    }

    @Test
    void readLastNLinesSuccess() throws IOException {
        Path file = tempDir.resolve("tail.txt");
        Files.writeString(file, "line1\nline2\nline3\nline4");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = readTools.readLastNLines("tail.txt", 2);

        assertEquals("line3" + System.lineSeparator() + "line4", result);
    }

    @Test
    void readLastNLinesCountExceedsFile() throws IOException {
        Path file = tempDir.resolve("short.txt");
        Files.writeString(file, "only\ntwo");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(file);

        String result = readTools.readLastNLines("short.txt", 10);

        assertEquals("only" + System.lineSeparator() + "two", result);
    }

    @Test
    void readLastNLinesNotFound() {
        Path nonExistent = tempDir.resolve("ghost.txt");
        when(pathValidator.resolveSandboxPath(any())).thenReturn(nonExistent);

        String result = readTools.readLastNLines("ghost.txt", 5);

        assertEquals("File 'ghost.txt' does not exist", result);
    }

    @Test
    void readLastNLinesPermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = readTools.readLastNLines("../etc/passwd", 3);

        assertEquals("Permission denied: path outside sandbox", result);
    }

}
