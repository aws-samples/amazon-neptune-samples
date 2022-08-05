package software.amazon.neptune.onegraph.playground.server.mapping;

import lombok.NonNull;

public class ConverterException extends RuntimeException {

    /**
     * Creates a new converter exception with the given message.
     * @param message The exception message seen by the user.
     */
    public ConverterException(@NonNull String message) {
        super(message);
    }
}
