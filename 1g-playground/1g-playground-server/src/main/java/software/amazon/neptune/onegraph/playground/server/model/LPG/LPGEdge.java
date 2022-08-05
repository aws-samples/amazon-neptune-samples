package software.amazon.neptune.onegraph.playground.server.model.LPG;

import lombok.NonNull;

/**
 * Implementation of the formal definition of a property graph edge.
 */
public class LPGEdge extends LPGElement {

    /**
     * The label of this edge, every edge must have a label.
     */
    public final String label;

    /**
     * The out-vertex of this edge, (the vertex that has this edge outgoing)
     */
    public final LPGVertex outVertex;

    /**
     * The in-vertex of this edge, (the vertex that has this edge incoming)
     */
    public final LPGVertex inVertex;

    /**
     * Creates a new edge
     * @param outVertex The out-vertex
     * @param inVertex The in-vertex
     * @param label The label of this edge
     */
    public LPGEdge(@NonNull LPGVertex outVertex, @NonNull LPGVertex inVertex, @NonNull String label) {
        this.outVertex = outVertex;
        this.inVertex  = inVertex;
        this.label = label;
    }

    @Override
    public String toString() {
        return this.outVertex + "-(" + this.label + ")-" + this.inVertex;
    }
}
