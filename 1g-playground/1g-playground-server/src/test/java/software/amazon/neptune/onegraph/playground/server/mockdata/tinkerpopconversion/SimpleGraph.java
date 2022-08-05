package software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;

/**
 * Alice knows bob graph with properties on the nodes and edge.
 */
public class SimpleGraph extends TinkerConversionTestCase {
    @Override
    public Graph tinkerPopGraph() {
        Graph g = TinkerGraph.open();
        Vertex alice = g.addVertex("Person");
        Vertex bob   = g.addVertex("Person");

        alice.property("name","Alice");
        bob.property("name","Bob");
        alice.addEdge("knows",bob,"since",1955);

        return g;
    }

    @Override
    public LPGGraph lpgGraph() {
        LPGGraph graph = new LPGGraph();
        LPGVertex alice = new LPGVertex("Person");
        LPGVertex bob = new LPGVertex("Person");

        alice.addPropertyValue("name", "Alice");
        bob.addPropertyValue("name", "Bob");

        LPGEdge knows = new LPGEdge(alice, bob, "knows");
        knows.addPropertyValue("since", 1955);

        graph.addVertices(alice, bob);
        graph.addEdges(knows);

        return graph;
    }
}
