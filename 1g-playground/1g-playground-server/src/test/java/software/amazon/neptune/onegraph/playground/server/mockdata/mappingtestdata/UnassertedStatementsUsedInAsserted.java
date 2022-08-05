package software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicateString;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;

/**
 * Data model where an asserted statement uses an unasserted statement, twice.
 *
 * Data in this scenario (OG representation): <br />
 * <br />
 * Rel1: A(Vertex) knows(String) B(Vertex) .<br />
 * Prp1: Rel1 since(String) "44"(Integer) .<br />
 * Rel2: Prp2 knows(String) B(Vertex) .<br />
 * Prp2: A(Vertex) since(String) "44"(Integer) .<br />
 * Mem1: Prp1 in DEFAULT_GRAPH <br />
 * Mem2: Rel2 in DEFAULT_GRAPH <br />
 */
public class UnassertedStatementsUsedInAsserted extends MappingTestData {

    @Override
    public Model ogMappedToRDF() {
        return new LinkedHashModel();
    }

    @Override
    public LPGGraph ogMappedToLPG() {
        LPGGraph g = new LPGGraph();
        g.addVertex(new LPGVertex());
        g.addVertex(new LPGVertex());
        return g;
    }

    @Override
    public OGDataset rdfMappedToOG() {
        return new OGDataset();
    }

    @Override
    public OGDataset lpgMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI snA = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeIRI snB = d.createOrObtainSimpleNode(bobVertex);
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());

        OGValue<?> vertexValue = new OGValue<>("vertex");
        OGPropertyStatement prp1 = new OGPropertyStatement(snA, type, vertexValue);
        OGPropertyStatement prp2 = new OGPropertyStatement(snB, type, vertexValue);

        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);

        return d;
    }

    @Override
    public LPGMappingConfiguration mappingConfiguration() {
        return new LPGMappingConfiguration();
    }

    @Override
    public OGDataset underlyingOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI snA = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeIRI snB = d.createOrObtainSimpleNode(bobVertex);

        OGPredicateString knows = new OGPredicateString(knowsString);
        OGPredicateString since = new OGPredicateString(sinceString);

        OGValue<?> value44 = new OGValue<>(int44);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(snA, knows, snB);
        OGPropertyStatement prp2 = new OGPropertyStatement(snA, since, value44);

        OGPropertyStatement prp1 = new OGPropertyStatement(rel1, since, value44);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(prp2, knows, snB);

        d.addStatement(rel1);
        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatement(prp2);

        return d;
    }
}
