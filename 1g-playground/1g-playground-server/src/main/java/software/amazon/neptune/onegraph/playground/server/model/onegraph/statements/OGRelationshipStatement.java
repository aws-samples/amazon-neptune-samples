package software.amazon.neptune.onegraph.playground.server.model.onegraph.statements;

import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGObject;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicate;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGReifiableElement;
import lombok.NonNull;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import java.util.Optional;

/**
 * An {@link OGRelationshipStatement} contains a {@code subject}, {@code predicate} and {@code object}.
 * The {@code subject} is an {@link OGReifiableElement},
 * the {@code predicate} is an {@link OGPredicate},
 * the {@code object} is an {@link OGReifiableElement}.
 */
public class OGRelationshipStatement extends OGTripleStatement {

    /**
     * The subject of this statement, gets returned in {@link #getSubject()}.
     */
    public OGReifiableElement subject;

    /**
     * The predicate of this statement, gets returned in {@link #getPredicate()}.
     */
    public OGPredicate<?> predicate;

    /**
     * The object of this statement, gets returned in {@link #getObject()}.
     */
    public OGReifiableElement object;

    /**
     * When LPG data is loaded, edges get transformed into relationship
     * statements, we need this property to keep track of the identifier
     * of those edges. This  is returned in {@link #elementId()}, if it is not {@code null}.
     */
    public Object edgeIDLPG;

    /**
     * Creates a new relationship statement with a {@code subject} and {@code object}.
     * @param subject Set to {@link #subject}.
     * @param object Set to {@link #object}.
     */
    public OGRelationshipStatement(@NonNull OGReifiableElement subject,
                                   @NonNull OGReifiableElement object) {
        this.subject = subject;
        this.object = object;
    }

    /**
     * Creates a new relationship statement with a {@code predicate}.
     * @param predicate Set to {@link #predicate}.
     */
    public OGRelationshipStatement(@NonNull OGPredicate<?> predicate) {
        this.predicate = predicate;
    }

    /**
     * Creates a new relationship statement with {@code subject}, {@code predicate}, and {@code object}.
     * @param subject Set to {@link #subject}.
     * @param predicate Set to {@link #predicate}.
     * @param object Set to {@link #object}.
     */
    public OGRelationshipStatement(@NonNull OGReifiableElement subject,
                                   @NonNull OGPredicate<?> predicate,
                                   @NonNull OGReifiableElement object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    /**
     * Obtains the edge label through the configuration, if there is no entry returns the
     * underlying value of the {@code predicate}
     * @param configuration The configuration to look in.
     * @return The edge label.
     */
    public String obtainEdgeLabel(LPGMappingConfiguration configuration) {
        String edgeLabel = "";
        if (this.predicate.iriPredicate().isPresent()) {
            IRI predicate = this.predicate.iriPredicate().get();
            edgeLabel = configuration.edgeLabelMappingInverse(predicate).orElse(predicate.toString());
        }
        if (this.predicate.stringPredicate().isPresent()) {
            String predicate = this.predicate.stringPredicate().get();
            edgeLabel = configuration.edgeLabelMappingInverse(predicate).orElse(predicate);
        }
        return edgeLabel;
    }

    @Override
    public Optional<Resource> convertToResource() {
        return Optional.empty();
    }

    @Override
    public OGReifiableElement getSubject() {
        return subject;
    }

    @Override
    public OGPredicate<?> getPredicate() {
        return predicate;
    }

    @Override
    public OGObject getObject() {
        return object;
    }

    @Override
    public boolean canBeSubjectOfNodeProperty() {
        return false;
    }

    @Override
    public boolean canBeSubjectOfEdgeProperty() {
        return this.subject.canBeOutNodeOfEdge() && this.object.canBeInNodeOfEdge();
    }

    @Override
    public Optional<String> getNodeLabelMappingEntry(LPGMappingConfiguration configuration) {
        Optional<IRI> objectIRI = object.convertToIRI();

        // The object must be an IRI for it to have a node label mapping entry.
        if (objectIRI.isPresent()) {
            IRI object = objectIRI.get();

            Optional<IRI> predicateIRI = this.predicate.iriPredicate();
            // If there is a node label mapping entry for this predicate/object combo then return it.
            if (predicateIRI.isPresent()) {
                Optional<String> label = configuration.nodeLabelMappingInverse(predicateIRI.get(), object);
                if (label.isPresent()) {
                    return label;
                }
                // If the predicate equals the default node label predicate return the string represenation of the iri.
                if (configuration.defaultNodeLabelPredicate.equals(predicateIRI.get())) {
                    return Optional.of(object.toString());
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<IRI> convertToIRI() {
        return Optional.empty();
    }

    @Override
    public boolean canBecomeEdge() {
        return subject.canBeOutNodeOfEdge() && object.canBeInNodeOfEdge();
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
    public boolean canBeInNodeOfEdge() {
        return false;
    }

    @Override
    public boolean canBeOutNodeOfEdge() {
        return false;
    }

    @Override
    public String toString() {
        return this.formalName() + " -> [subject: " + this.subject.formalName() +
                " predicate: " + this.predicate + " object:" + this.object.formalName() + "]";
    }

    @Override
    public String formalName() {
        return "(REL" + this.localId + ")";
    }

    @Override
    public Optional<Object> elementId() {
        if (this.edgeIDLPG != null) {
            return Optional.of(this.edgeIDLPG);
        } else {
            // Create an ID for the edge using its localId
            String edgeID = "E" + this.localId;
            return Optional.of(edgeID);
        }
    }
}
