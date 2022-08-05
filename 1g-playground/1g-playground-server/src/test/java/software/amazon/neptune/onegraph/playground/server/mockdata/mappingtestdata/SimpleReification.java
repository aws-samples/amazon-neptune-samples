package software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
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
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;

/**
 * "Alice knows Bob" data model where alice and bob have properties in all supported data types.
 * The "knows" relation is reified. There is a LPG mapping configuration that removes the namespace from all IRIs
 *
 * Data in this scenario (OG representation): <br />
 * <br />
 * Rel1: A(BNODE) type(IRI) Person(IRI) .<br />
 * Rel2: A(BNODE) type(IRI) Boss(IRI) .<br />
 * Rel3: B(BNODE) type(IRI) Person(IRI) .<br />
 * Rel4: A(BNODE) knows(IRI) B(BNODE).<br />
 * Rel3: B(BNODE) knows(IRI) C(IRI) .<br />
 * Rel4: C(IRI) knows(IRI) B(BNODE).<br />
 * Prp1: rel4 since(IRI) "1985-06-06"(Literal) .<br />
 * Prp2: A(BNODE) name(IRI) "Alice"(Literal) .<br />
 * Prp3: A(BNODE) birthDate(IRI) "1985-06-06"(Literal) .<br />
 * Prp4: A(BNODE) age(IRI) "44"(Literal) .<br />
 * Prp5: A(BNODE) address(IRI) "0x80"(Literal) .<br />
 * Prp6: A(BNODE) cashBalance(IRI) "1234"(Literal) .<br />
 * Prp7: B(BNODE) name(IRI) "Bob"(Literal) sid10 .<br />
 * Prp8: B(BNODE) address(IRI) "16"(Literal) .<br />
 * Prp9: B(BNODE) height(IRI) "1.72"(Literal) .<br />
 * Prp10: B(BNODE) cashBalance(IRI) "13.14"(Literal) .<br />
 * Mem: All statements in default graph.
 */
public class SimpleReification extends MappingTestData {

    OGTripleStatement reifiedStatement;

    @Override
    public Model ogMappedToRDF() {

        Model m = new LinkedHashModel();
        m.add(bNodeA, getTypeIRI(), getPersonIRI(baseIRIString), DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getTypeIRI(), getBossIRI(baseIRIString), DEFAULT_GRAPH_IRI);
        m.add(bNodeB, getTypeIRI(), getPersonIRI(baseIRIString), DEFAULT_GRAPH_IRI);
        m.add(getID_C_IRI(), getTypeIRI(), getPersonIRI(baseIRIString), DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getKnowsIRI(baseIRIString), bNodeB, DEFAULT_GRAPH_IRI);
        m.add(getID_C_IRI(), getKnowsIRI(baseIRIString), bNodeB, DEFAULT_GRAPH_IRI);
        m.add(bNodeB, getKnowsIRI(baseIRIString), getID_C_IRI(), DEFAULT_GRAPH_IRI);

        m.add(bNodeA, getNameIRI(baseIRIString), literalAliceString, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getBirthDateIRI(baseIRIString), literalDate1985, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getAgeIRI(baseIRIString), literalInt44, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getAddressIRI(baseIRIString), literalByte80, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getCashBalanceIRI(baseIRIString), literalLong1234, DEFAULT_GRAPH_IRI);

        m.add(bNodeB, getNameIRI(baseIRIString), literalBobString, DEFAULT_GRAPH_IRI);
        m.add(bNodeB, getAddressIRI(baseIRIString), literalShort16, DEFAULT_GRAPH_IRI);
        m.add(bNodeB, getHeightIRI(baseIRIString), literalDouble172, DEFAULT_GRAPH_IRI);
        m.add(bNodeB, getCashBalanceIRI(baseIRIString), literalFloat1314, DEFAULT_GRAPH_IRI);
        return m;
    }

    @Override
    public LPGGraph ogMappedToLPG() {

        LPGGraph g = new LPGGraph();

        LPGVertex alice = new LPGVertex(personString, bossString);
        LPGVertex bob = new LPGVertex(personString);
        LPGVertex charlie = new LPGVertex(personString);

        LPGEdge knows = new LPGEdge(alice, bob, knowsString);
        LPGEdge knows2 = new LPGEdge(bob, charlie, knowsString);
        LPGEdge knows3 = new LPGEdge(charlie, bob, knowsString);

        LPGProperty since = new LPGProperty(sinceString, date1985);
        LPGProperty aliceBirthDate = new LPGProperty(birthDateString, date1985);
        LPGProperty aliceName = new LPGProperty(nameString, aliceString);
        LPGProperty aliceAge = new LPGProperty(ageString, int44);
        LPGProperty aliceAddress = new LPGProperty(addressString, byte80);
        LPGProperty aliceCashBalance = new LPGProperty(cashBalanceString, long1234);

        LPGProperty bobName = new LPGProperty(nameString, bobString);
        LPGProperty bobHeight = new LPGProperty(heightString, double172);
        LPGProperty bobAddress = new LPGProperty(addressString, short16);
        LPGProperty bobCashBalance = new LPGProperty(cashBalanceString, float1314);

        alice.addProperty(aliceName);
        alice.addProperty(aliceBirthDate);
        alice.addProperty(aliceAge);
        alice.addProperty(aliceAddress);
        alice.addProperty(aliceCashBalance);

        bob.addProperty(bobName);
        bob.addProperty(bobHeight);
        bob.addProperty(bobAddress);
        bob.addProperty(bobCashBalance);

        knows.addProperty(since);

        g.addVertices(alice, bob, charlie);
        g.addEdges(knows, knows2, knows3);

        return g;
    }

