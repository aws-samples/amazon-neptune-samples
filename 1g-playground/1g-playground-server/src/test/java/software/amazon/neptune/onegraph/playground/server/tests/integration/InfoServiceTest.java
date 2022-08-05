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
import software.amazon.neptune.onegraph.playground.server.api.response.InfoResponse;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.InfoServiceTestData;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InfoServiceTest {

    private static final String infoEndpoint = "/info";


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
    public void info_whenFullyCompliantData_thenOKAndRetrievedData() throws Exception {
        InfoServiceTestData.addTestDataToDatasetFullyCompatible(state.getOgDataset());
        state.reloadDerivativeData();

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(infoEndpoint)).
                andExpect(status().isOk()).andReturn();
        String response = result.getResponse().getContentAsString();

        InfoResponse matResponse = objectMapper.readValue(response, InfoResponse.class);

        assertEquals(0,matResponse.lpgNonCompatibleLines.size());
        assertEquals(0,matResponse.rdfNonCompatibleLines.size());
        assertEquals(0,matResponse.pairwiseNonCompatibleLines.size());

        String og = matResponse.serializedOG;

        assertTrue(og.contains("__:SID1"));
        assertTrue(og.contains("_:A"));
        assertTrue(og.contains("\"Alice\""));
    }

}
