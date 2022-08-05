package software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicateString;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;

/**
 * Data model with 1 node having properties of all different supported types, Also multi valued properties in LPG.
 *
 * Data in this scenario (OG representation): <br />
 * <br />
 * Prp1: A(BNode) birthDate(IRI) "1985"(Date) .<br />
 * Prp2: A(BNode) name(IRI) "Alice"(String) .<br />
 * Prp3: A(BNode) age(IRI) "44"(String) .<br />
 * Prp4: A(BNode) address(IRI) "0x80"(Byte) .<br />
 * Prp5: A(BNode) address(IRI) "16"(Short) .<br />
 * Prp6: A(BNode) cashBalance(IRI) "1234"(Long) .<br />
 * Prp7: A(BNode) cashBalance(IRI) "13.14"(Float) .<br />
 * Prp8: A(BNode) height(IRI) "1.72"(Double) .<br />
 *
 * Mem: All statements in default graph.
 */
public class DifferentPropertyValueTypes extends MappingTestData {

    @Override
    public Model ogMappedToRDF() {
        Model m = new LinkedHashModel();

        m.add(bNodeA, getNameIRI(baseIRIString), literalAliceString, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getBirthDateIRI(baseIRIString), literalDate1985, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getAgeIRI(baseIRIString), literalInt44, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getAddressIRI(baseIRIString), literalByte80, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getCashBalanceIRI(baseIRIString), literalLong1234, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getAddressIRI(baseIRIString), literalShort16, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getHeightIRI(baseIRIString), literalDouble172, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getCashBalanceIRI(baseIRIString), literalFloat1314, DEFAULT_GRAPH_IRI);

        return m;
    }

    @Override
    public LPGGraph ogMappedToLPG() {

        LPGGraph g = new LPGGraph();
        LPGVertex alice = new LPGVertex();

        LPGProperty aliceBirthDate = new LPGProperty(getBirthDateIRI(baseIRIString).toString(), date1985);
        LPGProperty aliceName = new LPGProperty(getNameIRI(baseIRIString).toString(), aliceString);
        LPGProperty aliceAge = new LPGProperty(getAgeIRI(baseIRIString).toString(), int44);
        LPGProperty aliceAddress1 = new LPGProperty(getAddressIRI(baseIRIString).toString(), byte80);
        LPGProperty aliceCashBalance1 = new LPGProperty(getCashBalanceIRI(baseIRIString).toString(), long1234);
        LPGProperty aliceHeight = new LPGProperty(getHeightIRI(baseIRIString).toString(), double172);
        LPGProperty aliceAddress2 = new LPGProperty(getAddressIRI(baseIRIString).toString(), short16);
        LPGProperty aliceCashBalance2 = new LPGProperty(getCashBalanceIRI(baseIRIString).toString(), float1314);

        alice.addProperty(aliceBirthDate);
        alice.addProperty(aliceName);
        alice.addProperty(aliceAge);
        alice.addProperty(aliceAddress1);
        alice.addProperty(aliceCashBalance1);
        alice.addProperty(aliceHeight);
        alice.addProperty(aliceAddress2);
        alice.addProperty(aliceCashBalance2);

        g.addVertex(alice);
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

        OGSimpleNodeIRI age = d.createOrObtainSimpleNode(getAgeIRI(baseIRIString));
        OGSimpleNodeIRI name = d.createOrObtainSimpleNode(getNameIRI(baseIRIString));
        OGSimpleNodeIRI height = d.createOrObtainSimpleNode(getHeightIRI(baseIRIString));
        OGSimpleNodeIRI birthDate = d.createOrObtainSimpleNode(getBirthDateIRI(baseIRIString));
        OGSimpleNodeIRI address = d.createOrObtainSimpleNode(getAddressIRI(baseIRIString));
        OGSimpleNodeIRI cashBalance = d.createOrObtainSimpleNode(getCashBalanceIRI(baseIRIString));

        OGValue<?> value1985 = new OGValue<>(date1985);
        OGValue<?> value172 = new OGValue<>(double172);
        OGValue<?> valueAlice = new OGValue<>(aliceString);
        OGValue<?> value44 = new OGValue<>(int44);
        OGValue<?> value80 = new OGValue<>(byte80);
        OGValue<?> value16 = new OGValue<>(short16);
        OGValue<?> value1234 = new OGValue<>(long1234);
        OGValue<?> value1314 = new OGValue<>(float1314);

        OGPropertyStatement prop1 = new OGPropertyStatement(alice, name, valueAlice);
        OGPropertyStatement prop2 = new OGPropertyStatement(alice, birthDate, value1985);
        OGPropertyStatement prop3 = new OGPropertyStatement(alice, age, value44);
        OGPropertyStatement prop4 = new OGPropertyStatement(alice, address, value80);
        OGPropertyStatement prop5 = new OGPropertyStatement(alice, cashBalance, value1234);
        OGPropertyStatement prop6 = new OGPropertyStatement(alice, address, value16);
        OGPropertyStatement prop7 = new OGPropertyStatement(alice, height, value172);
        OGPropertyStatement prop8 = new OGPropertyStatement(alice, cashBalance, value1314);

        d.addStatementAndAddToDefaultGraph(prop1);
        d.addStatementAndAddToDefaultGraph(prop2);
        d.addStatementAndAddToDefaultGraph(prop3);
        d.addStatementAndAddToDefaultGraph(prop4);
        d.addStatementAndAddToDefaultGraph(prop5);
        d.addStatementAndAddToDefaultGraph(prop6);
        d.addStatementAndAddToDefaultGraph(prop7);
        d.addStatementAndAddToDefaultGraph(prop8);

        return d;
    }

