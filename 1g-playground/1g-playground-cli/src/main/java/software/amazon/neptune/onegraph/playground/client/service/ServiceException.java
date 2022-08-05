package software.amazon.neptune.onegraph.playground.client.service;

/**
 * Exceptions that occur during operations of any {@code Service} are thrown as {@link ServiceException},
 * the message is displayed directly to the user.
 */
public class ServiceException extends RuntimeException {
    private static final String causeLiteral = "CAUSE: ";
    private static final String errorMessage = "An error occurred while executing the %s command, %s";

    /**
     * Create a new exception with a message for the user indicating what went wrong.
     * @param commandRan Which command was running when the error occurred.
     * @param cause The cause of the error.
     */
    public ServiceException(String commandRan, Throwable cause) {
        super(String.format(errorMessage, commandRan, getCausesChain(cause)), cause);
    }

    // Collects the messages of all causes in a single string.
    private static String getCausesChain(Throwable cause) {
        StringBuilder causes = new StringBuilder();
        causes.append(causeLiteral).append(cause.getMessage()).append("\n");

        for (Throwable t = cause.getCause(); t != null; t = t.getCause()) {
            causes.append(causeLiteral).append(t.getMessage()).append("\n");
        }
        return causes.toString();
    }
}