    @Override
    public OGDataset underlyingOG() {

        OGDataset d = this.rdfMappedToOG();

        OGSimpleNodeIRI since = d.createOrObtainSimpleNode(getSinceIRI(baseIRIString));
        OGValue<?> value1985 = new OGValue<>(literalDate1985);
        OGPropertyStatement prop1 = new OGPropertyStatement(this.reifiedStatement, since, value1985);
        d.addStatementAndAddToDefaultGraph(prop1);

        return d;
    }

    @Override
    public OGDataset rdfMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeBNode alice = d.createOrObtainSimpleNode(bNodeA);
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);
        OGSimpleNodeIRI charlie = d.createOrObtainSimpleNode(getID_C_IRI());

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));
        OGSimpleNodeIRI person = d.createOrObtainSimpleNode(getPersonIRI(baseIRIString));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGSimpleNodeIRI age = d.createOrObtainSimpleNode(getAgeIRI(baseIRIString));
        OGSimpleNodeIRI name = d.createOrObtainSimpleNode(getNameIRI(baseIRIString));
        OGSimpleNodeIRI height = d.createOrObtainSimpleNode(getHeightIRI(baseIRIString));
        OGSimpleNodeIRI birthDate = d.createOrObtainSimpleNode(getBirthDateIRI(baseIRIString));
        OGSimpleNodeIRI boss = d.createOrObtainSimpleNode(getBossIRI(baseIRIString));
        OGSimpleNodeIRI address = d.createOrObtainSimpleNode(getAddressIRI(baseIRIString));
        OGSimpleNodeIRI cashBalance = d.createOrObtainSimpleNode(getCashBalanceIRI(baseIRIString));

        OGValue<?> value1985 = new OGValue<>(literalDate1985);
        OGValue<?> value172 = new OGValue<>(literalDouble172);
        OGValue<?> valueAlice = new OGValue<>(literalAliceString);
        OGValue<?> valueBob = new OGValue<>(literalBobString);
        OGValue<?> value44 = new OGValue<>(literalInt44);
        OGValue<?> value80 = new OGValue<>(literalByte80);
        OGValue<?> value16 = new OGValue<>(literalShort16);
        OGValue<?> value1234 = new OGValue<>(literalLong1234);
        OGValue<?> value1314 = new OGValue<>(literalFloat1314);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, type, person);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(alice, type, boss);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(bob, type, person);
        OGRelationshipStatement rel4 = new OGRelationshipStatement(alice, knows, bob);
        OGRelationshipStatement rel5 = new OGRelationshipStatement(charlie, knows, bob);
        OGRelationshipStatement rel6 = new OGRelationshipStatement(bob, knows, charlie);
        OGRelationshipStatement rel7 = new OGRelationshipStatement(charlie, type, person);

        this.reifiedStatement = rel4;

        OGPropertyStatement prop2 = new OGPropertyStatement(alice, name, valueAlice);
        OGPropertyStatement prop3 = new OGPropertyStatement(alice, birthDate, value1985);
        OGPropertyStatement prop4 = new OGPropertyStatement(alice, age, value44);
        OGPropertyStatement prop5 = new OGPropertyStatement(alice, address, value80);
        OGPropertyStatement prop6 = new OGPropertyStatement(alice, cashBalance, value1234);

        OGPropertyStatement prop7 = new OGPropertyStatement(bob, name, valueBob);
        OGPropertyStatement prop8 = new OGPropertyStatement(bob, address, value16);
        OGPropertyStatement prop9 = new OGPropertyStatement(bob, height, value172);
        OGPropertyStatement prop10 = new OGPropertyStatement(bob, cashBalance, value1314);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);
        d.addStatementAndAddToDefaultGraph(rel4);
        d.addStatementAndAddToDefaultGraph(rel5);
        d.addStatementAndAddToDefaultGraph(rel6);
        d.addStatementAndAddToDefaultGraph(rel7);

        d.addStatementAndAddToDefaultGraph(prop2);
        d.addStatementAndAddToDefaultGraph(prop3);
        d.addStatementAndAddToDefaultGraph(prop4);
        d.addStatementAndAddToDefaultGraph(prop5);
        d.addStatementAndAddToDefaultGraph(prop6);
        d.addStatementAndAddToDefaultGraph(prop7);
        d.addStatementAndAddToDefaultGraph(prop8);
        d.addStatementAndAddToDefaultGraph(prop9);
        d.addStatementAndAddToDefaultGraph(prop10);

        return d;
    }

    @Override
    public OGDataset lpgMappedToOG() {
        return underlyingOG();
    }

    @Override
    public LPGMappingConfiguration mappingConfiguration() {
        LPGMappingConfiguration config = new LPGMappingConfiguration();
        config.defaultNodeLabelPredicate = getTypeIRI();

        config.addToEdgeLabelMapping(knowsString, getKnowsIRI(baseIRIString).toString());
        config.addToPropertyNameMapping(ageString, getAgeIRI(baseIRIString).toString());
        config.addToPropertyNameMapping(sinceString, getSinceIRI(baseIRIString).toString());
        config.addToPropertyNameMapping(birthDateString, getBirthDateIRI(baseIRIString).toString());
        config.addToPropertyNameMapping(nameString, getNameIRI(baseIRIString).toString());
        config.addToPropertyNameMapping(addressString, getAddressIRI(baseIRIString).toString());
        config.addToPropertyNameMapping(cashBalanceString, getCashBalanceIRI(baseIRIString).toString());
        config.addToPropertyNameMapping(heightString, getHeightIRI(baseIRIString).toString());

        config.addToNodeLabelMapping(personString, config.defaultNodeLabelPredicate, getPersonIRI(baseIRIString).toString());
        config.addToNodeLabelMapping(bossString, config.defaultNodeLabelPredicate, getBossIRI(baseIRIString).toString());

        return config;
    }
}
