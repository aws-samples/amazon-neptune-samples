package software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import software.amazon.neptune.onegraph.playground.server.mapping.TinkerPopConverter;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;

/**
 * Test case to see if warning is thrown when there are meta properties in the TinkerPop graph,
 * calls the {@code delegate} of the {@link TinkerPopConverter}.
 */
public class MetaProperties extends TinkerConversionTestCase {

    @Override
    public Graph tinkerPopGraph() {
        Graph g = TinkerGraph.open();
        Vertex alice = g.addVertex("Person");
        alice.property("child","mark","acl:","user1234");
        return g;
    }

    @Override
    public LPGGraph lpgGraph() {
        LPGGraph graph = new LPGGraph();
        LPGVertex alice = new LPGVertex("Person");
        alice.addPropertyValue("child","mark");
        graph.addVertices(alice);
        return graph;
    }
}
