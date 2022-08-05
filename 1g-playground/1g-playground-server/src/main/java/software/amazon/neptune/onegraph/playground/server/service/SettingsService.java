package software.amazon.neptune.onegraph.playground.server.service;

import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.api.response.SettingsResponse;
import software.amazon.neptune.onegraph.playground.server.io.parsing.ConfigurationParser;
import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.state.State;
import lombok.NonNull;

import java.nio.file.Paths;

/**
 * Service that handles the config command.
 */
public class SettingsService extends Service {

    /**
     * All problems that occur during operations performed by view are thrown as
     * {@link SettingsException} containing a message to help the user fix the issue.
     */
    public static class SettingsException extends RuntimeException {

        /**
         * Creates a new config exception with the given message and cause.
         * @param message The exception message seen by the user.
         * @param cause The exception that caused this exception.
         */
        public SettingsException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Initialize the configuration service with the given state.
     * @param state The state.
     */
    public SettingsService(State state) {
        super(state);
    }

    /**
     * Loads the given config from the {@code path} into the {@link State}.
     * @param pathConfig Path to the config file.
     * @param statusBuilder Appends info/warnings that are encountered during load to this builder.
     * @throws SettingsException If an exception occurred during the loading of the config.
     */
    public void loadConfiguration(@NonNull String pathConfig, @NonNull StringBuilder statusBuilder) throws SettingsException {
        try {
            ConfigurationParser.parseConfiguration(Paths.get(pathConfig), state.getLpgMappingConfiguration());
            state.setConfigType(ConfigType.CUSTOM);
            state.setPathConfigurationLoadedFrom(pathConfig);
            state.reloadDerivativeData(statusBuilder);
        } catch (ParserException e) {
            throw new SettingsException("An exception occurred during the parsing of the configuration file", e);
        }
    }

    /**
     * Loads the given config from the {@code path} into the {@link State}.
     * @param pathConfig Path to the config file.
     * @throws SettingsException If an exception occurred during the loading of the config.
     */
    public void loadConfiguration(@NonNull String pathConfig) throws SettingsException {
        this.loadConfiguration(pathConfig, new StringBuilder());
    }

    /**
     * Loads the default configuration into the {@link State}.
     */
    public void loadDefaultConfiguration() {
        state.setLpgMappingConfiguration(LPGMappingConfiguration.defaultConfiguration());
        state.setConfigType(ConfigType.DEFAULT);
        state.setPathConfigurationLoadedFrom(null);
    }

    /**
     * Loads an empty configuration into the {@link State}.
     * An empty configuration only has an {@code defaultNodeLabel}
     */
    public void loadEmptyConfiguration() {
        state.setLpgMappingConfiguration(new LPGMappingConfiguration());
        state.setConfigType(ConfigType.EMPTY);
        state.setPathConfigurationLoadedFrom(null);
    }

    /**
     * Sets teh query timeout in ms on the {@link State}.
     * @param timeoutMillis The time to take before a query times out.
     */
    public void setQueryTimeoutMillis(long timeoutMillis) {
        this.state.setQueryTimeoutMillis(timeoutMillis);
    }

    /**
     * Sets the reversible field on the {@link State#getLpgMappingConfiguration()}.
     * @param reversible True if the mapping configuration should be reversible, false otherwise.
     */
    public void setIsLPGMappingReversible(boolean reversible) {
        this.state.getLpgMappingConfiguration().reversible = reversible;
    }

    /**
     * Gets the current settings from the {@link State}.
     * @return The current settings of the playground.
     */
    public SettingsResponse getCurrentSettings() {
        SettingsResponse response = new SettingsResponse();
        response.timeoutMillis = state.getQueryTimeoutMillis();
        response.reversible = state.getLpgMappingConfiguration().reversible;
        response.pathToConfig = state.getPathConfigurationLoadedFrom();
        response.configType = state.getConfigType();
        return response;
    }
}
