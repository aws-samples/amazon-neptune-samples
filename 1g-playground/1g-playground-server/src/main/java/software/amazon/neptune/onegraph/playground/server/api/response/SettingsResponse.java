package software.amazon.neptune.onegraph.playground.server.api.response;

import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.state.State;

/**
 * Response format when client makes GET request to /settings.
 */
public class SettingsResponse {

    public SettingsResponse() {}

    /**
     * The type of LPG mapping configuration currently in {@link State}.
     */
    public ConfigType configType;

    /**
     * The query timeout in milliseconds currently in {@link State}.
     */
    public Long timeoutMillis;

    /**
     * True if the LPG mapping configuration currently in {@link State} is reversible.
     */
    public Boolean reversible;

    /**
     * The path the custom LPG mapping configuration was loaded from.
     */
    public String pathToConfig;
}
