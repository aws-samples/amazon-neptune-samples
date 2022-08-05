package software.amazon.neptune.onegraph.playground.server.tests.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.GeneralServiceTestData;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataServiceViewTest {

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

    @BeforeEach
    public void clearState() {
        state.clear();
    }

    @Test
    public void view_whenViewNormalData_thenOKAndRetrievedData() throws Exception {
        GeneralServiceTestData.addToDataSet(state.getOgDataset());
        state.reloadDerivativeData();
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                get(dataEndpoint + "?dataFormat=NEPTUNECSV")).
                andExpect(status().isOk()).andReturn();

        String response = result.getResponse().getContentAsString();

        assertTrue(response.contains("---- NODES ----"));
        assertTrue(response.contains("---- EDGES ----"));
        assertTrue(response.contains("BNodeA"));
        assertTrue(response.contains("BNodeB"));
    }

    @Test
    public void view_whenViewGraphMLIncompatibleData_thenInternalError() throws Exception {
        GeneralServiceTestData.addGraphMLIncompatibleData(state.getOgDataset());
        state.reloadDerivativeData();
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(dataEndpoint + "?dataFormat=GRAPHML")).
                andExpect(status().isInternalServerError()).andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("An exception occurred during view"));
    }

    @Test
    public void view_whenViewFormal_thenOKAndRetrievedData() throws Exception {
        GeneralServiceTestData.addToDataSet(state.getOgDataset());
        state.reloadDerivativeData();
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(dataEndpoint + "?dataFormat=FORMAL")).
                andExpect(status().isOk()).andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("statement:"));
        assertTrue(response.contains("---- Relationship Statements ----"));
        assertTrue(response.contains("---- Property Statements ----"));
        assertTrue(response.contains("---- Membership Statements ----"));
        assertTrue(response.contains("---- Simple Nodes ----"));
        assertTrue(response.contains("---- Graphs ----"));
    }
}
