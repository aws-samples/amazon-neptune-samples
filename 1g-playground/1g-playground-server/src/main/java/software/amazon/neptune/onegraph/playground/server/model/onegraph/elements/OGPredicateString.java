package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import lombok.NonNull;
import org.eclipse.rdf4j.model.IRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;

import java.util.Optional;

/**
 * An {@link OGPredicateString} can appear in the {@code predicate} position of an {@link OGTripleStatement},
 * it has underlying {@link #content}.
 */
public class OGPredicateString implements OGPredicate<String> {

    /**
     * The string content of this predicate.
     */
    public final String content;

    /**
     * Creates a new simple node with its {@link #content} set to {@code content}.
     * @param content Is set to the {@link #content}.
     */
    public OGPredicateString(@NonNull String content) {
        this.content = content;
    }

    @Override
    public Optional<String> stringPredicate() {
        return Optional.of(this.content);
    }

    @Override
    public Optional<IRI> iriPredicate() {
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "\"" + content + "\"";
    }
}
