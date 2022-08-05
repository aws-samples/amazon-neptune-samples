package software.amazon.neptune.onegraph.playground.server.service;

import lombok.NonNull;
import software.amazon.neptune.onegraph.playground.server.state.State;

/**
 * Service that handles the clear command.
 */
public class ClearService extends Service {

    private final String INFO_NO_DATA_TO_CLEAR = "There was no data to clear.";
    private final String INFO_DATA_CLEARED = "All data cleared.";

    /**
     * Initialize the clear service with the given state.
     * @param state The state.
     */
    public ClearService(State state) {
        super(state);
    }

    /**
     * Clears all data currently in the {@link #state}, but not the {@code lpgMappingConfiguration}.
     * @param statusBuilder Logs information about the data that was cleared to this builder.
     */
    public void clearData(@NonNull StringBuilder statusBuilder) {
        boolean wasData = this.clearData();

        if (wasData) {
            statusBuilder.append(INFO_DATA_CLEARED);
        } else {
            statusBuilder.append(INFO_NO_DATA_TO_CLEAR);
        }
    }

    /**
     * Clears all data currently in the {@link #state}, but not the {@code lpgMappingConfiguration}.
     * @return True if there was data to clear, false otherwise.
     */
    public boolean clearData() {
        return this.state.clear();
    }
}
