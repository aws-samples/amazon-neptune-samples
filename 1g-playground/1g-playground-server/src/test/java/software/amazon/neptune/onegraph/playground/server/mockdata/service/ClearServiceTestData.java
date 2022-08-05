package software.amazon.neptune.onegraph.playground.server.mockdata.service;

import software.amazon.neptune.onegraph.playground.server.mockdata.TestData;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;
import software.amazon.neptune.onegraph.playground.server.tests.unit.service.ClearServiceTest;

/**
 * Test data for the {@link ClearServiceTest}
 */
public class ClearServiceTestData extends TestData {

    public static void addTestDataToDataset(OGDataset d) {

        OGSimpleNodeBNode alice = d.createOrObtainSimpleNode(bNodeA);
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);

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

        d.addStatementAndAddToDefaultGraph(prop2);
        d.addStatementAndAddToDefaultGraph(prop3);
        d.addStatementAndAddToDefaultGraph(prop4);
        d.addStatementAndAddToDefaultGraph(prop5);
        d.addStatementAndAddToDefaultGraph(prop6);
        d.addStatementAndAddToDefaultGraph(prop7);
        d.addStatementAndAddToDefaultGraph(prop8);
        d.addStatementAndAddToDefaultGraph(prop9);
        d.addStatementAndAddToDefaultGraph(prop10);
    }
}
