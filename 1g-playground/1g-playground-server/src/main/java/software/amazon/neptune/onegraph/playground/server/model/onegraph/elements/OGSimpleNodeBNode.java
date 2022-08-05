package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import lombok.NonNull;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import java.util.Optional;

/**
 * An {@link OGSimpleNodeBNode} represents a OneGraph simple node that has a {@link #linkedComponent}
 * of type {@link BNode}.
 */
public class OGSimpleNodeBNode extends OGSimpleNode<BNode> {

    /**
     * Creates a new simple node with a {@link #linkedComponent} of type {@link BNode}.
     * @param linked Is set to the {@link #linkedComponent}.
     */
    public OGSimpleNodeBNode(@NonNull BNode linked) {
        super(linked);
    }

    /**
     * Creates a new simple node with a {@link #linkedComponent} of type {@link BNode}
     * and calls {@link #setLocalId(long)}
     * @param linked Is set to the {@link #linkedComponent}.
     * @param localID Is set in {@link #setLocalId(long)}
     */
    public OGSimpleNodeBNode(@NonNull BNode linked, long localID) {
        super(linked, localID);
    }

    @Override
    public Optional<Resource> convertToResource() {
        return Optional.of(linkedComponent);
    }

    @Override
    public boolean canBeSubjectOfNodeProperty() {
        return true;
    }

    @Override
    public Optional<IRI> convertToIRI() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return this.formalName() + " -> [Blank node: " + this.linkedComponent + "]";
    }

    @Override
    public Optional<Object> elementId() {
        String bNodeString = "BNode" + linkedComponent.getID() + "_" + localId;
        return Optional.of(bNodeString);
    }
}
