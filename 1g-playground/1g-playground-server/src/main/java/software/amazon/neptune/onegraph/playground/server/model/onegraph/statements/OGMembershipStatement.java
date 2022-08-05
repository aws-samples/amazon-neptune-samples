package software.amazon.neptune.onegraph.playground.server.model.onegraph.statements;

import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGElement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGGraph;
import lombok.NonNull;

/**
 * An {@link OGMembershipStatement} contains a {@code statement} and a {@code graph}.
 * The {@code statement} is an {@link OGStatement} and the {@code graph} is an {@link OGGraph}
 */
public class OGMembershipStatement extends OGElement {

    /**
     * The statement this membership statement is about.
     */
    public final OGTripleStatement statement;

    /**
     * The graph the {@code statement} is in.
     */
    public final OGGraph<?> graph;

    /**
     * Creates a membership statement and sets the {@link #statement} and {@link #graph} properties from the given
     * parameters.
     * @param statement The statement of this membership statement.
     * @param graph The graph the given statement is in.
     */
    public OGMembershipStatement(@NonNull OGTripleStatement statement, @NonNull OGGraph<?> graph) {
        this.statement = statement;
        this.graph = graph;
    }

    @Override
    public String toString() {
        return this.formalName() + " -> [statement: " + this.statement.formalName() +
                " in: " + this.graph + "]";
    }

    @Override
    public String formalName() {
        return "(MEM" + this.localId + ")";
    }
}
