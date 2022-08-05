package software.amazon.neptune.onegraph.playground.server.tests.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.GeneralServiceTestData;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataServiceClearTest {

    private static final String dataEndpoint = "/data";

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

    @BeforeAll
    public void loadData() {
        GeneralServiceTestData.addToDataSet(state.getOgDataset());
        state.reloadDerivativeData();
    }

    @Test
    public void clear_whenData_thenIsOK() throws Exception {
        state.clear();
        GeneralServiceTestData.addToDataSet(state.getOgDataset());
        state.reloadDerivativeData();
        mockMvc.perform(delete(dataEndpoint)
                        .contentType("application/json")
                        .content(""))
                .andExpect(status().isOk());
        assertTrue(state.getOgDataset().isEmpty());
    }
}
