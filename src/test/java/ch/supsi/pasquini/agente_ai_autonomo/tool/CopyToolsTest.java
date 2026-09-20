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
public class CopyToolsTest {

    @Mock
    private PathValidator pathValidator;

    private CopyTools copyTools;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setup() {
        copyTools = new CopyTools(pathValidator);
    }

    // --- copyFile ---

    @Test
    void copyFileSuccess() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        Files.writeString(source, "hello");
        when(pathValidator.resolveSandboxPath(any()))
                .thenReturn(source)
                .thenReturn(target);

        String result = copyTools.copyFile("source.txt", "target.txt");

        assertEquals("Copied 'source.txt' to 'target.txt'", result);
        assertTrue(Files.exists(target));
        assertEquals("hello", Files.readString(target));
    }

    @Test
    void copyFileIntoExistingDirectory() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path targetDir = tempDir.resolve("targetDir");
        Path finalTarget = targetDir.resolve("source.txt");
        Files.writeString(source, "data");
        Files.createDirectory(targetDir);
        when(pathValidator.resolveSandboxPath(any()))
                .thenReturn(source)
                .thenReturn(targetDir)
                .thenReturn(finalTarget);

        String result = copyTools.copyFile("source.txt", "targetDir");

        assertEquals("Copied 'source.txt' to 'targetDir'", result);
        assertTrue(Files.exists(finalTarget));
    }

    @Test
    void copyDirectoryRecursivelySuccess() throws IOException {
        Path sourceDir = tempDir.resolve("src");
        Path sub = sourceDir.resolve("sub");
        Files.createDirectories(sub);
        Files.writeString(sourceDir.resolve("a.txt"), "a");
        Files.writeString(sub.resolve("b.txt"), "b");
        Path targetDir = tempDir.resolve("dst");
        when(pathValidator.resolveSandboxPath(any()))
                .thenReturn(sourceDir)
                .thenReturn(targetDir)
                .thenAnswer(inv -> inv.getArgument(0));

        String result = copyTools.copyFile("src", "dst");

        assertEquals("Copied 'src' to 'dst'", result);
        assertTrue(Files.exists(targetDir.resolve("a.txt")));
        assertTrue(Files.exists(targetDir.resolve("sub").resolve("b.txt")));
    }

    @Test
    void copyFileSourceNotFound() {
        Path nonExistent = tempDir.resolve("ghost.txt");
        Path target = tempDir.resolve("target.txt");
        when(pathValidator.resolveSandboxPath(any()))
                .thenReturn(nonExistent)
                .thenReturn(target);

        String result = copyTools.copyFile("ghost.txt", "target.txt");

        assertTrue(result.contains("does not exist"));
    }

    @Test
    void copyFileDestinationAlreadyExists() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        Files.writeString(source, "src");
        Files.writeString(target, "existing");
        when(pathValidator.resolveSandboxPath(any()))
                .thenReturn(source)
                .thenReturn(target);

        String result = copyTools.copyFile("source.txt", "target.txt");

        assertEquals("Cannot copy: destination 'target.txt' already exists", result);
        assertEquals("existing", Files.readString(target));
    }

    @Test
    void copyFilePermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = copyTools.copyFile("../etc/passwd", "target.txt");

        assertEquals("Permission denied: path outside sandbox", result);
    }

    // --- moveFileOrDirectory ---

    @Test
    void moveFileSuccess() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        Files.writeString(source, "content");
        when(pathValidator.resolveSandboxPath(any()))
                .thenReturn(source)
                .thenReturn(target);

        String result = copyTools.moveFileOrDirectory("source.txt", "target.txt");

        assertEquals("File moved from 'source.txt' to 'target.txt'", result);
        assertFalse(Files.exists(source));
        assertTrue(Files.exists(target));
        assertEquals("content", Files.readString(target));
    }

    @Test
    void moveDirectorySuccess() throws IOException {
        Path sourceDir = tempDir.resolve("srcDir");
        Path targetDir = tempDir.resolve("dstDir");
        Files.createDirectory(sourceDir);
        Files.writeString(sourceDir.resolve("file.txt"), "data");
        when(pathValidator.resolveSandboxPath(any()))
                .thenReturn(sourceDir)
                .thenReturn(targetDir);

        String result = copyTools.moveFileOrDirectory("srcDir", "dstDir");

        assertEquals("File moved from 'srcDir' to 'dstDir'", result);
        assertFalse(Files.exists(sourceDir));
        assertTrue(Files.exists(targetDir.resolve("file.txt")));
    }

    @Test
    void moveFileSourceNotFound() {
        Path nonExistent = tempDir.resolve("ghost.txt");
        Path target = tempDir.resolve("target.txt");
        when(pathValidator.resolveSandboxPath(any()))
                .thenReturn(nonExistent)
                .thenReturn(target);

        String result = copyTools.moveFileOrDirectory("ghost.txt", "target.txt");

        assertTrue(result.contains("does not exist"));
    }

    @Test
    void moveFileDestinationAlreadyExists() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        Files.writeString(source, "src");
        Files.writeString(target, "existing");
        when(pathValidator.resolveSandboxPath(any()))
                .thenReturn(source)
                .thenReturn(target);

        String result = copyTools.moveFileOrDirectory("source.txt", "target.txt");

        assertEquals("Cannot move file: destination 'target.txt' already exists", result);
        assertTrue(Files.exists(source));
        assertEquals("existing", Files.readString(target));
    }

    @Test
    void moveFilePermissionDenied() {
        when(pathValidator.resolveSandboxPath(any()))
                .thenThrow(new SandboxViolationException("path outside sandbox"));

        String result = copyTools.moveFileOrDirectory("../etc/passwd", "target.txt");

        assertEquals("Permission denied: path outside sandbox", result);
    }

}
