package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;

/**
 * An {@link OGObject} is in {@code object} position of an {@link OGTripleStatement}.
 */
public interface OGObject {

    /**
     * Return {@code true} if this object could be the in-node of a labeled property graph edge,
     * false otherwise.
     * @return  {@code true} or {@code false} depending on if this object could be the in-node of a
     * labeled property graph edge.
     */
    boolean canBeInNodeOfEdge();
}
