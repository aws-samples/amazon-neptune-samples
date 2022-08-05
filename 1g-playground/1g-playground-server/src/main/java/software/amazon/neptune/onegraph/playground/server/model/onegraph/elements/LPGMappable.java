package software.amazon.neptune.onegraph.playground.server.model.onegraph.elements;

import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;

import java.util.Optional;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGElement;

/**
 * Classes that implement LPGMappable can be mapped to elements of a labeled property graph.
 */
public interface LPGMappable {

    /**
     * Return {@code true} if this class can become a node property.
     * @return {@code true} if this class can become a node property, {@code false} otherwise.
     */
    boolean canBecomeNodeProperty();

    /**
     * Return {@code true} if this class can become an edge property.
     * @return {@code true} if this class can become an edge property, {@code false} otherwise.
     */
    boolean canBecomeEdgeProperty();

    /**
     * Return {@code true} if this class can become an edge.
     * @return {@code true} if this class can become an edge, {@code false} otherwise.
     */
    boolean canBecomeEdge();

    /**
     * Returns the mapped node label according to the configuration if such a mapping existed.
     * @param configuration The mapping configuration in which to look
     * @return An optional containing the mapped label if it could be found in the {@code configuration},
     * an empty optional otherwise.
     */
    Optional<String> getNodeLabelMappingEntry(LPGMappingConfiguration configuration);

    /**
     * Returns the unique id of this object if this object is able to transform itself to an {@link LPGElement},
     * only {@link OGSimpleNode}s can do this.
     * @return The LPG element id of this object or empty optional if this element can not be transformed into a
     * single {@link LPGElement}.
     */
    Optional<Object> elementId();
}
