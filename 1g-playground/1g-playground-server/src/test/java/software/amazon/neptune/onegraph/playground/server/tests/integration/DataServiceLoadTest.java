package software.amazon.neptune.onegraph.playground.server.tests.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.io.parsing.RDFParser;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;
import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.tests.unit.io.parsing.ConfigurationParserTest;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataServiceLoadTest {

    private static final String dataEndpoint = "/data";

    private static final String rootPath = "/parserTestDatasets/";

    private Path getPathToResource(String resourceName) {
        URL pathURL = ConfigurationParserTest.class.getResource(rootPath + resourceName);
        assert pathURL != null;
        return Paths.get(pathURL.getPath());
    }

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
    public void load_whenExistingRDFFile_thenOKAndStateNotEmpty() throws Exception {

        Path resource = getPathToResource("RDF/nQuads/nquads_normal.nq");

        state.clear();
        assertTrue(state.getOgDataset().isEmpty());

        try (InputStream stream = Files.newInputStream(resource)) {
            MockMultipartFile data = new MockMultipartFile("data", null,
                    "text/plain;charset=UTF-8", stream);

            mockMvc.perform(multipart(dataEndpoint).file(data).param("format", String.valueOf(DataFormat.NQUADS)))
                    .andExpect(status().isOk());
        }

        try (InputStream s = Files.newInputStream(resource)) {
            Model expected = RDFParser.parseRDF(s, DataFormat.NQUADS);
            assertEquals(expected, state.getRdfModel());
        }
    }

    @Test
    public void load_whenSecondPathProvidedButNotNEPTUNECSV_thenBadRequest() throws Exception {

        Path resource = getPathToResource("RDF/nQuads/nquads_normal.nq");

        state.clear();
        assertTrue(state.getOgDataset().isEmpty());

        try (InputStream stream1 = Files.newInputStream(resource);
             InputStream stream2 = Files.newInputStream(resource)) {
            MockMultipartFile data = new MockMultipartFile("data", null,
                    "text/plain;charset=UTF-8", stream1);
            MockMultipartFile data2 = new MockMultipartFile("additionalData", null,
                    "text/plain;charset=UTF-8", stream2);
            MvcResult result = mockMvc.perform(multipart(dataEndpoint).file(data).file(data2).param("format", String.valueOf(DataFormat.NQUADS)))
                    .andExpect(status().isBadRequest()).andReturn();
            String response = result.getResponse().getContentAsString();
            assertTrue(response.contains("Invalid argument combination"));
        }
    }

    @Test
    public void load_whenNoSecondPathProvidedButNEPTUNECSV_thenBadRequest() throws Exception {
        Path resource = getPathToResource("LPG/NeptuneCSV/nCSV_nodes_normal.csv");

        state.clear();
        assertTrue(state.getOgDataset().isEmpty());

        try (InputStream stream = Files.newInputStream(resource)) {
            MockMultipartFile data = new MockMultipartFile("data", null,
                    "text/plain;charset=UTF-8", stream);
            MvcResult result = mockMvc.perform(multipart(dataEndpoint).file(data).param("format", String.valueOf(DataFormat.NEPTUNECSV)))
                    .andExpect(status().isBadRequest()).andReturn();
            String response = result.getResponse().getContentAsString();
            assertTrue(response.contains("Invalid argument combination"));
        }
    }

    @Test
    public void load_whenDataFormatIsFORMAL_thenBadRequest() throws Exception {
        Path resource = getPathToResource("LPG/NeptuneCSV/nCSV_nodes_normal.csv");

        state.clear();
        assertTrue(state.getOgDataset().isEmpty());

        try (InputStream stream = Files.newInputStream(resource)) {
            MockMultipartFile data = new MockMultipartFile("data", null,
                    "text/plain;charset=UTF-8", stream);
            MvcResult result = mockMvc.perform(multipart(dataEndpoint).file(data).param("format", String.valueOf(DataFormat.FORMAL)))
                    .andExpect(status().isBadRequest()).andReturn();
            String response = result.getResponse().getContentAsString();
            assertTrue(response.contains("Incorrect arguments provided"));
        }
    }

    @Test
    public void load_whenMalformedFile_thenInternalServerError() throws Exception {

        Path resource = getPathToResource("oneGraph/malformed/og_malformed_iri.txt");

        state.clear();
        assertTrue(state.getOgDataset().isEmpty());

        try (InputStream stream = Files.newInputStream(resource)) {
            MockMultipartFile data = new MockMultipartFile("data", null,
                    "text/plain;charset=UTF-8", stream);
            MvcResult result = mockMvc.perform(multipart(dataEndpoint).file(data).param("format", String.valueOf(DataFormat.OG)))
                    .andExpect(status().isInternalServerError()).andReturn();
            String response = result.getResponse().getContentAsString();
            assertTrue(response.contains("An exception occurred during load"));
        }
    }

    @Test
    public void load_whenNEPTUNECSV_thenOKAndStateNotEmpty() throws Exception {

        Path resource1 = getPathToResource("LPG/NeptuneCSV/nCSV_nodes_normal.csv");
        Path resource2 = getPathToResource("LPG/NeptuneCSV/nCSV_edges_normal.csv");

        state.clear();
        assertTrue(state.getOgDataset().isEmpty());

        try (InputStream stream1 = Files.newInputStream(resource1);
             InputStream stream2 = Files.newInputStream(resource2)) {
            MockMultipartFile data = new MockMultipartFile("data", null,
                    "text/plain;charset=UTF-8", stream1);
            MockMultipartFile data2 = new MockMultipartFile("additionalData", null,
                    "text/plain;charset=UTF-8", stream2);

            mockMvc.perform(multipart(dataEndpoint).file(data).file(data2).param("format", String.valueOf(DataFormat.NEPTUNECSV)))
                    .andExpect(status().isOk());

            assertFalse(state.getOgDataset().isEmpty());
            assertNotEquals(0, state.getLpgGraph().vertices.size());
            assertNotEquals(0,  Iterators.size(state.getTinkerPopGraph().vertices()));
        }
    }
}
