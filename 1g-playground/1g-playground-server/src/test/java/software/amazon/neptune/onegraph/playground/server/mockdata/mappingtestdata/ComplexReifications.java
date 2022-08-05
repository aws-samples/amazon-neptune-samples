package software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;

/**
 * Data model having different reification configurations.
 *
 * Data in this scenario (OG representation): <br />
 * <br />
 * Rel1: A(Vertex) knows(IRI) B(BNode) .<br />
 * Rel2: B(BNode) knows(IRI) C(IRI) .<br />
 * Rel3: rel1 friendsOf(IRI) rel2 .<br />
 * Rel4: prp1 friendsOf(IRI) rel2 .<br />
 * Rel5: rel1 friendsOf(IRI) prp1 .<br />
 * Rel6: prp1 friendsOf(IRI) prp1 .<br />
 *
 * Prp1: A(Vertex) since(IRI) "44"(Integer) .<br />
 * Prp2: rel1 since(IRI) "44"(Integer) .<br />
 * Prp3: rel3 since(IRI) "44"(Integer) .<br />
 * Prp4: Prp1 since(IRI) "44"(Integer) .<br />
 *
 * Mem: All statements in default graph.
 */
public class ComplexReifications extends MappingTestData {

    private final String baseIRIUsedELM = URLConstants.EDGE;
    private final String baseIRIUsedPNM = URLConstants.PROPERTY_NAME;

    @Override
    public Model ogMappedToRDF() {
        Model m = new LinkedHashModel();
        m.add(getID_A_IRI(), getKnowsIRI(baseIRIUsedELM), bNodeB, DEFAULT_GRAPH_IRI);
        m.add(bNodeB, getKnowsIRI(baseIRIUsedELM), getID_C_IRI(), DEFAULT_GRAPH_IRI);
        m.add(getID_A_IRI(), getSinceIRI(baseIRIUsedPNM), literalInt44, DEFAULT_GRAPH_IRI);
        return m;
    }

    @Override
    public LPGGraph ogMappedToLPG() {
        LPGGraph g = new LPGGraph();
        LPGVertex alice = new LPGVertex();
        LPGVertex bob = new LPGVertex();
        LPGVertex charlie = new LPGVertex();

        LPGEdge aKnowsB = new LPGEdge(alice, bob, knowsString);
        LPGEdge bKnowsC = new LPGEdge(bob, charlie, knowsString);

        LPGProperty aSince44_alice = new LPGProperty(sinceString, int44);
        LPGProperty aSince44_edge = new LPGProperty(sinceString, int44);
        
        alice.addProperty(aSince44_alice);
        aKnowsB.addProperty(aSince44_edge);

        g.addVertices(alice, bob, charlie);
        g.addEdges(aKnowsB, bKnowsC);

        return g;
    }

    public OGDataset rdfMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI snA = d.createOrObtainSimpleNode(getID_A_IRI());
        OGSimpleNodeBNode snB = d.createOrObtainSimpleNode(bNodeB);
        OGSimpleNodeIRI snC = d.createOrObtainSimpleNode(getID_C_IRI());

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIUsedELM));
        OGSimpleNodeIRI since = d.createOrObtainSimpleNode(getSinceIRI(baseIRIUsedPNM));

        OGValue<?> value44 = new OGValue<>(int44);

        OGRelationshipStatement aKnowsB = new OGRelationshipStatement(snA, knows, snB);
        OGRelationshipStatement bKnowsC = new OGRelationshipStatement(snB, knows, snC);
        OGPropertyStatement aSince44 = new OGPropertyStatement(snA, since, value44);

        d.addStatementAndAddToDefaultGraph(aKnowsB);
        d.addStatementAndAddToDefaultGraph(bKnowsC);
        d.addStatementAndAddToDefaultGraph(aSince44);

        return d;
    }

    public OGDataset lpgMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI snA = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeIRI snB = d.createOrObtainSimpleNode(bobVertex);
        OGSimpleNodeIRI snC = d.createOrObtainSimpleNode(charlieVertex);

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIUsedELM));
        OGSimpleNodeIRI since = d.createOrObtainSimpleNode(getSinceIRI(baseIRIUsedPNM));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGSimpleNodeIRI vertex = d.createOrObtainSimpleNode(getVertexIRI(URLConstants.DEFAULT_NODE_LABEL));

        OGValue<?> value44 = new OGValue<>(int44);

        OGRelationshipStatement aKnowsB = new OGRelationshipStatement(snA, knows, snB);
        OGRelationshipStatement bKnowsC = new OGRelationshipStatement(snB, knows, snC);
        OGPropertyStatement aSince44 = new OGPropertyStatement(snA, since, value44);
        OGPropertyStatement prp1 = new OGPropertyStatement(aKnowsB, since, value44);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(snA, type, vertex);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(snB, type, vertex);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(snC, type, vertex);

        d.addStatementAndAddToDefaultGraph(aKnowsB);
        d.addStatementAndAddToDefaultGraph(bKnowsC);
        d.addStatementAndAddToDefaultGraph(aSince44);
        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);


        return d;
    }

    @Override
    public LPGMappingConfiguration mappingConfiguration() {
        return LPGMappingConfiguration.defaultConfiguration();
    }

    @Override
    public OGDataset underlyingOG() {

        OGDataset d = new OGDataset();

        OGSimpleNodeIRI snA = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeBNode snB = d.createOrObtainSimpleNode(bNodeB);
        OGSimpleNodeIRI snC = d.createOrObtainSimpleNode(getID_C_IRI());

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIUsedELM));
        OGSimpleNodeIRI friendsOf = d.createOrObtainSimpleNode(getFriendsOfIRI(baseIRIUsedELM));
        OGSimpleNodeIRI since = d.createOrObtainSimpleNode(getSinceIRI(baseIRIUsedPNM));

        OGValue<?> value44 = new OGValue<>(int44);

        OGRelationshipStatement aKnowsB = new OGRelationshipStatement(snA, knows, snB);
        OGRelationshipStatement bKnowsC = new OGRelationshipStatement(snB, knows, snC);

        OGPropertyStatement aSince44 = new OGPropertyStatement(snA, since, value44);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(aKnowsB, friendsOf, bKnowsC);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(aSince44, friendsOf, bKnowsC);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(aKnowsB, friendsOf, aSince44);
        OGRelationshipStatement rel4 = new OGRelationshipStatement(aSince44, friendsOf, aSince44);

        OGPropertyStatement prp1 = new OGPropertyStatement(aKnowsB, since, value44);
        OGPropertyStatement prp2 = new OGPropertyStatement(rel1, since, value44);
        OGPropertyStatement prp3 = new OGPropertyStatement(aSince44, since, value44);

        d.addStatementAndAddToDefaultGraph(aKnowsB);
        d.addStatementAndAddToDefaultGraph(bKnowsC);
        d.addStatementAndAddToDefaultGraph(aSince44);
        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);
        d.addStatementAndAddToDefaultGraph(rel4);

        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        d.addStatementAndAddToDefaultGraph(prp3);

        return d;
    }
}
