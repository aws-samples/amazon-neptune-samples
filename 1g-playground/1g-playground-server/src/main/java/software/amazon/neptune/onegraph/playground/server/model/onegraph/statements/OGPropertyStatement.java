package software.amazon.neptune.onegraph.playground.server.model.onegraph.statements;

import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGObject;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicate;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGReifiableElement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;
import lombok.NonNull;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;

import java.util.Optional;

/**
 * An {@link OGPropertyStatement} contains a {@code subject}, {@code predicate} and {@code object}.
 * The {@code subject} is an {@link OGReifiableElement},
 * the {@code predicate} is an {@link OGPredicate},
 * the {@code object} is an {@link OGValue}.
 */
public class OGPropertyStatement extends OGTripleStatement {

    /**
     * The subject of this statement, gets returned in {@link #getSubject()}.
     */
    public OGReifiableElement subject;

    /**
     * The predicate of this statement, gets returned in {@link #getPredicate()}.
     */
    public final OGPredicate<?> predicate;

    /**
     * The object of this statement, gets returned in {@link #getObject()}.
     */
    public final OGValue<?> object;

    /**
     * Creates a new property statement with {@code predicate} and {@code object}.
     * @param predicate Set to {@link #predicate}.
     * @param object Set to {@link #object}.
     */
    public OGPropertyStatement(@NonNull OGPredicate<?> predicate,
                               @NonNull  OGValue<?> object) {
        this.predicate = predicate;
        this.object = object;
    }

    /**
     * Creates a new property statement with {@code subject}, {@code predicate}, and {@code object}.
     * @param subject Set to {@link #subject}.
     * @param predicate Set to {@link #predicate}.
     * @param object Set to {@link #object}.
     */
    public OGPropertyStatement(@NonNull OGReifiableElement subject,
                               @NonNull OGPredicate<?> predicate,
                               @NonNull OGValue<?> object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    /**
     * Obtains the property name through the configuration, if there is no entry returns the
     * underlying value of the {@code predicate}
     * @param configuration The configuration to look in.
     * @return The property name.
     */
    public String obtainPropName(@NonNull LPGMappingConfiguration configuration) {
        String propName = "";
        if (predicate.stringPredicate().isPresent()) {
            String pred = predicate.stringPredicate().get();
            propName = configuration.propertyNameMappingInverse(pred).orElse(pred);
        }
        if (predicate.iriPredicate().isPresent()) {
            IRI pred = predicate.iriPredicate().get();
            propName = configuration.propertyNameMappingInverse(pred).orElse(pred.toString());
        }
        return propName;
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
    public boolean canBecomeNodeProperty() {
        return this.subject.canBeSubjectOfNodeProperty();
    }

    @Override
    public boolean canBecomeEdgeProperty() {
        return this.subject.canBeSubjectOfEdgeProperty();
    }

    @Override
    public boolean canBeSubjectOfNodeProperty() {
        return false;
    }

    @Override
    public boolean canBeSubjectOfEdgeProperty() {
        return false;
    }

    @Override
    public Optional<String> getNodeLabelMappingEntry(@NonNull LPGMappingConfiguration configuration) {
        String objectString = object.value.toString();

        Optional<IRI> predicateIRI = this.predicate.iriPredicate();

        if (predicateIRI.isPresent()) {
            Optional<String> label = configuration.nodeLabelMappingInverse(predicateIRI.get(), objectString);
            if (label.isPresent()) {
                return label;
            }
            // If the predicate equals the default node label predicate return the object string.
            if (configuration.defaultNodeLabelPredicate.equals(predicateIRI.get())) {
                return Optional.of(objectString);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Resource> convertToResource() {
        return Optional.empty();
    }

    @Override
    public Optional<IRI> convertToIRI() {
        return Optional.empty();
    }

    @Override
    public boolean canBecomeEdge() {
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
                " predicate: " + this.predicate + " object: " + this.object + "]";
    }

    @Override
    public String formalName() {
        return "(PRP" + this.localId + ")";
    }

    @Override
    public Optional<Object> elementId() {
        return Optional.empty();
    }
}
