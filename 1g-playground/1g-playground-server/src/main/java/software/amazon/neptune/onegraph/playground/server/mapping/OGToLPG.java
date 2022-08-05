package software.amazon.neptune.onegraph.playground.server.mapping;

import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGReifiableElement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Performs conversion from {@link OGDataset}s to {@link LPGGraph}s.
 */
public class OGToLPG {

    /**
     * The OneGraph dataset this mapper is currently modifying.
     */
    public final OGDataset dataset;

    /**
     *  The mapping configuration
     */
    public final LPGMappingConfiguration configuration;

    /**
     * Create a mapper with an existing dataset and existing configuration.
     * @param dataset An existing OneGraph dataset.
     * @param configuration The mapping configuration.
     */
    OGToLPG(@NonNull OGDataset dataset,
            @NonNull  LPGMappingConfiguration configuration) {
        this.dataset = dataset;
        this.configuration = configuration;
    }

    /**
     * Creates a {@link LPGGraph} from the current {@link #dataset}.
     * @return The labeled property graph.
     */
    public LPGGraph createLPGFromOGDataset() {
        LPGGraph g = new LPGGraph();
        Map<OGReifiableElement, LPGVertex> elemToVertex = new HashMap<>();
        Map<OGReifiableElement, LPGEdge> elemToEdge = new HashMap<>();

        // First add vertices for all simple nodes (that have a vertex ID as linked component)
        for (OGSimpleNodeIRI sn : this.dataset.getVertexSimpleNodes()) {
            LPGVertex v = new LPGVertex();
            elemToVertex.put(sn, v);
            g.addVertex(v);
        }
        // Then handle all relationship statements.
        for (OGRelationshipStatement statement : this.dataset.getRelationshipStatements()) {
            if (!this.dataset.isStatementAsserted(statement)) {
                continue;
            }
            Optional<LPGEdge> e = this.handleRelationshipStatement(statement, g, elemToVertex);
            e.ifPresent(lpgEdge -> elemToEdge.put(statement, lpgEdge));
        }
        // Then handle all property statements.
        for (OGPropertyStatement statement :  this.dataset.getPropertyStatements()) {
            if (!this.dataset.isStatementAsserted(statement)) {
                continue;
            }
            this.handlePropertyStatement(statement, g, elemToVertex, elemToEdge);
        }
        return g;
    }

    // Adds vertex and edge properties to the given LPG from the given property statement.
    // Only property statements that are of the following form can be translated to LPG:
    // 1. SN - pred - value
    // 2. RelStat - pred - value, and RelStat must be a key in the mapping relToEdge.
    private void handlePropertyStatement(OGPropertyStatement prop, LPGGraph g, Map<OGReifiableElement, LPGVertex> elemToVertex,
                                         Map<OGReifiableElement, LPGEdge> elemToEdge) {
        if (prop.canBecomeNodeProperty()) {
            // This property statement is about a node property or node label.
            LPGVertex v = createOrObtainVertex(prop.subject, elemToVertex);
            g.addVertex(v);
            Optional<String> optLabel = prop.getNodeLabelMappingEntry(this.configuration);
            if (optLabel.isPresent()) {
                v.addLabel(optLabel.get());
            } else {
                v.addPropertyValue(prop.obtainPropName(this.configuration), prop.object.lpgValue());
            }
        } else {
            // This property statement is about an edge property.
            if (elemToEdge.containsKey(prop.subject)) {
                LPGEdge e = elemToEdge.get(prop.subject);
                e.addPropertyValue(prop.obtainPropName(this.configuration), prop.object.lpgValue());
            }
        }
    }

    // Adds new elements to the given LPG inferred from the given relationship statement.
    // Only relationship statements that are of the following form can be translated to LPG:
    // 1. SN - pred - SN
    private Optional<LPGEdge> handleRelationshipStatement(OGRelationshipStatement rel, LPGGraph g,
                                                          Map<OGReifiableElement, LPGVertex> elemToVertex) {
        // Distinguish between relationship statements that say something about a vertex label,
        // and relationship statements that are an edge between 2 vertices.
        if (rel.getNodeLabelMappingEntry(this.configuration).isPresent()) {
            String label = rel.getNodeLabelMappingEntry(this.configuration).get();
            LPGVertex v = createOrObtainVertex(rel.subject, elemToVertex);
            g.addVertex(v);
            v.addLabel(label);
            // This relationship statement did not spawn an edge in LPG so return empty.
            return Optional.empty();
        } else if (rel.canBecomeEdge()) {
            LPGVertex outV = createOrObtainVertex(rel.subject, elemToVertex);
            LPGVertex inV = createOrObtainVertex(rel.object, elemToVertex);

            LPGEdge edge = new LPGEdge(outV, inV, rel.obtainEdgeLabel(this.configuration));
            rel.elementId().ifPresent(edge::setId);
            g.addVertex(outV);
            g.addVertex(inV);
            g.addEdge(edge);
            return Optional.of(edge);
        } else {
            // The relationship statement can not be translated to LPG
            return Optional.empty();
        }
    }

    // Either obtains an existing vertex from the elemToVertex map, or creates and adds
    // a new one. Sets the vertex id.
    public LPGVertex createOrObtainVertex(OGReifiableElement elem, Map<OGReifiableElement, LPGVertex> elemToVertex) {
        LPGVertex outV = elemToVertex.computeIfAbsent(elem, k -> new LPGVertex());
        elem.elementId().ifPresent(outV::setId);
        return outV;
    }
}
