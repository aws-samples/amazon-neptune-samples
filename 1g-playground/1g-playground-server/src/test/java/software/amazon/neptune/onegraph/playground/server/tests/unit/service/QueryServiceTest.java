package software.amazon.neptune.onegraph.playground.server.tests.unit.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.api.response.QueryResponse;
import software.amazon.neptune.onegraph.playground.server.helper.Equivalence;
import software.amazon.neptune.onegraph.playground.server.service.QueryService;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.GeneralServiceTestData;

import java.util.Arrays;
import java.util.HashSet;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class QueryServiceTest {

    public static final State state = new State();

    @BeforeAll
    public static void loadData() {
        GeneralServiceTestData.addToDataSet(state.getOgDataset());
        state.reloadDerivativeData();
    }

    @Test
    public void gremlin() throws InterruptedException, ExecutionException, TimeoutException {
        QueryService service = new QueryService(state);

        String query = "g.V()";
        Future<QueryResponse> future = service.performGremlinQuery(query);

        QueryResponse response = future.get(10, TimeUnit.SECONDS);
        assertEquals(2, response.traversalResult.size());
    }

    @Test
    public void sparqlSELECTAll() throws InterruptedException, ExecutionException, TimeoutException {
        QueryService service = new QueryService(state);

        String query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o }";
        Future<QueryResponse> future = service.performSPARQLQuery(query);

        QueryResponse response = future.get(10, TimeUnit.SECONDS);

        assertEquals(3, response.tupleResult.size());
        assertEquals(new HashSet<>(Arrays.asList("s", "p","o")), response.tupleResult.keySet());
        Equivalence.assertModelSuperficiallyEqualToSPOMap(GeneralServiceTestData.rdfRepresentation(), response.tupleResult);
    }

    @Test
    public void sparqlASK() throws InterruptedException, ExecutionException, TimeoutException {
        QueryService service = new QueryService(state);

        String query = "ASK { ?s ?p ?o }";
        Future<QueryResponse> future = service.performSPARQLQuery(query);

        QueryResponse response = future.get(10, TimeUnit.SECONDS);
        assertTrue(response.booleanResult);
    }

    @Test
    public void sparqlCONSTRUCTAll() throws InterruptedException, ExecutionException, TimeoutException {
        QueryService service = new QueryService(state);

        String query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
        Future<QueryResponse> future = service.performSPARQLQuery(query);

        QueryResponse response = future.get(10, TimeUnit.SECONDS);
        assertEquals(4, response.graphResult.size());
        Equivalence.assertModelSuperficiallyEqualToSPOMap(GeneralServiceTestData.rdfRepresentation(), response.graphResult);
    }

    @Test
    public void sparqlError() {
        QueryService service = new QueryService(state);

        String query = "MALFORMED { ?s ?p ?o } WHERE { ?s ?p ?o }";
        Future<QueryResponse> future = service.performSPARQLQuery(query);

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> future.get(10, TimeUnit.SECONDS));

        String expectedMessage = "An exception occurred during the execution of the query";
        String actualMessage = exception.getCause().getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void gremlinError() {
        QueryService service = new QueryService(state);

        String query = "g.VVVVVV";
        Future<QueryResponse> future = service.performGremlinQuery(query);

        ExecutionException exception = assertThrows(ExecutionException.class,
                () -> future.get(10, TimeUnit.SECONDS));

        String expectedMessage = "An exception occurred during the execution of the traversal";
        String actualMessage = exception.getCause().getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
