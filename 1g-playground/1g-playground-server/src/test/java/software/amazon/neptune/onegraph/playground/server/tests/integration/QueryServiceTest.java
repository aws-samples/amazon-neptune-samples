package software.amazon.neptune.onegraph.playground.server.tests.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.api.response.QueryResponse;
import software.amazon.neptune.onegraph.playground.server.helper.Equivalence;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.GeneralServiceTestData;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class QueryServiceTest {

    private static final String queryEndpoint = "/query";
    
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

        GeneralServiceTestData.addToDataSet(state.getOgDataset());
        state.reloadDerivativeData();
    }

    @Test
    public void query_whenGremlinValid_thenIsOKAndData() throws Exception {
        String validQuery = "g.V()";
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.
                        get(String.format(queryEndpoint + "/gremlin?query=%s", validQuery))).
                andExpect(status().isOk()).andReturn();

        String response = result.getResponse().getContentAsString();
        QueryResponse matResponse = objectMapper.readValue(response, QueryResponse.class);

        assertEquals(2, matResponse.traversalResult.size());
        assertTrue(matResponse.traversalResult.toString().contains("v[BNodeA"));
        assertTrue(matResponse.traversalResult.toString().contains("v[BNodeB"));
    }
//    @Test
//    public void query_whenSPARQLSELECTValid_thenIsOKAndData() throws Exception  {
//        String validQuery = "SELECT ?s ?p ?o WHERE { ?s ?p ?o }";
//
//        RequestBuilder b = MockMvcRequestBuilders.get(queryEndpoint + "/sparql").param("query",validQuery);
//
//        MvcResult result = mockMvc.perform(b).
//                andExpect(status().isOk()).andReturn();
//
//        String response = result.getResponse().getContentAsString();
//        QueryResponse matResponse = objectMapper.readValue(response, QueryResponse.class);
//        Equivalence.assertModelSuperficiallyEqualToSPOMap(GeneralServiceTestData.rdfRepresentation(), matResponse.tupleResult);
//    }
//
//    @Test
//    public void query_whenSPARQLCONSTRUCTValid_thenIsOKAndData() throws Exception  {
//        String validQuery = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
//
//        RequestBuilder b = MockMvcRequestBuilders.get(queryEndpoint + "/sparql").param("query",validQuery);
//
//        MvcResult result = mockMvc.perform(b).
//                andExpect(status().isOk()).andReturn();
//
//        String response = result.getResponse().getContentAsString();
//        QueryResponse matResponse = objectMapper.readValue(response, QueryResponse.class);
//        Equivalence.assertModelSuperficiallyEqualToSPOMap(GeneralServiceTestData.rdfRepresentation(), matResponse.graphResult);
//    }

    @Test
    public void query_whenSPARQLASKValid_thenIsOKAndData() throws Exception  {
        String validQuery = "ASK { ?s ?p ?o }";

        RequestBuilder b = MockMvcRequestBuilders.get(queryEndpoint + "/sparql").param("query",validQuery);

        MvcResult result = mockMvc.perform(b).
                andExpect(status().isOk()).andReturn();

        String response = result.getResponse().getContentAsString();
        QueryResponse matResponse = objectMapper.readValue(response, QueryResponse.class);
        assertTrue(matResponse.booleanResult);
    }

    @Test
    public void query_whenGremlinMalformed_thenBadRequest() throws Exception {
        String badQuery = "ggggg.v(";

        RequestBuilder b = MockMvcRequestBuilders.get(queryEndpoint + "/gremlin").param("query", badQuery);

        MvcResult result = mockMvc.perform(b).
                andExpect(status().isBadRequest()).andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("An exception occurred during the execution of the traversal"));
    }

    @Test
    public void query_whenSPARQLMalformed_thenBadRequest() throws Exception {
        String badQuery = "SELECdfffaT ?s ?p ?o WHERE { ?s ?p ?o }";

        RequestBuilder b = MockMvcRequestBuilders.get(queryEndpoint + "/sparql").param("query", badQuery);

        MvcResult result = mockMvc.perform(b).
                andExpect(status().isBadRequest()).andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("An exception occurred during the execution of the query"));
    }
}
