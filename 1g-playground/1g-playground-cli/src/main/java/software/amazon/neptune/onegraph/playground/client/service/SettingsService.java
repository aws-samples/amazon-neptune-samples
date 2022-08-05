package software.amazon.neptune.onegraph.playground.client.service;

import lombok.NonNull;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.client.api.API;
import software.amazon.neptune.onegraph.playground.client.api.ConfigType;
import software.amazon.neptune.onegraph.playground.client.request.SettingsRequest;
import software.amazon.neptune.onegraph.playground.client.response.SettingsResponse;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Service for requests to the /settings endpoint.
 * It makes a request to the endpoint using the {@link API} and
 * formats the response for the user.
 */
@Service
public class SettingsService {

    private final API api;

    /**
     * Creates a new instance of the settings service.
     * @throws IOException When an exception occurs during creation.
     */
    public SettingsService() throws IOException {
        api = new API();
    }

    /**
     * Makes POST request to the {@code /settings} endpoint to change the current settings of the playground.
     * @param configType Can be {@code null}, use this to indicate the type of the mapping configuration.
     * @param pathConfig Can be {@code null}, this should point to the path of the file where the custom configuration is.
     * @param reversible Can be {@code null}, set to {@code true} when the LPG mapping configuration should also be applied in reverse
     *                   e.g. when mapping from OneGraph back to LPG.
     * @param timeoutMillis Can be {@code null}, the query timeout.
     * @throws IllegalArgumentException when {@code configType} is {@code null} but {@code pathConfig} is not, or when
     * {@code configType} is not {@code null} but {@code pathConfig} is.
     * @throws ServiceException when an exception occurred during the process.
     * @return A string containing information passed in the response.
     */
    public String updateSettings(ConfigType configType,
                                   String pathConfig,
                                   Boolean reversible,
                                   Long timeoutMillis) throws IllegalArgumentException, ServiceException {
        if (configType == null && pathConfig == null && reversible == null && timeoutMillis == null) {
            return "";
        }
        validateParameters(configType, pathConfig);
        SettingsRequest req = new SettingsRequest(configType, pathConfig, reversible, timeoutMillis);
        try {
            HttpURLConnection connection = api.setupPOSTConnection(api.endpoints.SETTINGS_ENDPOINT, req);
            connection.connect();
            return api.serializeResponseToString(connection);
        } catch (Exception e) {
            throw new ServiceException("settings", e);
        }
    }

    /**
     * Obtains the settings that are currently on the server.
     * @return A formatted string containing the settings.
     * @throws ServiceException when an exception occurred during the process.
     */
    public String getCurrentSettings() throws ServiceException {
        try {
            HttpURLConnection connection = api.setupGETConnection(api.endpoints.SETTINGS_ENDPOINT);
            connection.connect();
            String responseString = api.serializeResponseToString(connection);
            SettingsResponse response = api.mapper.readValue(responseString, SettingsResponse.class);
            return formatSettingsResponse(response);
        } catch (IOException e) {
            throw new ServiceException("settings", e);
        }
    }

    private String formatSettingsResponse(@NonNull SettingsResponse response) {
        StringBuilder formattedResponse = new StringBuilder();
        formattedResponse.append("---- Current LPG mapping configuration type ----").append("\n");
        formattedResponse.append(response.configType.toString()).append("\n\n");
        if (response.pathToConfig != null) {
            formattedResponse.append("---- Path configuration loaded from ----").append("\n");
            formattedResponse.append(response.pathToConfig).append("\n\n");
        }
        formattedResponse.append("---- Is current LPG mapping configuration reversible? ----").append("\n");
        formattedResponse.append(response.reversible).append("\n\n");
        formattedResponse.append("---- Query timeout in milliseconds ----").append("\n");
        formattedResponse.append(response.timeoutMillis).append("\n\n");
        return formattedResponse.toString();
    }

    private void validateParameters(ConfigType configType,
                                    String pathConfig) throws IllegalArgumentException {
        if (configType == ConfigType.CUSTOM && (pathConfig == null || pathConfig.isEmpty())) {
            throw new IllegalArgumentException("A path to config should be specified when the config type is set to CUSTOM");
        } else if (configType != ConfigType.CUSTOM && (pathConfig != null && !pathConfig.isEmpty())) {
            throw new IllegalArgumentException("No path to config should be specified when the config type is not CUSTOM");
        }
    }
}
