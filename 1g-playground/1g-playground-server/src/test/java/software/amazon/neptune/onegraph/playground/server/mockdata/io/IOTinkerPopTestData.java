package software.amazon.neptune.onegraph.playground.server.mockdata.io;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

/**
 * Contains different test scenarios for TinkerPop {@link Graph}s
 */
public class IOTinkerPopTestData {
    public static Graph tinkerPopGraphAliceKnowsBobWithEmptyNode() {
        Graph g = TinkerGraph.open();
        Vertex alice = g.addVertex("Person");
        Vertex bob   = g.addVertex("Person");
        g.addVertex();

        alice.property("name","Alice");
        bob.property("name","Bob");

        alice.addEdge("knows",bob,"since",1955);

        return g;
    }

    public static Graph tinkerPopGraphConflictingKeyForGraphML() {
        Graph g = TinkerGraph.open();
        Vertex alice = g.addVertex("Person");
        Vertex bob   = g.addVertex("Person");
        g.addVertex();

        alice.property("name","Alice");
        bob.property("labelV","thisWillProduceConflict");

        alice.addEdge("labelE",bob,"labelE",1955);

        return g;
    }
}
