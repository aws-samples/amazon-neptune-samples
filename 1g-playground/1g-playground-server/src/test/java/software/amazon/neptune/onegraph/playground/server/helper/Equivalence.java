package software.amazon.neptune.onegraph.playground.server.helper;

import com.google.common.collect.ImmutableList;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGElement;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Used to test equivalence between data models.
 */
public class Equivalence {

    /**
     * Asserts if the given graphs are of equal cardinality in nodes and edges,
     * and contain vertices and edges with the same labels.
     * @param g1 Graph 1
     * @param g2 Graph 2
     */
    public static void assertTinkerPopGraphsAreSuperficiallyEqual(Graph g1, Graph g2) {
        List<Vertex> v1 = ImmutableList.copyOf(g1.vertices());
        List<Vertex> v2 = ImmutableList.copyOf(g2.vertices());
        List<Edge> e1 = ImmutableList.copyOf(g1.edges());
        List<Edge> e2 = ImmutableList.copyOf(g2.edges());

        assertEquals(v1.size(), v2.size(), "There is a different amount of vertices");
        assertEquals(e1.size(), e2.size(), "There is a different amount of edges");

        for (Edge edge1 : e1) {
            boolean found = false;
            for (Edge edge2 : e2) {
                if (edge1.label().equals(edge2.label())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "There is no common edge with label " + edge1.label());
        }
        for (Vertex vertex1 : v1) {
            boolean found = false;
            for (Vertex vertex2 : v2) {
                if (vertex1.label().equals(vertex2.label())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found,"There is no common vertex with label: " + vertex1.label());
        }

        Collection<Property<?>> pV1 = getTinkerProperties(v1);
        Collection<Property<?>> pV2 = getTinkerProperties(v2);
        Collection<Property<?>> pE1 = getTinkerProperties(e1);
        Collection<Property<?>> pE2 = getTinkerProperties(e2);

        assertEquals(pV1.size(), pV2.size(), "There is a different amount of vertex properties.");
        assertEquals(pE1.size(), pE2.size(), "There is a different amount of edge properties.");

        assertTinkerPropertiesEqual(pV1, pV2);
        assertTinkerPropertiesEqual(pE1, pE2);
    }

    /**
     * Asserts if the given graphs are of equal cardinality in nodes and edges,
     * and contain vertices and edges with the same labels.
     * @param g1 Graph 1
     * @param g2 Graph 2
     */
    public static void assertGraphsAreSuperficiallyEqual(LPGGraph g1, LPGGraph g2) {
        List<LPGVertex> v1 = new ArrayList<>(g1.vertices);
        List<LPGVertex> v2 = new ArrayList<>(g2.vertices);
        List<LPGEdge> e1 = new ArrayList<>(g1.edges);
        List<LPGEdge> e2 = new ArrayList<>(g2.edges);

        assertEquals(v1.size(), v2.size(), "There is a different amount of vertices");
        assertEquals(e1.size(), e2.size(), "There is a different amount of edges");

        for (LPGEdge edge1 : e1) {
            boolean found = false;
            for (LPGEdge edge2 : e2) {
                if (edge1.label.equals(edge2.label)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "There is no common edge with label " + edge1.label);
        }
        for (LPGVertex vertex1 : v1) {
            boolean found = false;
            for (LPGVertex vertex2 : v2) {
                if (collectionEqualsIgnoreOrder(vertex1.getLabels(), vertex2.getLabels())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "There is no common vertex with labels: " + vertex1.getLabels());
        }

        Collection<LPGProperty> pV1 = getLPGProperties(v1);
        Collection<LPGProperty> pV2 = getLPGProperties(v2);
        Collection<LPGProperty> pE1 = getLPGProperties(e1);
        Collection<LPGProperty> pE2 = getLPGProperties(e2);

        assertEquals(pV1.size(), pV2.size(), "There is a different amount of vertex properties.");
        assertEquals(pE1.size(), pE2.size(), "There is a different amount of edge properties.");

        assertLPGPropertiesEqual(pV1, pV2);
        assertLPGPropertiesEqual(pE1, pE2);
    }

    /**
     * Asserts if the given datasets are of equal cardinality in elements.
     * @param d1 Dataset 1
     * @param d2 Dataset 2
     */
    public static void assertOGDatasetsAreSuperficiallyEqual(OGDataset d1, OGDataset d2) {
        assertEquals(Iterables.size(d1.getSimpleNodes()), Iterables.size(d2.getSimpleNodes()),
                "There is a different amount of simple nodes");
        assertEquals(Iterables.size(d1.getGraphs()), Iterables.size(d2.getGraphs()),
                "There is a different amount of graphs");
        assertEquals(Iterables.size(d1.getRelationshipStatements()), Iterables.size(d2.getRelationshipStatements()),
                "There is a different amount of relationship statements");
        assertEquals(Iterables.size(d1.getMembershipStatements()), Iterables.size(d2.getMembershipStatements()),
                "There is a different amount of membership statements");
        assertEquals(Iterables.size(d1.getPropertyStatements()), Iterables.size(d2.getPropertyStatements()),
                "There is a different amount of property statements");
    }

    private static boolean collectionEqualsIgnoreOrder(Collection<?> list1, Collection<?> list2) {
        boolean card = list1.size() == list2.size();
        boolean elems = (new HashSet<>(list1).equals(new HashSet<>(list2)));
        return card && elems;
    }

    private static Collection<LPGProperty> getLPGProperties(List<? extends LPGElement> elements) {
        ArrayList<LPGProperty> properties = new ArrayList<>();
        for (LPGElement e : elements) {
            properties.addAll(e.getProperties());
        }

        return properties;
    }

    private static Collection<Property<?>> getTinkerProperties(List<? extends Element> elements) {
        ArrayList<Property<?>> properties = new ArrayList<>();
        for (Element e : elements) {
            properties.addAll(ImmutableList.copyOf(e.properties()));
        }

        return properties;
    }

    private static void assertLPGPropertiesEqual(Collection<LPGProperty> props1, Collection<LPGProperty> props2) {
        for (LPGProperty p1 : props1) {
            boolean match = false;
            for (LPGProperty p2 : props2) {
                if (p1.name.equals(p2.name)) {
                    if (p1.values.equals(p2.values)) {
                        match = true;
                        break;
                    }
                }
            }
            assertTrue(match, String.format("There was no matching property for: \"%s\"", p1.name));
        }
    }

    private static void assertTinkerPropertiesEqual(Collection<Property<?>> props1, Collection<Property<?>> props2) {
        for (Property<?> p1 : props1) {
            boolean match = false;
            for (Property<?> p2 : props2) {
                if (p1.key().equals(p2.key())) {
                    if (p1.value().equals(p2.value())) {
                        match = true;
                        break;
                    }
                }
            }
            assertTrue(match, String.format("There was no matching property for: \"%s\"", p1.key()));
        }
    }

    public static void assertModelSuperficiallyEqualToSPOMap(Model m, Map<String, List<String>> map) {
        assertEquals(map.get("s").size(), map.get("p").size());
        assertEquals(map.get("s").size(), map.get("o").size());
        assertEquals(map.get("s").size(), m.size());

        for (Resource sub : m.subjects()) {
            assertTrue(map.get("s").contains(sub.toString()));
        }
        for (IRI pred : m.predicates()) {
            assertTrue(map.get("p").contains(pred.toString()));
        }
        for (Value obj : m.objects()) {
            assertTrue(map.get("o").contains(obj.toString()));
        }
    }
}
