package software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicateString;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;

/**
 * Data model where there are several prop and rel statements in different graphs (but they are all at least in the default graph).
 * There is no mapping configuration hence, all IRIs get translated to LPG as they are.
 *
 * Data in this scenario (OG representation): <br />
 * <br />
 * Rel1: A(BNODE) type(IRI) Person(IRI) .<br />
 * Rel2: B(BNODE) type(IRI) Person(IRI) .<br />
 * Rel3: C(BNODE) type(IRI) Person(IRI) .<br />
 * Rel4: A(BNODE) knows(IRI) B(BNODE) .<br />
 * Rel5: A(BNODE) knows(IRI) C(BNODE) .<br />
 * Prp2: A(BNODE) age(IRI) "44"(Literal) sid4 .<br />
 * Prp3: A(BNODE) name(IRI) "Alice"(Literal) sid4 .<br />
 * Mem1: Rel1 in graph1 <br />
 * Mem2: Rel1 in DEFAULT_GRAPH <br />
 * Mem3: Rel2 in graph2 <br />
 * Mem4: Rel2 in DEFAULT_GRAPH <br />
 * Mem5: Rel3 in graph3 <br />
 * Mem6: Rel3 in DEFAULT_GRAPH <br />
 * Mem7: Rel4 in DEFAULT_GRAPH <br />
 * Mem8: Rel5 in graph1 <br />
 * Mem9: Rel5 in graph2 <br />
 * Mem10: Rel5 in graph3 <br />
 * Mem11: Rel5 in DEFAULT_GRAPH <br />
 * Mem16: Prp1 in graph1 <br />
 * Mem17: Prp1 in DEFAULT_GRAPH <br />
 * Mem18: Prp2 in DEFAULT_GRAPH <br />
 * Mem19: Prp3 in graph2 <br />
 * Mem20: Prp3 in DEFAULT_GRAPH <br />
 */
public class MultipleGraphs extends MappingTestData {

    private final String baseIRIUsed = baseIRIString;

    @Override
    public Model ogMappedToRDF() {
        Model m = new LinkedHashModel();
        m.add(bNodeA, getTypeIRI(), getPersonIRI(baseIRIUsed), graph1);
        m.add(bNodeB, getTypeIRI(), getPersonIRI(baseIRIUsed), graph2);
        m.add(bNodeC, getTypeIRI(), getPersonIRI(baseIRIUsed), graph3);
        m.add(bNodeA, getKnowsIRI(baseIRIUsed), bNodeB, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getKnowsIRI(baseIRIUsed), bNodeC, graph1);
        m.add(bNodeA, getKnowsIRI(baseIRIUsed), bNodeC, graph2);
        m.add(bNodeA, getKnowsIRI(baseIRIUsed), bNodeC, graph3);

        m.add(bNodeA, getAgeIRI(baseIRIUsed), literalInt44, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getNameIRI(baseIRIUsed), literalAliceString, graph2);
        m.add(bNodeA, getNameIRI(baseIRIUsed), literalAliceString, graph3);

        return m;
    }

    @Override
    public LPGGraph ogMappedToLPG() {

        LPGGraph g = new LPGGraph();
        
        LPGVertex alice = aliceVertex;
        alice.addLabel(baseIRIUsed + personString);
        LPGVertex bob = bobVertex;
        bob.addLabel(baseIRIUsed + personString);
        LPGVertex charlie = charlieVertex;
        charlie.addLabel(baseIRIUsed + personString);

        LPGEdge knows1 = new LPGEdge(alice, bob, baseIRIUsed + knowsString);
        LPGEdge knows2 = new LPGEdge(alice, charlie, baseIRIUsed + knowsString);

        LPGProperty aliceName = new LPGProperty(baseIRIUsed + nameString, aliceString);
        LPGProperty aliceAge = new LPGProperty(baseIRIUsed + ageString, int44);

        alice.addProperty(aliceAge);
        alice.addProperty(aliceName);

        g.addVertices(alice, bob, charlie);
        g.addEdges(knows1, knows2);

        return g;
    }

