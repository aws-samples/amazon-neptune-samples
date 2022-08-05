package software.amazon.neptune.onegraph.playground.client.response;

import software.amazon.neptune.onegraph.playground.client.api.ConfigType;

/**
 * Response format for GET requests to /settings.
 */
public class SettingsResponse {

    public SettingsResponse() {}

    /**
     * The type of LPG mapping configuration currently in the settings.
     */
    public ConfigType configType;

    /**
     * The query timeout in milliseconds currently in the settings.
     */
    public Long timeoutMillis;

    /**
     * True if the LPG mapping configuration currently in the settings is reversible.
     */
    public Boolean reversible;

    /**
     * The path the custom LPG mapping configuration was loaded from.
     */
    public String pathToConfig;
}
