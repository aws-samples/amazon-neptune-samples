package software.amazon.neptune.onegraph.playground.server.tests.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.api.request.SettingsRequest;
import software.amazon.neptune.onegraph.playground.server.api.response.InfoResponse;
import software.amazon.neptune.onegraph.playground.server.api.response.SettingsResponse;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;
import software.amazon.neptune.onegraph.playground.server.tests.unit.io.parsing.ConfigurationParserTest;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SettingsServiceTest {

    private static final String rootPath = "/parserTestDatasets/LPG/LPGMappingConfigurations/";

    private Path getPathToResource(String resourceName) {
        URL pathURL = ConfigurationParserTest.class.getResource(rootPath + resourceName);
        assert pathURL != null;
        return Paths.get(pathURL.getPath());
    }

    private static final String settingsEndpoint = "/settings";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StateSpring state;

    @BeforeAll
    public void resetSettings() {
        state.setConfigType(ConfigType.DEFAULT);
        state.setQueryTimeoutMillis(15000);
        state.setLpgMappingConfiguration(LPGMappingConfiguration.defaultConfiguration());
        state.setPathConfigurationLoadedFrom(null);
    }

    @Test
    public void settings_withCustomConfig_thenIsOK() throws Exception {
        Path custom = getPathToResource("normal_lpg_config.json");

        SettingsRequest req = new SettingsRequest();
        req.reversible = true;
        req.timeoutMillis = 150L;
        req.configType = ConfigType.CUSTOM;
        req.pathConfig = custom.toString();

        mockMvc.perform(post(settingsEndpoint)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertTrue(state.getLpgMappingConfiguration().reversible);
        assertEquals(ConfigType.CUSTOM, state.getConfigType());
        assertEquals(custom.toString(), state.getPathConfigurationLoadedFrom());
        assertEquals(150L, state.getQueryTimeoutMillis());
    }

    @Test
    public void settings_withDefault_thenIsOK() throws Exception {
        SettingsRequest req = new SettingsRequest();
        req.reversible = true;
        req.configType = ConfigType.DEFAULT;

        mockMvc.perform(post(settingsEndpoint)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        assertTrue(state.getLpgMappingConfiguration().reversible);
        assertEquals(ConfigType.DEFAULT, state.getConfigType());
    }

    @Test
    public void settings_withEmpty_thenIsOK() throws Exception {
        SettingsRequest req = new SettingsRequest();
        req.reversible = true;
        req.configType = ConfigType.EMPTY;

        mockMvc.perform(post(settingsEndpoint)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        assertTrue(state.getLpgMappingConfiguration().reversible);
        assertEquals(ConfigType.EMPTY, state.getConfigType());
    }

    @Test
    public void settings_withMalformedConfig_thenInternalServerError() throws Exception {
        Path customMalformed = getPathToResource("malformed_lpg_config.json");

        SettingsRequest req = new SettingsRequest();
        req.reversible = true;
        req.timeoutMillis = 150L;
        req.configType = ConfigType.CUSTOM;
        req.pathConfig = customMalformed.toString();

        MvcResult result = mockMvc.perform(post(settingsEndpoint)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError()).andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("An unknown exception has occurred while changing settings"));
    }

    @Test
    public void settings_withDefaultConfigButAlsoPath_thenBadRequest() throws Exception {
        Path custom = getPathToResource("normal_lpg_config.json");

        SettingsRequest req = new SettingsRequest();
        req.configType = ConfigType.DEFAULT;
        req.pathConfig = custom.toString();

        MvcResult result = mockMvc.perform(post(settingsEndpoint)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Incorrect arguments provided;"));
    }

    @Test
    public void settings_withCustomConfigButNoPath_thenBadRequest() throws Exception {
        SettingsRequest req = new SettingsRequest();
        req.configType = ConfigType.CUSTOM;

        MvcResult result = mockMvc.perform(post(settingsEndpoint)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Incorrect arguments provided;"));
    }

    @Test
    public void settings_withPathButNoType_thenBadRequest() throws Exception {
        Path custom = getPathToResource("normal_lpg_config.json");

        SettingsRequest req = new SettingsRequest();
        req.pathConfig = custom.toString();

        MvcResult result = mockMvc.perform(post(settingsEndpoint)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest()).andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Incorrect arguments provided;"));
    }

    @Test
    public void settings_whenGetCurrent_thenIsOK() throws Exception {

        state.setQueryTimeoutMillis(1234L);
        state.setConfigType(ConfigType.EMPTY);
        state.getLpgMappingConfiguration().reversible = false;

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(settingsEndpoint)).
                andExpect(status().isOk()).andReturn();
        String response = result.getResponse().getContentAsString();
        SettingsResponse matResponse = objectMapper.readValue(response, SettingsResponse.class);

        assertFalse(matResponse.reversible);
        assertEquals(ConfigType.EMPTY, matResponse.configType);
        assertEquals(1234L, matResponse.timeoutMillis);
    }
}
