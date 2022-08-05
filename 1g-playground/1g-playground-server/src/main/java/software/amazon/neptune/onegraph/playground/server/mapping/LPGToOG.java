package software.amazon.neptune.onegraph.playground.server.mapping;

import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGElement;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicate;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicateString;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGReifiableElement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;
import lombok.NonNull;
import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;

/**
 * Performs conversion from {@link LPGGraph}s to {@link OGDataset}s.
 */
public class LPGToOG {

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
    LPGToOG(@NonNull OGDataset dataset,
            @NonNull  LPGMappingConfiguration configuration) {
        this.dataset = dataset;
        this.configuration = configuration;
    }

    /**
     * Adds the given {@link LPGGraph} to the current {@link #dataset}.
     * @param graph The LPG graph to add.
     */
    public void addLPGToOGDataset(@NonNull LPGGraph graph) {

        // Adds statements for the vertices
        for (LPGVertex vertex : graph.vertices) {
            OGSimpleNode<?> sn = this.dataset.createOrObtainSimpleNode(vertex);
            this.addStatementsForVertexLabels(sn, vertex);
            this.addPropertyStatementsForElementProperties(sn, vertex);
        }
        // Add statements for the edges
        for (LPGEdge edge : graph.edges) {
            OGRelationshipStatement r =
                    this.addOrObtainRelationshipStatementForEdge(edge);
            this.addPropertyStatementsForElementProperties(r, edge);
        }
    }

    // Adds property statements to the dataset for all properties of the given element (vertex/edge)
    private void addPropertyStatementsForElementProperties(OGReifiableElement subElem, LPGElement elem) {
        for (LPGProperty p : elem.getProperties()) {
            for (Object value : p.values) {
                OGValue<?> objValue = new OGValue<>(value);

                Optional<LPGMappingConfiguration.ELMorPNMEntry<?>> entry = this.configuration.propertyNameMapping(p.name);
                if (entry.isPresent()) {
                    entry.get().unpack(new LPGMappingConfiguration.MappingEntryDelegate() {
                        @Override
                        public void handleIRI(IRI i) {
                            OGPredicate<?> pred = dataset.createOrObtainSimpleNode(i);
                            dataset.addStatementAndAddToDefaultGraph(new OGPropertyStatement(subElem, pred, objValue));
                        }

                        @Override
                        public void handleString(String s) {
                            OGPredicate<?> pred = new OGPredicateString(s);
                            dataset.addStatementAndAddToDefaultGraph(new OGPropertyStatement(subElem, pred, objValue));
                        }
                    });
                } else {
                    OGPredicate<?> pred = new OGPredicateString(p.name);
                    dataset.addStatementAndAddToDefaultGraph(new OGPropertyStatement(subElem, pred, objValue));
                }
            }
        }
    }

    // Adds statements to the 1G data set for the vertex labels
    private void addStatementsForVertexLabels(OGSimpleNode<?> snSub, LPGVertex v) {
        for (String label : v.getLabels()) {

            Optional<LPGMappingConfiguration.NLMEntry<?>> entry = this.configuration.nodeLabelMapping(label);

            if (entry.isPresent()) {
                OGSimpleNodeIRI snPred = this.dataset.createOrObtainSimpleNode(entry.get().predicate);

                entry.get().unpack(new LPGMappingConfiguration.MappingEntryDelegate() {
                    @Override
                    public void handleIRI(IRI i) {
                        OGSimpleNodeIRI snObj = dataset.createOrObtainSimpleNode(i);
                        dataset.addStatementAndAddToDefaultGraph(new OGRelationshipStatement(snSub, snPred, snObj));
                    }

                    @Override
                    public void handleString(String s) {
                        OGValue<String> valObj = new OGValue<>(s);
                        dataset.addStatementAndAddToDefaultGraph(new OGPropertyStatement(snSub, snPred, valObj));
                    }
                });
            } else {
                // Use the default node label, add a property statement
                OGSimpleNodeIRI snPred = this.dataset.createOrObtainSimpleNode(this.configuration.defaultNodeLabelPredicate);
                dataset.addStatementAndAddToDefaultGraph(new OGPropertyStatement(snSub, snPred, new OGValue<>(label)));
            }
        }
    }

    // Creates and returns a relationship statement from the given LPG edge
    private OGRelationshipStatement addOrObtainRelationshipStatementForEdge(LPGEdge edge) {
        OGSimpleNode<?> outNode = dataset.createOrObtainSimpleNode(edge.outVertex);
        OGSimpleNode<?> inNode = dataset.createOrObtainSimpleNode(edge.inVertex);

        Object edgeID = edge.getId();
        // Try to obtain an existing relationship statement with this ID.
        Optional<OGRelationshipStatement> optRel = dataset.getRelationshipStatementForLPGEdgeID(edgeID);
        if (optRel.isPresent()) {
            // If the relationship statement already in the data set has the same in and out node
            // it is the same edge, return it.
            if (optRel.get().subject == outNode && optRel.get().object == inNode) {
                return optRel.get();
            } else {
                // There is a relationship statement in the data set with the same linked edge ID.
                // but the in- or out- node does not match, create a new rel with a different unique ID in this case.
                edgeID = String.format("%s-%s-%s", edgeID, edge.outVertex, edge.inVertex);
            }
        }
        OGRelationshipStatement rel = new OGRelationshipStatement(
                dataset.createOrObtainSimpleNode(edge.outVertex),
                dataset.createOrObtainSimpleNode(edge.inVertex));
        rel.edgeIDLPG = edgeID;

        Optional<LPGMappingConfiguration.ELMorPNMEntry<?>> entry = this.configuration.edgeLabelMapping(edge.label);
        if (entry.isPresent()) {
            entry.get().unpack(new LPGMappingConfiguration.MappingEntryDelegate() {
                @Override
                public void handleIRI(IRI i) {
                    rel.predicate = dataset.createOrObtainSimpleNode(i);
                }

                @Override
                public void handleString(String s) {
                    rel.predicate = new OGPredicateString(s);
                }
            });
        } else {
            rel.predicate = new OGPredicateString(edge.label);
        }
        dataset.addStatementAndAddToDefaultGraph(rel);
        return rel;
    }
}
