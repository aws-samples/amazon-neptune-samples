package software.amazon.neptune.onegraph.playground.server.tests.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.helper.Equivalence;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.GeneralServiceTestData;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataServiceExportTest {

    private static final String exportEndpoint = "/data/export";
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
    public void loadData() {
        state.clear();
        GeneralServiceTestData.addToDataSet(state.getOgDataset());
        state.reloadDerivativeData();
    }

    @Test
    public void export_whenSecondPathProvidedButNotNEPTUNECSV_thenBadRequest() throws Exception {
        Path temp1 = Files.createTempFile("tempNodes",".csv");
        Path temp2 = Files.createTempFile("tempEdges",".csv");
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(exportEndpoint + "?dataFormat=GRAPHML&path1=%s&path2=%s", temp1.toAbsolutePath(),
                                temp2.toAbsolutePath())).
                andExpect(status().isBadRequest()).andReturn();
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Invalid argument combination"));
    }

    @Test
    public void export_whenNoSecondPathProvidedButNEPTUNECSV_thenBadRequest() throws Exception {
        Path temp1 = Files.createTempFile("tempNodes",".csv");
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(exportEndpoint + "?dataFormat=NEPTUNECSV&path1=%s", temp1.toAbsolutePath())).
                andExpect(status().isBadRequest()).andReturn();
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Invalid argument combination"));
    }

    @Test
    public void export_FORMAL_thenIsOKAndDataIsInFile() throws Exception {
        Path temp1 = Files.createTempFile("tempFormal",".txt");
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(String.format(exportEndpoint + "?dataFormat=FORMAL&path1=%s", temp1.toAbsolutePath()))).
                andExpect(status().isOk()).andReturn();
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Export successful"));

        byte[] encoded1 = Files.readAllBytes(temp1);
        String content1 = new String(encoded1, StandardCharsets.UTF_8);
        assertTrue(content1.contains("Relationship"));
    }

    @Test
    public void export_GRAPHSON_thenClearState_thenLoad_thenHasData() throws Exception {
        Path temp1 = Files.createTempFile("tempGraphSON",".json");
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(String.format(exportEndpoint + "?dataFormat=GRAPHSON&path1=%s", temp1.toAbsolutePath()))).
                andExpect(status().isOk()).andReturn();
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Export successful"));

        OGDataset expected = state.getOgDataset();
        state.clear();

        try (InputStream stream = Files.newInputStream(temp1)) {
            MockMultipartFile data = new MockMultipartFile("data", null,
                    "text/plain;charset=UTF-8", stream);
            mockMvc.perform(multipart(dataEndpoint).file(data).param("format", String.valueOf(DataFormat.GRAPHSON)))
                    .andExpect(status().isOk());
        }

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, state.getOgDataset());
    }

    @Test
    public void export_TURTLE_thenClearState_thenLoad_thenSameData() throws Exception {
        Path temp1 = Files.createTempFile("tempTurtle",".ttl");
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(String.format(exportEndpoint + "?dataFormat=TURTLE&path1=%s", temp1.toAbsolutePath()))).
                andExpect(status().isOk()).andReturn();
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Export successful"));

        OGDataset expected = state.getOgDataset();
        state.clear();

        try (InputStream stream = Files.newInputStream(temp1)) {
            MockMultipartFile data = new MockMultipartFile("data", null,
                    "text/plain;charset=UTF-8", stream);
            mockMvc.perform(multipart(dataEndpoint).file(data).param("format", String.valueOf(DataFormat.TURTLE)))
                    .andExpect(status().isOk());
        }

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, state.getOgDataset());
    }

    @Test
    public void export_OG_thenClearState_thenLoad_thenSameData() throws Exception {
        Path temp1 = Files.createTempFile("tempOG",".txt");
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(String.format(exportEndpoint + "?dataFormat=OG&path1=%s", temp1.toAbsolutePath()))).
                andExpect(status().isOk()).andReturn();
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Export successful"));

        OGDataset expected = state.getOgDataset();
        state.clear();

        try (InputStream stream = Files.newInputStream(temp1)) {
            MockMultipartFile data = new MockMultipartFile("data", null,
                    "text/plain;charset=UTF-8", stream);
            mockMvc.perform(multipart(dataEndpoint).file(data).param("format", String.valueOf(DataFormat.OG)))
                    .andExpect(status().isOk());
        }

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, state.getOgDataset());
    }

    @Test
    public void export_whenGraphMLIncompatibleData_thenInternalError() throws Exception {
        Path temp1 = Files.createTempFile("tempGraphML",".xml");

        GeneralServiceTestData.addGraphMLIncompatibleData(state.getOgDataset());
        state.reloadDerivativeData();
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(String.format(exportEndpoint + "?dataFormat=GRAPHML&path1=%s",temp1.toAbsolutePath()))).
                andExpect(status().isInternalServerError()).andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("An unknown exception has occurred during export"));
    }
}
