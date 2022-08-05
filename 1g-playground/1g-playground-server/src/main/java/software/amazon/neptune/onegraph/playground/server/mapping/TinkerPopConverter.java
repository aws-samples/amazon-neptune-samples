package software.amazon.neptune.onegraph.playground.server.mapping;

import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import lombok.NonNull;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs conversions between TinkerPop {@link Graph}s and {@link LPGGraph}s in both directions.
 */
public class TinkerPopConverter {

    /**
     * The delegate gets called when certain unsupported elements are encountered during the conversion.
     */
    public final TinkerPopConverterDelegate delegate;

    private final String INFORM_TYPE_NOT_SUPPORTED = "The property with key %s and value %s will be ignored, reason: Property values of type LIST, SET, and MAP are currently not supported";
    private final String INFORM_META_NOT_SUPPORTED = "The meta-property on the property with key %s will be ignored, reason: Meta-properties are currently not supported";

    private final String INFORM_MULTI_LABEL_NOT_SUPPORTED = "Multi-labeled vertex encountered; The vertex with labels %s has multiple labels, multiple labels are currently not supported for Gremlin, only the first label %s will be queryable.";

    /**
     * Delegate, gets called when:
     * - An unsupported property value is found (when translating from TinkerPop to LPG)
     * - A meta-property is found (when translating from TinkerPop to LPG)
     * - A vertex is found with multiple labels (when translating from LPG to TinkerPop)
     */
    public interface TinkerPopConverterDelegate {

        /**
         * Can be called during conversion from {@link Graph}s to {@link LPGGraph}s, when a property value is
         * encountered with an unsupported value.<br />
         * The following value types are not supported: <br />
         * - SET <br />
         * - LIST <br />
         * - MAP <br />
         * @param infoString String with information for the user about which property had the unsupported type.
         */
        void unsupportedValueFound(String infoString);

        /**
         * Can be called during conversion from {@link Graph}s to {@link LPGGraph}s, when a meta-property is detected on a vertex.
         * @param infoString String with information for the user about which property had the meta-property.
         */
        void metaPropertyFound(String infoString);

        /**
         * Can be called during conversion from {@link LPGGraph}s to {@link Graph}s, when a multi-labeled vertex is encountered.
         * @param infoString String with information for the user about which vertex had the multi-labeled property.
         */
        void multiLabeledVertexFound(String infoString);
    }

