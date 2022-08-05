package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import lombok.NonNull;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;

/**
 * An {@link OGSimpleNode} represents a OneGraph simple node.
 * @param <T> The type of the {@link #linkedComponent}, either {@link BNode} or {@link IRI}.
 */
public abstract class OGSimpleNode<T> extends OGReifiableElement implements LPGMappable {

    /**
     * The linked component of this simple node, the type can either be {@link BNode}, {@link IRI} or, {@link LPGVertex}
     */
    public final T linkedComponent;

    /**
     * Creates a new simple node with given linked component.
     * @param linkedComponent The {@link #linkedComponent} must be either {@link BNode}, {@link IRI} or, {@link LPGVertex}
     */
    OGSimpleNode(@NonNull T linkedComponent) {
        if (!(linkedComponent instanceof BNode || linkedComponent instanceof IRI || linkedComponent instanceof LPGVertex)) {
            throw new IllegalArgumentException();
        }
        this.linkedComponent = linkedComponent;
    }

    /**
     * Creates a new simple node with given linked component.
     * and calls {@link #setLocalId(long)}
     * @param linkedComponent Is set to the {@link #linkedComponent}.
     * @param localID Is set in {@link #setLocalId(long)}
     */
    OGSimpleNode(@NonNull T linkedComponent, long localID) {
        if (!(linkedComponent instanceof BNode || linkedComponent instanceof IRI || linkedComponent instanceof LPGVertex)) {
            throw new IllegalArgumentException();
        }
        this.linkedComponent = linkedComponent;
        this.setLocalId(localID);
    }

    @Override
    public boolean canBeInNodeOfEdge() {
        return true;
    }

    @Override
    public boolean canBeOutNodeOfEdge() {
        return true;
    }

    @Override
    public boolean canBeSubjectOfEdgeProperty() {
        return false;
    }

    @Override
    public String formalName() {
        return "(SN" + this.localId + ")";
    }

    @Override
    public boolean canBecomeNodeProperty() {
        return false;
    }

    @Override
    public boolean canBecomeEdgeProperty() {
        return false;
    }

    @Override
    public boolean canBecomeEdge() {
        return false;
    }

    @Override
    public Optional<String> getNodeLabelMappingEntry(LPGMappingConfiguration configuration) {
        return Optional.empty();
    }
}
