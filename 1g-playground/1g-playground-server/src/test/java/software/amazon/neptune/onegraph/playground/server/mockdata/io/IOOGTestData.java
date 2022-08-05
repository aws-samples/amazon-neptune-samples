package software.amazon.neptune.onegraph.playground.server.mockdata.io;

import software.amazon.neptune.onegraph.playground.server.io.serializing.OGSerializer;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGPredicateString;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;
import software.amazon.neptune.onegraph.playground.server.mockdata.TestData;

/**
 * Contains test data to test edge cases for the {@link OGSerializer}
 */
public class IOOGTestData extends TestData {

    public static OGDataset dataWithManyNestings() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(getPersonIRI(baseIRIString));
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));
        OGSimpleNodeIRI person = d.createOrObtainSimpleNode(getPersonIRI(baseIRIString));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGSimpleNodeIRI boss = d.createOrObtainSimpleNode(getBossIRI(baseIRIString));

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, type, person);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(alice, type, boss);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(bob, type, rel1);
        OGRelationshipStatement rel4 = new OGRelationshipStatement(alice, knows, bob);
        OGRelationshipStatement rel5 = new OGRelationshipStatement(rel4, knows, rel3);
        OGRelationshipStatement rel6 = new OGRelationshipStatement(rel5, knows, rel3);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);
        d.addStatementAndAddToDefaultGraph(rel4);
        d.addStatementAndAddToDefaultGraph(rel5);
        d.addStatementAndAddToDefaultGraph(rel6);

        return d;
    }

    public static OGDataset dataWithUnassertedElements() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(getPersonIRI(baseIRIString));
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));
        OGSimpleNodeIRI person = d.createOrObtainSimpleNode(getPersonIRI(baseIRIString));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGSimpleNodeIRI boss = d.createOrObtainSimpleNode(getBossIRI(baseIRIString));

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, type, person);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(alice, type, boss);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(bob, type, rel1);
        OGRelationshipStatement rel4 = new OGRelationshipStatement(alice, knows, bob);
        OGRelationshipStatement rel5 = new OGRelationshipStatement(rel4, knows, rel3);
        OGRelationshipStatement rel6 = new OGRelationshipStatement(rel5, knows, rel3);

        d.addStatement(rel1);
        d.addStatement(rel2);
        d.addStatement(rel3);
        d.addStatement(rel4);
        d.addStatementAndAddToDefaultGraph(rel5);
        d.addStatement(rel6);

        return d;
    }

    public static OGDataset dataInMultipleGraphs() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(getPersonIRI(baseIRIString));
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));
        OGSimpleNodeIRI person = d.createOrObtainSimpleNode(getPersonIRI(baseIRIString));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGSimpleNodeIRI boss = d.createOrObtainSimpleNode(getBossIRI(baseIRIString));

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, type, person);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(alice, type, boss);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(bob, type, rel1);
        OGRelationshipStatement rel4 = new OGRelationshipStatement(alice, knows, bob);
        OGRelationshipStatement rel5 = new OGRelationshipStatement(rel4, knows, rel3);
        OGRelationshipStatement rel6 = new OGRelationshipStatement(rel5, knows, rel3);

        d.addStatementAndAddToGraph(rel1, graph1);
        d.addStatementAndAddToGraph(rel1, OGGraph.DEFAULT_GRAPH_IRI);
        d.addStatementAndAddToGraph(rel2, graph2);
        d.addStatementAndAddToGraph(rel2, OGGraph.DEFAULT_GRAPH_IRI);
        d.addStatementAndAddToGraph(rel3, graph3);
        d.addStatementAndAddToGraph(rel3, OGGraph.DEFAULT_GRAPH_IRI);
        d.addStatementAndAddToGraph(rel3, graph1);
        d.addStatementAndAddToGraph(rel3, graph2);
        d.addStatementAndAddToDefaultGraph(rel6);
        d.addStatementAndAddToDefaultGraph(rel5);
        d.addStatementAndAddToDefaultGraph(rel4);

        return d;
    }

    public static OGDataset dataWithComplexLiterals() {
        OGDataset d = new OGDataset();

        OGSimpleNodeBNode alice = d.createOrObtainSimpleNode(bNodeA);

        OGSimpleNodeIRI age = d.createOrObtainSimpleNode(getAgeIRI(baseIRIString));
        OGSimpleNodeIRI name = d.createOrObtainSimpleNode(getNameIRI(baseIRIString));
        OGSimpleNodeIRI height = d.createOrObtainSimpleNode(getHeightIRI(baseIRIString));
        OGSimpleNodeIRI birthDate = d.createOrObtainSimpleNode(getBirthDateIRI(baseIRIString));
        OGSimpleNodeIRI address = d.createOrObtainSimpleNode(getAddressIRI(baseIRIString));
        OGSimpleNodeIRI cashBalance = d.createOrObtainSimpleNode(getCashBalanceIRI(baseIRIString));

        OGValue<?> value1985 = new OGValue<>(literalByte80);
        OGValue<?> value172 = new OGValue<>(literalDate1985);
        OGValue<?> valueAlice = new OGValue<>(aliceString);
        OGValue<?> value44 = new OGValue<>(int44);
        OGValue<?> value80 = new OGValue<>(byte80);
        OGValue<?> value16 = new OGValue<>(short16);
        OGValue<?> value1234 = new OGValue<>(long1234);

        OGValue<?> escapeChars = new OGValue<>("\n\n\n\n \r \r apoj %d lorem" +
                " \\u00ae ipsum \\u00a2 dolor \\u00bf sit \\u00be amet \\u00a9 + \" \" ");

        OGPropertyStatement prop1 = new OGPropertyStatement(alice, name, valueAlice);
        OGPropertyStatement prop2 = new OGPropertyStatement(alice, birthDate, value1985);
        OGPropertyStatement prop3 = new OGPropertyStatement(alice, age, value44);
        OGPropertyStatement prop4 = new OGPropertyStatement(alice, address, value80);
        OGPropertyStatement prop5 = new OGPropertyStatement(alice, cashBalance, value1234);
        OGPropertyStatement prop6 = new OGPropertyStatement(alice, address, value16);
        OGPropertyStatement prop7 = new OGPropertyStatement(alice, height, value172);
        OGPropertyStatement prop8 = new OGPropertyStatement(alice, cashBalance, escapeChars);

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

    public static OGDataset lpgData() {
        OGDataset d = new OGDataset();

        LPGVertex p1 = new LPGVertex("Person");
        p1.setId("1");
        LPGVertex p2 = new LPGVertex("Person");
        p2.setId("theID");
        LPGVertex p3 = new LPGVertex("Person");
        p3.setId(3);
        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(p1);
        OGSimpleNodeIRI bob = d.createOrObtainSimpleNode(p2);
        OGSimpleNodeIRI charlie = d.createOrObtainSimpleNode(p3);

        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());

        OGPredicateString knows = new OGPredicateString(knowsString);

        OGValue<?> person = new OGValue<>(personString);
        OGValue<?> ageValue = new OGValue<>(int44);
        OGValue<?> dateValue = new OGValue<>(date1985);
        OGPredicateString age = new OGPredicateString(ageString);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, knows, bob);
        OGPropertyStatement prp1 = new OGPropertyStatement(alice, age, dateValue);
        OGPropertyStatement prp2 = new OGPropertyStatement(bob, type, person);
        OGPropertyStatement prp3 = new OGPropertyStatement(charlie, type, person);
        OGPropertyStatement prp4 = new OGPropertyStatement(alice, age, ageValue);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        d.addStatementAndAddToDefaultGraph(prp3);
        d.addStatementAndAddToDefaultGraph(prp4);
        
        return d;
    }

}
