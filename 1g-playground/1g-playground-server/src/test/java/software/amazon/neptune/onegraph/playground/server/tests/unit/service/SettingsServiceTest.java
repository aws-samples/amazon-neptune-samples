package software.amazon.neptune.onegraph.playground.server.tests.unit.service;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.api.response.SettingsResponse;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.SimpleReification;
import software.amazon.neptune.onegraph.playground.server.service.SettingsService;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.tests.unit.io.parsing.ConfigurationParserTest;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SettingsServiceTest {

    private static final String rootPath = "/parserTestDatasets/LPG/LPGMappingConfigurations/";

    private Path getPathToResource(String resourceName) {
        URL pathURL = ConfigurationParserTest.class.getResource(rootPath + resourceName);
        assert pathURL != null;
        return Paths.get(pathURL.getPath());
    }

    public static LPGMappingConfiguration customConfig = (new SimpleReification()).mappingConfiguration();

    @Test
    public void setQueryTimeout() {
        State state = new State();

        SettingsService s = new SettingsService(state);
        s.setQueryTimeoutMillis(5000);

        assertEquals(5000, state.getQueryTimeoutMillis());
    }

    @Test
    public void setDefaultConfig() {
        State state = new State();

        state.setLpgMappingConfiguration(customConfig);
        SettingsService s = new SettingsService(state);
        s.loadDefaultConfiguration();

        assertNotEquals(customConfig, state.getLpgMappingConfiguration());
    }

    @Test
    public void setCustomConfig() {
        Path custom = getPathToResource("normal_lpg_config.json");

        State state = new State();

        LPGMappingConfiguration defaultC = LPGMappingConfiguration.defaultConfiguration();
        state.setLpgMappingConfiguration(defaultC);
        IRI oldDNL = LPGMappingConfiguration.defaultConfiguration().defaultNodeLabelPredicate;

        SettingsService s = new SettingsService(state);
        s.loadConfiguration(custom.toString());

        assertNotEquals(oldDNL, state.getLpgMappingConfiguration().defaultNodeLabelPredicate);
    }

    @Test
    public void setEmptyConfig() {
        State state = new State();

        state.setLpgMappingConfiguration(customConfig);
        SettingsService s = new SettingsService(state);
        s.loadEmptyConfiguration();

        assertNotEquals(customConfig, state.getLpgMappingConfiguration());
    }

    @Test
    public void setReversibleConfig() {
        State state = new State();

        state.setLpgMappingConfiguration(customConfig);
        state.getLpgMappingConfiguration().reversible = false;
        SettingsService s = new SettingsService(state);
        s.setIsLPGMappingReversible(true);

        assertTrue(state.getLpgMappingConfiguration().reversible);
    }

    @Test
    public void loadMalformedConfigCauseException() {
        Path customMalformed = getPathToResource("malformed_lpg_config.json");

        State state = new State();
        SettingsService service = new SettingsService(state);

        SettingsService.SettingsException exception = assertThrows(SettingsService.SettingsException.class,
                () -> service.loadConfiguration(customMalformed.toString()));

        String expectedMessage = "An exception occurred during the parsing of the configuration file";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void getCurrentSettings() {
        State state = new State();

        state.getLpgMappingConfiguration().reversible = true;
        state.setConfigType(ConfigType.DEFAULT);
        state.setPathConfigurationLoadedFrom("example/path/config.json");
        state.setQueryTimeoutMillis(1234);

        SettingsService s = new SettingsService(state);
        SettingsResponse response = s.getCurrentSettings();

        assertEquals(true, response.reversible);
        assertEquals(ConfigType.DEFAULT, response.configType);
        assertEquals("example/path/config.json", response.pathToConfig);
        assertEquals(1234, response.timeoutMillis);
    }
}
