package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import org.eclipse.rdf4j.model.IRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;
import org.eclipse.rdf4j.model.Resource;

import java.util.Optional;

/**
 * An {@link OGReifiableElement} appears in the subject or object position of a {@link OGTripleStatement},
 * every reifiable element is also mappable to LPG.
 */
public abstract class OGReifiableElement extends OGElement implements OGObject, LPGMappable {

    /**
     * Returns the underlying {@link IRI} of this element if it has one.
     * @return An optional containing the {@link IRI} or the empty optional.
     */
    public abstract Optional<IRI> convertToIRI();

    /**
     * Returns the underlying {@link Resource} of this element if it has one.
     * @return An optional containing the {@link Resource} or the empty optional.
     */
    public abstract Optional<Resource> convertToResource();

    /**
     * Return {@code true} if this element can be the subject of an LPG node property.
     * @return  {@code true} or {@code false} depending on if this object can be the subject of an LPG node property.
     */
    public abstract boolean canBeSubjectOfNodeProperty();

    /**
     * Return {@code true} if this element can be the subject of an LPG edge property.
     * @return  {@code true} or {@code false} depending on if this object can be the subject of an LPG edge property.
     */
    public abstract boolean canBeSubjectOfEdgeProperty();

    /**
     * Return {@code true} if this object could be the out-node of a labeled property graph edge,
     * false otherwise.
     * @return  {@code true} or {@code false} depending on if this object could be the out-node of a
     * labeled property graph edge.
     */
    public abstract boolean canBeOutNodeOfEdge();
}
