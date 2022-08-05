package software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion;

import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;

/**
 * Test case with properties of list and set cardinality.
 */
public class SetAndListCardinalities extends TinkerConversionTestCase {

    @Override
    public Graph tinkerPopGraph() {
        Graph g = TinkerGraph.open();
        Vertex alice = g.addVertex("Person");
        Vertex bob = g.addVertex("Person");
        alice.property(VertexProperty.Cardinality.list, "name", "Alice");
        alice.property(VertexProperty.Cardinality.list, "name", "Bob");
        alice.property(VertexProperty.Cardinality.list, "name", "Charlie");
        alice.property(VertexProperty.Cardinality.set, "age", 44L);
        alice.property(VertexProperty.Cardinality.set, "age", get1985());
        alice.property(VertexProperty.Cardinality.set, "age", "55");

        Edge e = alice.addEdge("knows", bob);
        e.property("age", "55");

        return g;
    }

    @Override
    public LPGGraph lpgGraph() {
        LPGGraph graph = new LPGGraph();
        LPGVertex alice = new LPGVertex("Person");
        LPGVertex bob = new LPGVertex("Person");
        alice.addPropertyValue("name", "Alice");
        alice.addPropertyValue("name", "Bob");
        alice.addPropertyValue("name", "Charlie");
        alice.addPropertyValue("age", 44L);
        alice.addPropertyValue("age", get1985());
        alice.addPropertyValue("age", "55");

        LPGEdge e = new LPGEdge(alice, bob, "knows");
        e.addProperty(new LPGProperty("age", "55"));
        graph.addEdge(e);
        graph.addVertices(alice, bob);
        return graph;
    }
}
