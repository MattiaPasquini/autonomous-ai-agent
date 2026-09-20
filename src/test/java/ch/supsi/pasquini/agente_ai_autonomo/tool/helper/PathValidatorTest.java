package ch.supsi.pasquini.agente_ai_autonomo.tool.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PathValidatorTest {

    @TempDir
    private Path tempDir;

    @Test
    void resolveSimplePathSuccess() {
        PathValidator validator = new PathValidator();
        validator.setRootSandbox(tempDir);

        Path result = validator.resolveSandboxPath(Path.of("file.txt"));

        assertEquals(tempDir.resolve("file.txt").normalize(), result);
    }

    @Test
    void resolveNestedPathSuccess() {
        PathValidator validator = new PathValidator();
        validator.setRootSandbox(tempDir);

        Path result = validator.resolveSandboxPath(Path.of("sub/file.txt"));

        assertEquals(tempDir.resolve("sub/file.txt").normalize(), result);
    }

    @Test
    void resolvePathTraversalThrows() {
        PathValidator validator = new PathValidator();
        validator.setRootSandbox(tempDir);

        SandboxViolationException ex = assertThrows(
                SandboxViolationException.class,
                () -> validator.resolveSandboxPath(Path.of("../../etc/passwd"))
        );

        assertEquals("Path traversal attempt: the resolved path is outside the sandbox directory", ex.getMessage());
    }

    @Test
    void resolveAbsolutePathOutsideSandboxThrows() {
        PathValidator validator = new PathValidator();
        validator.setRootSandbox(tempDir);
        Path outsidePath = tempDir.getParent().resolve("other.txt");

        assertThrows(
                SandboxViolationException.class,
                () -> validator.resolveSandboxPath(outsidePath)
        );
    }

    @Test
    void sandboxDirectoryMissingThrows() throws IOException {
        Path missingDir = tempDir.resolve("sandbox");
        Files.createDirectory(missingDir);
        PathValidator validator = new PathValidator();
        validator.setRootSandbox(missingDir);
        Files.delete(missingDir);

        SandboxViolationException ex = assertThrows(
                SandboxViolationException.class,
                () -> validator.resolveSandboxPath(Path.of("file.txt"))
        );

        assertEquals("Stop: Tell the user the sandbox directory does not exists anymore or not valid.", ex.getMessage());
    }

    @Test
    void sandboxIsFileNotDirectoryThrows() throws IOException {
        Path file = tempDir.resolve("notadir.txt");
        Files.createFile(file);
        PathValidator validator = new PathValidator();
        validator.setRootSandbox(file);

        assertThrows(
                SandboxViolationException.class,
                () -> validator.resolveSandboxPath(Path.of("file.txt"))
        );
    }

}
