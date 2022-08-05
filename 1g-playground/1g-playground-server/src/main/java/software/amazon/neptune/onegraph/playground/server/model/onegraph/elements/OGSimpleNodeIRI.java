package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import lombok.NonNull;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;

import java.util.Optional;

/**
 * An {@link OGSimpleNodeIRI} represents a OneGraph simple node that has a {@link #linkedComponent}
 * of type {@link IRI}.
 */
public class OGSimpleNodeIRI extends OGSimpleNode<IRI> implements OGPredicate<IRI> {

    /**
     * Creates a new simple node with a {@link #linkedComponent} of type {@link IRI}.
     * @param linked Is set to the {@link #linkedComponent}.
     */
    public OGSimpleNodeIRI(@NonNull IRI linked) {
        super(linked);
    }

    /**
     * Creates a new simple node with a {@link #linkedComponent} of type {@link IRI}
     * and calls {@link #setLocalId(long)}
     * @param linked Is set to the {@link #linkedComponent}.
     * @param localID Is set in {@link #setLocalId(long)}
     */
    public OGSimpleNodeIRI(@NonNull IRI linked, long localID) {
        super(linked, localID);
    }

    @Override
    public Optional<Resource> convertToResource() {
        return Optional.of(linkedComponent);
    }

    @Override
    public Optional<String> stringPredicate() {
        return Optional.empty();
    }

    @Override
    public Optional<IRI> iriPredicate() {
        return Optional.of(linkedComponent);
    }

    @Override
    public boolean canBeSubjectOfNodeProperty() {
        return true;
    }

    @Override
    public Optional<IRI> convertToIRI() {
        return Optional.of(linkedComponent);
    }

    @Override
    public String toString() {
        return this.formalName() + " -> [IRI: " + this.linkedComponent + "]";
    }

    @Override
    public Optional<Object> elementId() {
        // Either use the actual full IRI as an id, or take the local name if the
        // namespace of the iri is URLConstants.ID
        if (this.linkedComponent.getNamespace().equals(URLConstants.ID)) {
            return Optional.of(this.linkedComponent.getLocalName());
        }
        return Optional.of(linkedComponent.toString());
    }
}
