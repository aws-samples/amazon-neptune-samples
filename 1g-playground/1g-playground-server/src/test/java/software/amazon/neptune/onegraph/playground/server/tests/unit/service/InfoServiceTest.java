package software.amazon.neptune.onegraph.playground.server.tests.unit.service;

import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.api.response.InfoResponse;
import software.amazon.neptune.onegraph.playground.server.service.InfoService;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.InfoServiceTestData;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InfoServiceTest {

    @Test
    public void lpgNonCompliant() {
        State state = new State();
        InfoServiceTestData.addTestDataToDatasetLPGNonCompatible(state.getOgDataset());
        InfoService service = new InfoService(state);
        InfoResponse response = service.getInfo();

        assertEquals(0, response.pairwiseNonCompatibleLines.size());
        assertEquals(3, response.lpgNonCompatibleLines.size());
        assertEquals(3, response.rdfNonCompatibleLines.size());
    }

    @Test
    public void fullyCompliant() {
        State state = new State();
        InfoServiceTestData.addTestDataToDatasetFullyCompatible(state.getOgDataset());
        InfoService service = new InfoService(state);
        InfoResponse response = service.getInfo();

        assertEquals(0, response.pairwiseNonCompatibleLines.size());
        assertEquals(0, response.lpgNonCompatibleLines.size());
        assertEquals(0, response.rdfNonCompatibleLines.size());
    }

    @Test
    public void pairsewiseNonCompliant() {
        State state = new State();
        InfoServiceTestData.addTestDataToDatasetPairwiseNonCompatible(state.getOgDataset());
        InfoService service = new InfoService(state);
        InfoResponse response = service.getInfo();

        assertEquals(3, response.pairwiseNonCompatibleLines.size());
        assertEquals(0, response.lpgNonCompatibleLines.size());
        assertEquals(0, response.rdfNonCompatibleLines.size());
    }

    @Test
    public void rdfNonCompliant() {
        State state = new State();
        InfoServiceTestData.addTestDataToDatasetRDFNonCompatible(state.getOgDataset());
        InfoService service = new InfoService(state);
        InfoResponse response = service.getInfo();

        assertEquals(0, response.pairwiseNonCompatibleLines.size());
        assertEquals(1, response.lpgNonCompatibleLines.size());
        assertEquals(2, response.rdfNonCompatibleLines.size());
    }

    @Test
    public void fullyNonCompliant() {
        State state = new State();
        InfoServiceTestData.addTestDataToDatasetFullyNonCompliant(state.getOgDataset());
        InfoService service = new InfoService(state);
        InfoResponse response = service.getInfo();

        assertEquals(0, response.pairwiseNonCompatibleLines.size());
        assertEquals(10, response.lpgNonCompatibleLines.size());
        assertEquals(10, response.rdfNonCompatibleLines.size());
    }
}