    @Override
    public LPGMappingConfiguration mappingConfiguration() {
        return new LPGMappingConfiguration();
    }

    @Override
    public OGDataset underlyingOG() {

        OGDataset d = new OGDataset();

        OGSimpleNodeBNode alice = d.createOrObtainSimpleNode(bNodeA);
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);
        OGSimpleNodeBNode charlie = d.createOrObtainSimpleNode(bNodeC);

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIUsed));
        OGSimpleNodeIRI person = d.createOrObtainSimpleNode(getPersonIRI(baseIRIUsed));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGSimpleNodeIRI age = d.createOrObtainSimpleNode(getAgeIRI(baseIRIUsed));
        OGSimpleNodeIRI name = d.createOrObtainSimpleNode(getNameIRI(baseIRIUsed));

        OGValue<?> valueAlice = new OGValue<>(literalAliceString);
        OGValue<?> value44 = new OGValue<>(literalInt44);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, type, person);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(bob, type, person);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(charlie, type, person);
        OGRelationshipStatement rel4 = new OGRelationshipStatement(alice, knows, bob);
        OGRelationshipStatement rel5 = new OGRelationshipStatement(alice, knows, charlie);

        OGPropertyStatement prop1 = new OGPropertyStatement(alice, age, value44);
        OGPropertyStatement prop2 = new OGPropertyStatement(alice, name, valueAlice);

        d.addStatementAndAddToGraph(rel1, graph1);
        d.addStatementAndAddToGraph(rel2, graph2);
        d.addStatementAndAddToGraph(rel3, graph3);

        d.addStatementAndAddToGraph(rel4, OGGraph.DEFAULT_GRAPH_IRI);
        d.addStatementAndAddToGraph(rel5, graph1);
        d.addStatementAndAddToGraph(rel5, graph2);
        d.addStatementAndAddToGraph(rel5, graph3);

        d.addStatementAndAddToGraph(prop1, OGGraph.DEFAULT_GRAPH_IRI);
        d.addStatementAndAddToGraph(prop2, graph2);
        d.addStatementAndAddToGraph(prop2, graph3);

        return d;
    }

    @Override
    public OGDataset rdfMappedToOG() {
        return underlyingOG();
    }

    @Override
    public OGDataset lpgMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeIRI bob = d.createOrObtainSimpleNode(bobVertex);
        OGSimpleNodeIRI charlie = d.createOrObtainSimpleNode(charlieVertex);

        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());

        OGPredicateString knows = new OGPredicateString(getKnowsIRI(baseIRIUsed).toString());
        OGPredicateString age = new OGPredicateString(getAgeIRI(baseIRIUsed).toString());
        OGPredicateString name = new OGPredicateString(getNameIRI(baseIRIUsed).toString());

        OGValue<?> valueAlice = new OGValue<>(aliceString);
        OGValue<?> value44 = new OGValue<>(int44);
        OGValue<?> person = new OGValue<>(getPersonIRI(baseIRIUsed).toString());

        OGPropertyStatement prp1 = new OGPropertyStatement(alice, type, person);
        OGPropertyStatement prp2 = new OGPropertyStatement(bob, type, person);
        OGPropertyStatement prp3 = new OGPropertyStatement(charlie, type, person);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, knows, bob);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(alice, knows, charlie);

        OGPropertyStatement prp5 = new OGPropertyStatement(alice, age, value44);
        OGPropertyStatement prp6 = new OGPropertyStatement(alice, name, valueAlice);

        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        d.addStatementAndAddToDefaultGraph(prp3);
        d.addStatementAndAddToDefaultGraph(prp5);
        d.addStatementAndAddToDefaultGraph(prp6);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);

        return d;
    }
}