    @Override
    public OGDataset rdfMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeBNode alice = d.createOrObtainSimpleNode(bNodeA);

        OGSimpleNodeIRI age = d.createOrObtainSimpleNode(getAgeIRI(baseIRIString));
        OGSimpleNodeIRI name = d.createOrObtainSimpleNode(getNameIRI(baseIRIString));
        OGSimpleNodeIRI height = d.createOrObtainSimpleNode(getHeightIRI(baseIRIString));
        OGSimpleNodeIRI birthDate = d.createOrObtainSimpleNode(getBirthDateIRI(baseIRIString));
        OGSimpleNodeIRI address = d.createOrObtainSimpleNode(getAddressIRI(baseIRIString));
        OGSimpleNodeIRI cashBalance = d.createOrObtainSimpleNode(getCashBalanceIRI(baseIRIString));

        OGValue<?> value1985 = new OGValue<>(literalDate1985);
        OGValue<?> value172 = new OGValue<>(literalDouble172);
        OGValue<?> valueAlice = new OGValue<>(literalAliceString);
        OGValue<?> value44 = new OGValue<>(literalInt44);
        OGValue<?> value80 = new OGValue<>(literalByte80);
        OGValue<?> value16 = new OGValue<>(literalShort16);
        OGValue<?> value1234 = new OGValue<>(literalLong1234);
        OGValue<?> value1314 = new OGValue<>(literalFloat1314);

        OGPropertyStatement prop1 = new OGPropertyStatement(alice, name, valueAlice);
        OGPropertyStatement prop2 = new OGPropertyStatement(alice, birthDate, value1985);
        OGPropertyStatement prop3 = new OGPropertyStatement(alice, age, value44);
        OGPropertyStatement prop4 = new OGPropertyStatement(alice, address, value80);
        OGPropertyStatement prop5 = new OGPropertyStatement(alice, cashBalance, value1234);
        OGPropertyStatement prop6 = new OGPropertyStatement(alice, address, value16);
        OGPropertyStatement prop7 = new OGPropertyStatement(alice, height, value172);
        OGPropertyStatement prop8 = new OGPropertyStatement(alice, cashBalance, value1314);

        d.addStatementAndAddToDefaultGraph(prop1);
        d.addStatementAndAddToDefaultGraph(prop2);
        d.addStatementAndAddToDefaultGraph(prop3);
        d.addStatementAndAddToDefaultGraph(prop4);
        d.addStatementAndAddToDefaultGraph(prop5);
        d.addStatementAndAddToDefaultGraph(prop6);
        d.addStatementAndAddToDefaultGraph(prop7);
        d.addStatementAndAddToDefaultGraph(prop8);

        return d;
    }

    @Override
    public OGDataset lpgMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGValue<?> vertex = new OGValue<>(vertexString);

        OGPredicateString age = new OGPredicateString(getAgeIRI(baseIRIString).toString());
        OGPredicateString name = new OGPredicateString(getNameIRI(baseIRIString).toString());
        OGPredicateString height = new OGPredicateString(getHeightIRI(baseIRIString).toString());
        OGPredicateString birthDate = new OGPredicateString(getBirthDateIRI(baseIRIString).toString());
        OGPredicateString address = new OGPredicateString(getAddressIRI(baseIRIString).toString());
        OGPredicateString cashBalance = new OGPredicateString(getCashBalanceIRI(baseIRIString).toString());

        OGValue<?> value1985 = new OGValue<>(date1985);
        OGValue<?> value172 = new OGValue<>(double172);
        OGValue<?> valueAlice = new OGValue<>(aliceString);
        OGValue<?> value44 = new OGValue<>(int44);
        OGValue<?> value80 = new OGValue<>(byte80);
        OGValue<?> value16 = new OGValue<>(short16);
        OGValue<?> value1234 = new OGValue<>(long1234);
        OGValue<?> value1314 = new OGValue<>(float1314);

        OGPropertyStatement prp1 = new OGPropertyStatement(alice, name, valueAlice);
        OGPropertyStatement prp2 = new OGPropertyStatement(alice, birthDate, value1985);
        OGPropertyStatement prp3 = new OGPropertyStatement(alice, age, value44);
        OGPropertyStatement prp4 = new OGPropertyStatement(alice, address, value80);
        OGPropertyStatement prp5 = new OGPropertyStatement(alice, cashBalance, value1234);
        OGPropertyStatement prp6 = new OGPropertyStatement(alice, address, value16);
        OGPropertyStatement prp7 = new OGPropertyStatement(alice, height, value172);
        OGPropertyStatement prp8 = new OGPropertyStatement(alice, cashBalance, value1314);
        OGPropertyStatement prp9 = new OGPropertyStatement(alice, type, vertex);

        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        d.addStatementAndAddToDefaultGraph(prp3);
        d.addStatementAndAddToDefaultGraph(prp4);
        d.addStatementAndAddToDefaultGraph(prp5);
        d.addStatementAndAddToDefaultGraph(prp6);
        d.addStatementAndAddToDefaultGraph(prp7);
        d.addStatementAndAddToDefaultGraph(prp8);
        d.addStatementAndAddToDefaultGraph(prp9);

        return d;
    }
}
