package software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;

/**
 * Test case where there is mapping between not explicitly labeled vertices.
 */
public class NonExplicitLabels extends TinkerConversionTestCase {
    @Override
    public Graph tinkerPopGraph() {
        Graph g = TinkerGraph.open();
        g.addVertex();
        return g;
    }

    @Override
    public LPGGraph lpgGraph() {
        LPGGraph graph = new LPGGraph();
        LPGVertex alice = new LPGVertex();
        graph.addVertices(alice);
        return graph;
    }
}