    /**
     * Create a new {@link TinkerPopConverter} with given a {@link TinkerPopConverterDelegate}
     */
    public TinkerPopConverter(TinkerPopConverterDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Converts a Tinkerpop {@link Graph} to a {@link LPGGraph}
     * @param graph The graph to convert.
     * @return A labeled property graph
     */
    public LPGGraph convertTinkerGraphToLPG(@NonNull Graph graph) throws ConverterException {
        LPGGraph resultG = new LPGGraph();
        Map<Vertex, LPGVertex> tinkerVtoLPGV = addVerticesToLPG(resultG, graph);
        addEdgesToLPG(resultG, graph, tinkerVtoLPGV);

        return resultG;
    }

    /**
     * Converts an {@link LPGGraph} to a TinkerPop {@link Graph}
     * @param graph The graph to convert.
     * @return A TinkerPop graph
     */
    public Graph convertLPGToTinkerGraph(@NonNull LPGGraph graph) {
        TinkerGraph resultG = TinkerGraph.open();
        Map<LPGVertex, Vertex> lpgVToTinkerV = addVerticesToTinkerGraph(graph, resultG);
        addEdgesToTinkerGraph(graph, lpgVToTinkerV);

        return resultG;
    }

    // Adds the vertices to the lpg together with their properties
    // Returns a mapping from tinker vertices to lpg vertices
    private Map<Vertex, LPGVertex> addVerticesToLPG(LPGGraph lpgGraph, Graph tinkerGraph) {
        Map<Vertex, LPGVertex> tinkerVtoLPGV = new HashMap<>();
        // Iterate all tinker vertices, create new lpg vertices.
        for (Iterator<Vertex> it = tinkerGraph.vertices(); it.hasNext();) {
            Vertex tinkerV = it.next();
            LPGVertex lpgV = new LPGVertex();
            lpgV.setId(tinkerV.id());
            lpgV.addLabel(tinkerV.label());

            tinkerVtoLPGV.put(tinkerV, lpgV);
            lpgGraph.addVertex(lpgV);

            addPropertiesOfTinkerVertexToLPGVertex(tinkerV, lpgV);
        }
        return tinkerVtoLPGV;
    }

    // Not all values are supported in the playground, values that are NOT supported:
    // lists
    // maps
    // sets
    private boolean isValueSupported(Object value) {
        return !(value instanceof Map || value instanceof List || value instanceof Set);
    }

    // Calls the delegate.
    private void informDelegateOfUnsupportedValue(Object key, Object value) {
        this.delegate.unsupportedValueFound(String.format(INFORM_TYPE_NOT_SUPPORTED, key, value));
    }

    // Calls the delegate.
    private void informDelegateOfUnsupportedMetaProperty(Object key) {
        this.delegate.metaPropertyFound(String.format(INFORM_META_NOT_SUPPORTED, key));
    }

    // Calls the delegate.
    private void informDelegateOfMultiLabeledVertex(Collection<String> labels, String label) {
        this.delegate.multiLabeledVertexFound(String.format(INFORM_MULTI_LABEL_NOT_SUPPORTED, labels, label));
    }

    private void addPropertiesOfTinkerVertexToLPGVertex(Vertex tinkerV, LPGVertex lpgV) {

        Iterator<VertexProperty<Object>> itProp = tinkerV.properties();

        while (itProp.hasNext()) {
            VertexProperty<Object> tinkerProperty = itProp.next();

            // Check for meta-properties
            if (tinkerProperty.properties().hasNext()) {
                // Inform the user that meta-properties can not be translated and will be ignored.
                this.informDelegateOfUnsupportedMetaProperty(tinkerProperty.key());
            }

            if (tinkerProperty.value() != null) {
                // Property has SINGLE cardinality.

                // Check if the value is of unsupported type such as LIST, SET, or MAP
                if (!this.isValueSupported(tinkerProperty.value())) {
                    this.informDelegateOfUnsupportedValue(tinkerProperty.key(), tinkerProperty.value());
                    continue;
                }
                lpgV.addPropertyValue(tinkerProperty.key(), tinkerProperty.value());
            } else if (tinkerProperty.values() != null) {
                // Property has SET or LIST cardinality.

                Iterator<Object> itValues = tinkerProperty.values();
                while (itValues.hasNext()) {
                    Object value = itValues.next();
                    if (!this.isValueSupported(value)) {
                        this.informDelegateOfUnsupportedValue(tinkerProperty.key(), value);
                        continue;
                    }
                    lpgV.addPropertyValue(tinkerProperty.key(), value);
                }
            } else {
                // Property has no value, add an empty property to the LPG.
                LPGProperty emptyProperty = new LPGProperty(tinkerProperty.key());
                lpgV.addProperty(emptyProperty);
            }
        }
    }

    // Takes all properties tied to a TinkerPop element and adds similar labeled property graph properties to the
    // lpgElem
    private void addPropertiesOfTinkerEdgeToLPGEdge(Edge tinkerEdge, LPGEdge lpgEdge) {

        Iterator<? extends Property<Object>> itProp = tinkerEdge.properties();

        while (itProp.hasNext()) {
            Property<Object> tinkerProperty = itProp.next();

            if (tinkerProperty.isPresent()) {
                // Check if the value is of unsupported type such as LIST, SET, or MAP
                if (!this.isValueSupported(tinkerProperty.value())) {
                    this.informDelegateOfUnsupportedValue(tinkerProperty.key(), tinkerProperty.value());
                    continue;
                }
                lpgEdge.addPropertyValue(tinkerProperty.key(), tinkerProperty.value());
            } else {
                LPGProperty emptyProperty = new LPGProperty(tinkerProperty.key());
                lpgEdge.addProperty(emptyProperty);
            }
        }
    }

    // Creates and adds edges to the given labeled property graph similar to the edges in the given TinkerPop graph.
    private void addEdgesToLPG(LPGGraph lpgGraph, Graph tinkerGraph, Map<Vertex, LPGVertex> tinkerVtoLPGV) throws ConverterException {
        for (Iterator<Edge> it = tinkerGraph.edges(); it.hasNext();) {
            Edge tinkerE = it.next();
            // Get the LPG vertices that correspond to the tinker vertices.
            LPGVertex outV = tinkerVtoLPGV.get(tinkerE.outVertex());
            LPGVertex inV  = tinkerVtoLPGV.get(tinkerE.inVertex());

            if (outV == null || inV == null) {
                throw new ConverterException(String.format("The edge %s has an in-vertex or out-vertex that is not in the graph.",tinkerE));
            }
            LPGEdge lpgE = new LPGEdge(outV, inV, tinkerE.label());
            lpgE.setId(tinkerE.id());
            lpgGraph.addEdge(lpgE);

            addPropertiesOfTinkerEdgeToLPGEdge(tinkerE, lpgE);
        }
    }

    // Takes all properties tied to a labeled property graph element and adds similar TinkerPop properties to the given
    // tinkerElem
    private void addPropertiesOfLPGVertexToTinkerVertex(Vertex tinkerV, LPGVertex lpgV) {
        for (LPGProperty lpgProp : lpgV.getProperties()) {
            if (lpgProp.values.size() == 0) {
                tinkerV.property(lpgProp.name);
            } else if (lpgProp.values.size() == 1) {
                tinkerV.property(lpgProp.name, lpgProp.values.toArray()[0]);
            } else {
                for (Object value : lpgProp.values) {
                    tinkerV.property(VertexProperty.Cardinality.set, lpgProp.name, value);
                }
            }
        }
    }

    private static void addPropertiesOfLPGEdgeToTinkerEdge(Edge tinkerE, LPGEdge lpgE) {
        for (LPGProperty lpgProp : lpgE.getProperties()) {
            if (lpgProp.values.size() == 0) {
                tinkerE.property(lpgProp.name);
            } else {
                tinkerE.property(lpgProp.name, lpgProp.values.toArray()[0]);
            }
        }
    }

    // Creates and adds edges to the TinkerPop vertices in the given lpgVToTinkerV
    private void addEdgesToTinkerGraph(LPGGraph lpgGraph, Map<LPGVertex, Vertex> lpgVToTinkerV) {
        for (LPGEdge lpgE : lpgGraph.edges) {
            // Get the Tinker vertices that correspond to the LPG vertices.
            Vertex outV = lpgVToTinkerV.get(lpgE.outVertex);
            Vertex inV  = lpgVToTinkerV.get(lpgE.inVertex);

            Edge e = outV.addEdge(lpgE.label, inV, T.id, lpgE.getId());

            // Add properties to the edge.
            addPropertiesOfLPGEdgeToTinkerEdge(e, lpgE);
        }
    }

    // Creates and adds vertices to the given TinkerPop graph from the given labeled property graph.
    private Map<LPGVertex, Vertex> addVerticesToTinkerGraph(LPGGraph lpgGraph, Graph tinkerGraph) {
        Map<LPGVertex, Vertex> lpgVToTinkerV = new HashMap<>();
        for (LPGVertex lpgV : lpgGraph.vertices) {

            // Create a new TinkerPop vertex in the tinker graph, set its id.
            // getLabels() will always return at least 1 element.
            Collection<String> labels = lpgV.getLabels();

            // Inform the delegate if needed.
            if (lpgV.getLabels().size() > 1) {
                this.informDelegateOfMultiLabeledVertex(labels, labels.iterator().next());
            }
            Vertex tinkerV = tinkerGraph.addVertex(T.id, lpgV.getId(), T.label, labels.iterator().next());
            lpgVToTinkerV.put(lpgV, tinkerV);

            addPropertiesOfLPGVertexToTinkerVertex(tinkerV, lpgV);
        }
        return lpgVToTinkerV;
    }
}
