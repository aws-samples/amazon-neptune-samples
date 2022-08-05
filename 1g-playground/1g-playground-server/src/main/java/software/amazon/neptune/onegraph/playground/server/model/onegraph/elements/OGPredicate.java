package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;
import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;

/**
 * An {@link OGPredicate} is in {@code predicate} position of an {@link OGTripleStatement}.
 * @param <T> The type of the {@link #predicate()}, either {@link IRI} or {@link String}.
 */
public interface OGPredicate<T> {

    /**
     * Returns the {@link String} if this predicate is of type {@link String}.
     * @return A {@link String} or the empty optional.
     */
    Optional<String> stringPredicate();

    /**
     * Returns the {@link IRI} if this predicate is of type {@link IRI}.
     * @return An {@link IRI} or the empty optional.
     */
    Optional<IRI> iriPredicate();
}
