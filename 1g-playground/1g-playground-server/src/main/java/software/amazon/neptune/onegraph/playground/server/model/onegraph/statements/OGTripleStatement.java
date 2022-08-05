package software.amazon.neptune.onegraph.playground.server.model.onegraph.statements;

import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGObject;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicate;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGReifiableElement;

import java.util.Optional;

/**
 * An {@link OGTripleStatement} has a subject, predicate and object. Accessible by {@link #getSubject()},
 * {@link #getPredicate()}, and {@link #getObject()} respectively.
 * Both {@link OGPropertyStatement} and {@link OGRelationshipStatement} are subclasses.
 */
public abstract class OGTripleStatement extends OGReifiableElement implements OGStatement {

    /**
     * Gets the subject of this triple statement.
     * @return The subject.
     */
    public abstract OGReifiableElement getSubject();

    /**
     * Gets the predicate of this triple statement.
     * @return The predicate.
     */
    public abstract OGPredicate<?> getPredicate();

    /**
     * Gets the object of this triple statement.
     * @return The object.
     */
    public abstract OGObject getObject();
}
