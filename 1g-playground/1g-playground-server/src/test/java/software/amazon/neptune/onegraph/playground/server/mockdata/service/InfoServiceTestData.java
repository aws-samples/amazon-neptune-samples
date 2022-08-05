package software.amazon.neptune.onegraph.playground.server.mockdata.service;

import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;
import software.amazon.neptune.onegraph.playground.server.tests.unit.service.InfoServiceTest;
import software.amazon.neptune.onegraph.playground.server.mockdata.TestData;

/**
 * Test data for the {@link InfoServiceTest}
 */
public class InfoServiceTestData extends TestData {

    /**
     * Add statements to the given dataset that are both RDF and LPG non-compliant
     * @param d Dataset to add to.
     */
    public static void addTestDataToDatasetFullyNonCompliant(OGDataset d) {

        OGSimpleNodeBNode snA = d.createOrObtainSimpleNode(bNodeA);
        OGSimpleNodeBNode snB = d.createOrObtainSimpleNode(bNodeB);
        OGSimpleNodeIRI snC = d.createOrObtainSimpleNode(getID_C_IRI());

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));
        OGSimpleNodeIRI friendsOf = d.createOrObtainSimpleNode(getFriendsOfIRI(baseIRIString));
        OGSimpleNodeIRI since = d.createOrObtainSimpleNode(getSinceIRI(baseIRIString));

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

        d.addStatement(aKnowsB);
        d.addStatement(bKnowsC);
        d.addStatement(aSince44);
        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);
        d.addStatementAndAddToDefaultGraph(rel4);

        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        d.addStatementAndAddToDefaultGraph(prp3);
    }

    /**
     * Add full compliant statements to the given dataset.
     * @param d Dataset to add to.
     */
    public static void addTestDataToDatasetFullyCompatible(OGDataset d) {
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

    /**
     * Add pairwise non-compliant statements to the given dataset.
     * @param d Dataset to add to.
     */
    public static void addTestDataToDatasetPairwiseNonCompatible(OGDataset d) {
        OGSimpleNodeBNode alice = d.createOrObtainSimpleNode(bNodeA);
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, knows, bob);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(alice, knows, bob);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(alice, knows, bob);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);
    }

    /**
     * Add non-lpg compliant statements to the given dataset.
     * @param d Dataset to add to.
     */
    public static void addTestDataToDatasetLPGNonCompatible(OGDataset d) {
        OGSimpleNodeBNode snA = d.createOrObtainSimpleNode(bNodeA);
        OGSimpleNodeBNode snB = d.createOrObtainSimpleNode(bNodeB);
        OGSimpleNodeIRI snC = d.createOrObtainSimpleNode(getID_C_IRI());

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));

        OGRelationshipStatement aKnowsB = new OGRelationshipStatement(snA, knows, snB);
        OGRelationshipStatement bKnowsC = new OGRelationshipStatement(aKnowsB, knows, snC);


        OGSimpleNodeIRI cashBalance = d.createOrObtainSimpleNode(getCashBalanceIRI(baseIRIString));
        OGValue<?> value1985 = new OGValue<>(literalDate1985);

        OGPropertyStatement since = new OGPropertyStatement(aKnowsB, cashBalance, value1985);
        d.addStatement(aKnowsB);
        d.addStatementAndAddToDefaultGraph(bKnowsC);
        d.addStatementAndAddToDefaultGraph(since);
    }

    /**
     * Add non-rdf compliant statements to the given dataset.
     * @param d Dataset to add to.
     */
    public static void addTestDataToDatasetRDFNonCompatible(OGDataset d) {
        OGSimpleNodeBNode snA = d.createOrObtainSimpleNode(bNodeA);
        OGSimpleNodeBNode snB = d.createOrObtainSimpleNode(bNodeB);
        OGSimpleNodeIRI snC = d.createOrObtainSimpleNode(getID_C_IRI());

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));

        OGRelationshipStatement aKnowsB = new OGRelationshipStatement(snA, knows, snB);
        OGRelationshipStatement bKnowsC = new OGRelationshipStatement(aKnowsB, knows, snC);


        OGSimpleNodeIRI cashBalance = d.createOrObtainSimpleNode(getCashBalanceIRI(baseIRIString));
        OGValue<?> value1985 = new OGValue<>(literalDate1985);

        OGPropertyStatement since = new OGPropertyStatement(aKnowsB, cashBalance, value1985);
        d.addStatementAndAddToDefaultGraph(aKnowsB);
        d.addStatementAndAddToDefaultGraph(bKnowsC);
        d.addStatementAndAddToDefaultGraph(since);
    }
}
