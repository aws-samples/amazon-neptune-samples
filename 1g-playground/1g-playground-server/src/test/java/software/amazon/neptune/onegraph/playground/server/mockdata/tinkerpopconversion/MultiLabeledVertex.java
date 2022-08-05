package software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import software.amazon.neptune.onegraph.playground.server.mapping.TinkerPopConverter;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;

/**
 * Test case with multi-valued labels of vertices,
 * calls the {@code delegate} of the {@link TinkerPopConverter}.
 *
 * This test case should only be used to go from LPG to Tinker.
 */
public class MultiLabeledVertex extends TinkerConversionTestCase {

    @Override
    public Graph tinkerPopGraph() {
        Graph g = TinkerGraph.open();
        Vertex alice = g.addVertex("Person");
        alice.property("name", "Alice");
        return g;
    }

    @Override
    public LPGGraph lpgGraph() {
        LPGGraph graph = new LPGGraph();
        LPGVertex alice = new LPGVertex("Person","Boss","Woman");
        alice.addPropertyValue("name", "Alice");

        graph.addVertices(alice);
        return graph;
    }
}
