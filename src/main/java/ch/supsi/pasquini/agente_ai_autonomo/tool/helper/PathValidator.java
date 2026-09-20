package ch.supsi.pasquini.agente_ai_autonomo.tool.helper;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class PathValidator {

    private Path rootSandbox;

    public Optional<Path> getRootSandbox() {
        return Optional.ofNullable(this.rootSandbox);
    }

    public void setRootSandbox(Path path) {
        this.rootSandbox = path.toAbsolutePath().normalize();
    }

    private boolean isSandboxExistsAndHealthy() {
         return this.rootSandbox != null && Files.isDirectory(this.rootSandbox, LinkOption.NOFOLLOW_LINKS);
    }

    public Path resolveSandboxPath(Path pathInput) {
        if (!isSandboxExistsAndHealthy()) {
            throw new SandboxViolationException(
                    "Stop: Tell the user the sandbox directory does not exists anymore or not valid."
            );
        }

        Path path = this.rootSandbox.resolve(pathInput).normalize();

        if(!path.startsWith(this.rootSandbox)) {
            throw new SandboxViolationException(
                    "Path traversal attempt: the resolved path is outside the sandbox directory"
            );
        }
        return path;
    }

}
