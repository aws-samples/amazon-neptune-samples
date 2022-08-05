package software.amazon.neptune.onegraph.playground.server.service;

import lombok.NonNull;
import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.state.State;

import java.io.ByteArrayOutputStream;

/**
 * Service that handles the view command.
 */
public class ViewService extends Service {

    /**
     * All problems that occur during operations performed by view are thrown as
     * {@link ViewException} containing a message to help the user fix the issue.
     */
    public static class ViewException extends RuntimeException {

        /**
         * Creates a new view exception with the given message and cause.
         * @param message The exception message seen by the user.
         * @param cause The exception that caused this exception.
         */
        public ViewException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public final ExportService exportService;

    /**
     * Initialize the view service with the given state.
     * @param state The state.
     */
    public ViewService(State state, ExportService exportService) {
        super(state);
        this.exportService = exportService;
    }

    /**
     * View the current OG data set in {@link State} in the given data format.
     * @return A string containing the data set in the given data format.
     * @throws ViewException When an exception occurs during the serializing of the data set in the given format.
     */
    public String viewOGDatasetInDataFormat(@NonNull DataFormat dataFormat) throws ViewException {

        try {
            ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream();

            if (dataFormat == DataFormat.NEPTUNECSV) {
                ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
                this.exportService.exportData(dataFormat, outputStream1, outputStream2);
                return String.format(" ---- NODES ---- \n %s \n ---- EDGES ----\n %s",
                        outputStream1,
                        outputStream2);
            } else {
                this.exportService.exportData(dataFormat, outputStream1, null);
                return outputStream1.toString();
            }
        } catch (Exception e) {
            throw new ViewException("An exception occurred during view", e);
        }
    }
}
