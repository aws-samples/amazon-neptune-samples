package software.amazon.neptune.onegraph.playground.server.api.controller;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.api.request.SettingsRequest;
import software.amazon.neptune.onegraph.playground.server.api.response.SettingsResponse;
import software.amazon.neptune.onegraph.playground.server.service.SettingsService;
import software.amazon.neptune.onegraph.playground.server.servicespring.SettingsServiceSpring;
import software.amazon.neptune.onegraph.playground.server.state.State;

/**
 * REST controller for the settings command.
 */
@RestController
@RequestMapping("/settings")
public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    private final SettingsServiceSpring settingsService;

    @Autowired
    public SettingsController(SettingsServiceSpring ss) {
        this.settingsService = ss;
    }

    /**
     * Callback for POST requests to the /settings path, allows the client to change the settings of the playground which are
     * stored in the {@link State}.
     * @param request The request containing the settings the client wants to modify.
     * @return A string containing information for the client about the settings.
     * @throws IllegalArgumentException if the {@link SettingsRequest#pathConfig} is not set while the
     * {@link SettingsRequest#configType} is {@code CUSTOM}, or when the {@link SettingsRequest#pathConfig} is set but the
     * {@link SettingsRequest#configType} is not {@code CUSTOM}.
     */
    @PostMapping
    public String updateSettings(@RequestBody SettingsRequest request) throws IllegalArgumentException {
        validateParameters(request);

        StringBuilder statusBuilder = new StringBuilder();
        if (request.configType != null) {
            switch (request.configType) {
                case EMPTY:
                    this.settingsService.loadEmptyConfiguration();
                    break;
                case CUSTOM:
                    this.settingsService.loadConfiguration(request.pathConfig, statusBuilder);
                    break;
                case DEFAULT:
                default:
                    this.settingsService.loadDefaultConfiguration();
            }
        }

        if (request.timeoutMillis != null) {
            this.settingsService.setQueryTimeoutMillis(request.timeoutMillis);
        }

        if (request.reversible != null) {
            this.settingsService.setIsLPGMappingReversible(request.reversible);
        }

        statusBuilder.append("Settings successfully updated");
        return statusBuilder.toString();
    }

    /**
     * Callback for GET requests to the /settings endpoint.
     * @return Information about the current settings in the playground
     */
    @GetMapping
    public SettingsResponse retrieveSettings() {
        return this.settingsService.getCurrentSettings();
    }

    // Checks if pathConfig is set if and only if type is CUSTOM.
    private void validateParameters(SettingsRequest req) throws IllegalArgumentException {
        if (req.configType == null) {
            if (req.pathConfig != null) {
                throw new IllegalArgumentException("Invalid argument combination: no pathConfig argument should be provided when there is no type argument");
            }
        } else if (req.configType == ConfigType.CUSTOM && (req.pathConfig == null || req.pathConfig.isEmpty())) {
            throw new IllegalArgumentException("Invalid argument combination: a pathConfig argument should be provided when type is CUSTOM");
        } else if (req.configType != ConfigType.CUSTOM && req.pathConfig != null) {
            throw new IllegalArgumentException("Invalid argument combination: no pathConfig argument should be provided when type is not CUSTOM");
        }
    }

    @ExceptionHandler({ IllegalArgumentException.class })
    public ResponseEntity<String> handleException(IllegalArgumentException e) {
        logger.warn("Argument exception - " + e.getMessage());

        String errorToReturn = String.format("Incorrect arguments provided; %s", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorToReturn);
    }

    @ExceptionHandler({ SettingsService.SettingsException.class })
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleException(SettingsService.SettingsException settingsException) {
        logger.warn("Settings exception - " + settingsException.getCause().getMessage());

        Throwable rootCause = ExceptionUtils.getRootCause(settingsException);
        String errorToReturn = String.format("An unknown exception has occurred while changing settings CAUSE: %s ROOT CAUSE: %s",
                settingsException.getMessage(),
                rootCause.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorToReturn);
    }
}
