package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;
import lombok.NonNull;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * An {@link OGGraph} represents a OneGraph graph element.
 * @param <T> The type of the {@code graphName}, either {@link BNode} or {@link IRI}
 */
public class OGGraph<T> extends OGElement {

    /**
     * When the {@code graphName} is set to this IRI, then this graph represents the RDF default graph.
     */
    public static final IRI DEFAULT_GRAPH_IRI = SimpleValueFactory.getInstance().createIRI(URLConstants.DEFAULT_GRAPH);

    /**
     * The name of this graph, the type can either be {@link BNode} or {@link IRI}.
     */
    public final T graphName;

    /**
     * Creates a new graph with given name.
     * @param graphName The name of this graph, must be either {@link BNode} or {@link IRI},
     *                  specify {@link #DEFAULT_GRAPH_IRI} to indicate that this is the default graph.
     */
    public OGGraph(@NonNull T graphName) {
        if (!(graphName instanceof BNode || graphName instanceof IRI)) {
            throw new IllegalArgumentException();
        }
        this.graphName = graphName;
    }

    /**
     * Creates a new graph that is the default graph.
     * @return The new graph.
     */
    public static OGGraph<?> defaultGraph() {
        return new OGGraph<>(DEFAULT_GRAPH_IRI);
    }

    @Override
    public String toString() {
        return this.formalName() + " -> [name: " + this.graphName + "]";
    }

    @Override
    public String formalName() {
        return "(GR" + this.localId + ")";
    }
}
