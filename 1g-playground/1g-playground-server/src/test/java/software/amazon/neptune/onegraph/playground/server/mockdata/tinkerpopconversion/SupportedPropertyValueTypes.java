package software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;

/**
 * Test case with property values of all supported types.
 */
public class SupportedPropertyValueTypes extends TinkerConversionTestCase {

    @Override
    public Graph tinkerPopGraph() {
        Graph g = TinkerGraph.open();
        Vertex alice = g.addVertex("Person");
        alice.property("name", "Alice");
        alice.property("age", 44);
        alice.property("height", 1.87f);
        alice.property("secondsLived", 112e5d);
        alice.property("memoryAddress", 0x04);
        alice.property("birthDay", get1985());
        alice.property("cash", 1234L);
        alice.property("amountOfFriends", (short) 12);

        return g;
    }

    @Override
    public LPGGraph lpgGraph() {
        LPGGraph graph = new LPGGraph();
        LPGVertex alice = new LPGVertex("Person");
        alice.addPropertyValue("name", "Alice");
        alice.addPropertyValue("age", 44);
        alice.addPropertyValue("height", 1.87f);
        alice.addPropertyValue("secondsLived", 112e5d);
        alice.addPropertyValue("memoryAddress", 0x04);
        alice.addPropertyValue("birthDay", get1985());
        alice.addPropertyValue("cash", 1234L);
        alice.addPropertyValue("amountOfFriends", (short) 12);

        graph.addVertices(alice);
        return graph;
    }
}
