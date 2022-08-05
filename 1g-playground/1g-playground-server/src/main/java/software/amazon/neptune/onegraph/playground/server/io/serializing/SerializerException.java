package software.amazon.neptune.onegraph.playground.server.io.serializing;

import lombok.NonNull;

/**
 * All problems that occur during the serializing are thrown as
 * {@link SerializerException} containing a message to help the user fix the issue.
 */
public class SerializerException extends RuntimeException {

    /**
     * Creates a new serializer exception with the given message and cause.
     * @param message The exception message seen by the user.
     * @param cause The exception that caused this exception.
     */
    public SerializerException(@NonNull String message, @NonNull  Throwable cause) {
        super(message, cause);
    }
}
