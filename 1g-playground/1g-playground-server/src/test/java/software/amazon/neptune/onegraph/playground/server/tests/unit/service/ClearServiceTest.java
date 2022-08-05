package software.amazon.neptune.onegraph.playground.server.tests.unit.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.service.ClearService;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.ClearServiceTestData;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClearServiceTest {

    @Test
    public void clearState() {

        State state = new State();
        ClearServiceTestData.addTestDataToDataset(state.getOgDataset());
        state.reloadDerivativeData();

        ClearService service = new ClearService(state);

        OGDataset dBefore = state.getOgDataset();
        Graph tinkerGBefore = state.getTinkerPopGraph();
        LPGGraph lpgBefore = state.getLpgGraph();
        Model rdfBefore = state.getRdfModel();

        assertNotEquals(0, Iterables.size(dBefore.getMembershipStatements()));
        assertNotEquals(0, Iterables.size(dBefore.getRelationshipStatements()));
        assertNotEquals(0, Iterables.size(dBefore.getPropertyStatements()));
        assertNotEquals(0, Iterables.size(dBefore.getSimpleNodes()));
        assertNotEquals(0, Iterables.size(dBefore.getGraphs()));
        assertNotEquals(0, Iterables.size(dBefore.getGraphs()));
        assertNotEquals(0, Iterators.size(tinkerGBefore.vertices()));
        assertNotEquals(0, Iterators.size(tinkerGBefore.edges()));
        assertNotEquals(0, lpgBefore.vertices.size());
        assertNotEquals(0, lpgBefore.edges.size());
        assertNotEquals(0, rdfBefore.size());

        service.clearData();

        OGDataset dAfter = state.getOgDataset();
        Graph tinkerGAfter = state.getTinkerPopGraph();
        LPGGraph lpgAfter = state.getLpgGraph();
        Model rdfAfter = state.getRdfModel();

        assertEquals(0, Iterables.size(dAfter.getMembershipStatements()));
        assertEquals(0, Iterables.size(dAfter.getRelationshipStatements()));
        assertEquals(0, Iterables.size(dAfter.getPropertyStatements()));
        assertEquals(0, Iterables.size(dAfter.getSimpleNodes()));
        assertEquals(0, Iterables.size(dAfter.getGraphs()));
        assertEquals(0, Iterators.size(tinkerGAfter.vertices()));
        assertEquals(0, Iterators.size(tinkerGAfter.edges()));
        assertEquals(0, lpgAfter.vertices.size());
        assertEquals(0, lpgAfter.edges.size());
        assertEquals(0, rdfAfter.size());
    }
}
