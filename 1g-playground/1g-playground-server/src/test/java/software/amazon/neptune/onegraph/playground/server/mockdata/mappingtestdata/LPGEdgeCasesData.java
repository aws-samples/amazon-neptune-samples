package software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicateString;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;

/**
 * Data model containing lpg edge cases, such as:
 * - same labeled multi edge between the same nodes.
 * - Node without label or relations or properties.
 * - Self loop node.
 *
 * Data in this scenario (OG representation): <br />
 * <br />
 * Rel1: B(Vertex) knows(String) C(Vertex) .<br />
 * Rel2: B(Vertex) knows(String) C(Vertex) .<br />
 * Rel3: C(Vertex) knows(String) C(Vertex) .<br />
 *
 * Prp1: A(Vertex) type(IRI) "" .<br />
 * Prp2: Rel2 cashBalance(String) "44"(Integer) .<br />
 * Prp3: Rel3 age(String) "13.14"(Float) .<br />
 * Mem: All statements in default graph.
 */
public class LPGEdgeCasesData extends MappingTestData {
    @Override
    public Model ogMappedToRDF() {
        Model m = new LinkedHashModel();
        m.add(getID_A_IRI(), getTypeIRI(), literalVertex, DEFAULT_GRAPH_IRI);
        m.add(getID_B_IRI(), getTypeIRI(), literalVertex, DEFAULT_GRAPH_IRI);
        m.add(getID_C_IRI(), getTypeIRI(), literalVertex, DEFAULT_GRAPH_IRI);
        return m;
    }

    @Override
    public LPGGraph ogMappedToLPG() {
        LPGGraph g = new LPGGraph();

        LPGVertex alice = new LPGVertex();
        LPGVertex bob = new LPGVertex();
        LPGVertex charlie = new LPGVertex();

        LPGEdge e1 = new LPGEdge(bob, charlie, knowsString);
        LPGEdge e2 = new LPGEdge(bob, charlie, knowsString);
        LPGEdge e3 = new LPGEdge(charlie, charlie, knowsString);

        LPGProperty p1 = new LPGProperty(cashBalanceString, int44);
        LPGProperty p2 = new LPGProperty(ageString, float1314);

        e1.addProperty(p1);
        e2.addProperty(p2);

        g.addVertices(alice, bob, charlie);
        g.addEdges(e1, e2, e3);

        return g;
    }

    @Override
    public LPGMappingConfiguration mappingConfiguration() {
        return new LPGMappingConfiguration();
    }

    @Override
    public OGDataset underlyingOG() {

        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeIRI bob = d.createOrObtainSimpleNode(bobVertex);
        OGSimpleNodeIRI charlie = d.createOrObtainSimpleNode(charlieVertex);

        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGValue<?> vertex = new OGValue<>(vertexString);

        OGValue<?> value44 = new OGValue<>(int44);
        OGValue<?> value1314 = new OGValue<>(float1314);

        OGPredicateString age = new OGPredicateString(ageString);
        OGPredicateString knows = new OGPredicateString(knowsString);
        OGPredicateString cashBalance = new OGPredicateString(cashBalanceString);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(bob, knows, charlie);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(bob, knows, charlie);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(charlie, knows, charlie);

        OGPropertyStatement prp1 = new OGPropertyStatement(rel2, cashBalance, value44);
        OGPropertyStatement prp2 = new OGPropertyStatement(rel3, age, value1314);
        OGPropertyStatement prp3 = new OGPropertyStatement(bob, type, vertex);
        OGPropertyStatement prp4 = new OGPropertyStatement(charlie, type, vertex);
        OGPropertyStatement prp5 = new OGPropertyStatement(alice, type, vertex);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);

        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        d.addStatementAndAddToDefaultGraph(prp3);
        d.addStatementAndAddToDefaultGraph(prp4);
        d.addStatementAndAddToDefaultGraph(prp5);

        return d;
    }

    @Override
    public OGDataset rdfMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGValue<?> vertex = new OGValue<>(literalVertex);

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(getID_A_IRI());
        OGSimpleNodeIRI bob = d.createOrObtainSimpleNode(getID_B_IRI());
        OGSimpleNodeIRI charlie = d.createOrObtainSimpleNode(getID_C_IRI());

        OGPropertyStatement prp1 = new OGPropertyStatement(bob, type, vertex);
        OGPropertyStatement prp2 = new OGPropertyStatement(charlie, type, vertex);
        OGPropertyStatement prp3 = new OGPropertyStatement(alice, type, vertex);

        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        d.addStatementAndAddToDefaultGraph(prp3);

        return d;
    }

    @Override
    public OGDataset lpgMappedToOG() {
        return underlyingOG();
    }
}
