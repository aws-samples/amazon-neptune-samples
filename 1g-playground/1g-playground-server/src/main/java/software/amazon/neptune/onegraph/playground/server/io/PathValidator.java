package software.amazon.neptune.onegraph.playground.server.io;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility for validating user-supplied file paths.
 * <p>
 * The 1G Playground is a local developer tool, so users legitimately supply the location of their
 * own data and configuration files. Rather than confining access to a fixed directory (which would
 * break the tool's intended "read/write the file I named" behavior), this normalizes the path and
 * rejects any input containing a {@code ..} traversal segment. That mitigates path-injection
 * (CodeQL rule {@code java/path-injection}) from the HTTP endpoints without changing intended use.
 */
public final class PathValidator {

    private PathValidator() {
        // Utility class, not instantiable.
    }

    /**
     * Normalizes a user-supplied path and rejects path-traversal sequences.
     * @param userPath The raw path provided by the user.
     * @return The normalized {@link Path}.
     * @throws IllegalArgumentException If {@code userPath} contains a {@code ..} traversal segment.
     */
    public static Path normalizeUserPath(String userPath) throws IllegalArgumentException {
        Path normalized = Paths.get(userPath).normalize();
        for (Path segment : normalized) {
            if (segment.toString().equals("..")) {
                throw new IllegalArgumentException("Path must not contain '..' traversal segments");
            }
        }
        return normalized;
    }
}
