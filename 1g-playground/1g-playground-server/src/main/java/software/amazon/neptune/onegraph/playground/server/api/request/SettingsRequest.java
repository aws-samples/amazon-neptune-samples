package software.amazon.neptune.onegraph.playground.server.api.request;

/**
 * Request format of client for settings command.
 */
public class SettingsRequest {

    public SettingsRequest() {}

    /**
     * Path to the configuration file, is only passed when {@link #configType} is set to {@code CUSTOM}.
     * can be {@code null}.
     */
    public String pathConfig;

    /**
     * The type of configuration being loaded, can be {@code null}.
     */
    public ConfigType configType;

    /**
     * The query timeout in milliseconds, can be {@code null}.
     */
    public Long timeoutMillis;

    /**
     * True if the mapping configuration should also be applied in reverse, can be {@code null}.
     */
    public Boolean reversible;
}
