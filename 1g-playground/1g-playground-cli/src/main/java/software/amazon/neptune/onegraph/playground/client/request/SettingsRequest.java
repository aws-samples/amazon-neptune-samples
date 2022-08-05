package software.amazon.neptune.onegraph.playground.client.request;

import software.amazon.neptune.onegraph.playground.client.api.ConfigType;

/**
 * Expected content of the POST to /settings.
 */
public class SettingsRequest extends Request {

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
     * True if the mapping configuration should also be applied in reverse, false otherwise. Can be {@code null}.
     */
    public Boolean reversible;

    /**
     * Creates a new settings request with the given parameters.
     * @param configType Used to set the {@link SettingsRequest#configType}, can be {@code null}.
     * @param pathConfig  Used to set the {@link SettingsRequest#pathConfig}, can be {@code null}.
     * @param reversible  Used to set the {@link SettingsRequest#reversible}, can be {@code null}.
     * @param timeoutMillis  Used to set the {@link SettingsRequest#timeoutMillis}, can be {@code null}.
     */
    public SettingsRequest(ConfigType configType,
                           String pathConfig,
                           Boolean reversible,
                           Long timeoutMillis) {
        this.configType = configType;
        if (pathConfig != null &&  !pathConfig.isEmpty()) {
            this.pathConfig = pathConfig;
        }
        this.reversible = reversible;
        this.timeoutMillis = timeoutMillis;
    }

}
