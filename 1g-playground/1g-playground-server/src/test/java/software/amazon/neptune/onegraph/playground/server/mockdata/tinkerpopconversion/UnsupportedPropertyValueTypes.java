package software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.mapping.TinkerPopConverter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Test case to test that MAP and LIST and nested SET property values are ignored,
 * calls the {@code delegate} of the {@link TinkerPopConverter}.
 *
 * This test case should only be used to go from Tinker to LPG
 */
public class UnsupportedPropertyValueTypes extends TinkerConversionTestCase {

    @Override
    public Graph tinkerPopGraph() {
        Graph g = TinkerGraph.open();
        Vertex alice = g.addVertex("Person");

        Map<String, String> children = new HashMap<>();
        children.put("firstBorn","Mark");
        children.put("secondBorn","Charlie");

        List<String> moreChildren = new ArrayList<>();
        moreChildren.add("Raphael");
        moreChildren.add("Michelangelo");
        alice.property("children", children);
        alice.property("moreChildren", moreChildren);

        Set<String> evenMoreChildren = new HashSet<>();
        evenMoreChildren.add("Donatello");
        evenMoreChildren.add("Leonardo");
        alice.property("evenMoreChildren", evenMoreChildren);
        return g;
    }

    @Override
    public LPGGraph lpgGraph() {
        LPGGraph graph = new LPGGraph();
        LPGVertex alice = new LPGVertex("Person");
        graph.addVertices(alice);
        return graph;
    }
}
