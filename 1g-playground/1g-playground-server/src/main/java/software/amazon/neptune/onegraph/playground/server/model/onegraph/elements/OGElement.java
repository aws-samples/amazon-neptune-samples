package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGStatement;
import lombok.Setter;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;

/**
 * Abstract superclass of {@link OGStatement},
 * {@link OGGraph} and, {@link OGSimpleNode}
 */
public abstract class OGElement {

    /**
     * Elements only have an id in the context of an {@link OGDataset}, their {@code id} is set once they
     * have been added.
     */
    @Setter
    protected long localId = 0;

    /**
     * Get the formal name for this element, the user sees this.
     * @return The formal name of the element.
     */
    public abstract String formalName();
}
