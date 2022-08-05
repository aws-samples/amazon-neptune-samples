package software.amazon.neptune.onegraph.playground.server.tests.unit.misc;

import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.SimpleReification;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests .toString() of certain model objects, these are mainly for debugging purposes.
 */
public class ToStringTests {

    @Test
    public void lpgGraphToString() {
        LPGGraph graph = new SimpleReification().ogMappedToLPG();
        String graphString = graph.toString();
        assertTrue(graphString.contains("---- Vertices ----"));
        assertTrue(graphString.contains("---- Edges ----"));
    }

    @Test
    public void ogDatasetToString() {
        OGDataset ds = new SimpleReification().underlyingOG();
        String dsString = ds.toString();
        assertTrue(dsString.contains("---- Relationship statements ----"));
        assertTrue(dsString.contains("---- Property statements ----"));
        assertTrue(dsString.contains("---- Membership statements ----"));
        assertTrue(dsString.contains("---- Graphs ----"));
        assertTrue(dsString.contains("---- Simple nodes ----"));
    }
}
