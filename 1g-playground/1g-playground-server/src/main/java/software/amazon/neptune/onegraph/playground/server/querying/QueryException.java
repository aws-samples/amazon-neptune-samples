package software.amazon.neptune.onegraph.playground.server.querying;

/**
 * All problems that occur during querying are thrown as
 * {@link QueryException} containing a message to help the user fix the issue.
 */
public class QueryException extends RuntimeException {
    /**
     * Creates a new query exception with the given message and cause.
     * @param message The exception message seen by the user.
     * @param cause The exception that caused this exception.
     */
    QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
