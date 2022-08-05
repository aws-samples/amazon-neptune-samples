package software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;
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
 * Simple data model where a default lpg mapping configuration has additional configurations.
 *
 * Data in this scenario (OG representation): <br />
 * <br />
 * Rel1: B(Vertex) custom(IRI) Boss(IRI) .<br />
 * Rel2: A(Vertex) knows(IRI) B(Vertex) .<br />
 * Prp1: A(Vertex) age(IRI) "44"(Literal) .<br />
 * Mem: All statements in default graph.
 */
public class DefaultMappingConfigWithCustomConfig extends MappingTestData {

    protected static final IRI customDNL = SimpleValueFactory.getInstance().createIRI("http://example.com/customDNL/");

    @Override
    public Model ogMappedToRDF() {
        Model m = new LinkedHashModel();
        m.add(getID_B_IRI(), customDNL, getBossIRI(baseIRIString), DEFAULT_GRAPH_IRI);
        m.add(getID_A_IRI(), getKnowsIRI(baseIRIString), getID_B_IRI(), DEFAULT_GRAPH_IRI);
        m.add(getID_A_IRI(), getHeightIRI(baseIRIString), literalInt44, DEFAULT_GRAPH_IRI);
        m.add(getID_A_IRI(), getCashBalanceIRI(baseIRIString), literalInt44, DEFAULT_GRAPH_IRI);
        return m;
    }

    @Override
    public LPGGraph ogMappedToLPG() {
        LPGGraph g = new LPGGraph();

        LPGProperty aliceAge = new LPGProperty(addressString, int44);
        LPGProperty aliceHeight = new LPGProperty(getHeightIRI(baseIRIString).toString(), int44);
        LPGProperty cashBalance = new LPGProperty(cashBalanceString, int44);

        LPGVertex alice = aliceVertex;
        aliceVertex.addProperty(aliceAge);
        aliceVertex.addProperty(aliceHeight);
        aliceVertex.addProperty(cashBalance);

        LPGVertex bob = bobVertex;
        bob.addLabel(personString);

        LPGEdge knows = new LPGEdge(alice, bob, friendsOfString);

        g.addVertices(alice, bob);
        g.addEdge(knows);

        return g;
    }

    @Override
    public LPGMappingConfiguration mappingConfiguration() {
        LPGMappingConfiguration config = LPGMappingConfiguration.defaultConfiguration();
        // Address becomes age.
        config.addToPropertyNameMapping(addressString, ageString);

        // Address becomes age.
        config.addToPropertyNameMapping(addressString, ageString);
        // Map cash balance.
        config.addToPropertyNameMapping(cashBalanceString, getCashBalanceIRI(baseIRIString).toString());

        // Person becomes boss with custom predicate.
        config.addToNodeLabelMapping(personString, customDNL, getBossIRI(baseIRIString).toString());
        // friends of becomes knows
        config.addToEdgeLabelMapping(friendsOfString, getKnowsIRI(baseIRIString).toString());
        return config;
    }

    @Override
    public OGDataset underlyingOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeIRI bob = d.createOrObtainSimpleNode(bobVertex);

        OGSimpleNodeIRI customDNLSN = d.createOrObtainSimpleNode(customDNL);
        OGSimpleNodeIRI bossIRI = d.createOrObtainSimpleNode(getBossIRI(baseIRIString));
        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));
        OGPredicateString age = new OGPredicateString(ageString);
        OGSimpleNodeIRI height = d.createOrObtainSimpleNode(getHeightIRI(baseIRIString));
        OGSimpleNodeIRI cashBalance = d.createOrObtainSimpleNode(getCashBalanceIRI(baseIRIString));

        OGValue<?> value44 = new OGValue<>(int44);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(bob, customDNLSN, bossIRI);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(alice, knows, bob);
        OGPropertyStatement prp1 = new OGPropertyStatement(alice, age, value44);
        OGPropertyStatement prp2 = new OGPropertyStatement(alice, height, value44);
        OGPropertyStatement prp3 = new OGPropertyStatement(alice, cashBalance, value44);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        d.addStatementAndAddToDefaultGraph(prp3);

        return d;
    }

    @Override
    public OGDataset rdfMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(getID_A_IRI());
        OGSimpleNodeIRI bob = d.createOrObtainSimpleNode(getID_B_IRI());

        OGSimpleNodeIRI customDNLSN = d.createOrObtainSimpleNode(customDNL);
        OGSimpleNodeIRI bossIRI = d.createOrObtainSimpleNode(getBossIRI(baseIRIString));
        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));
        OGSimpleNodeIRI height = d.createOrObtainSimpleNode(getHeightIRI(baseIRIString));
        OGSimpleNodeIRI cashBalance = d.createOrObtainSimpleNode(getCashBalanceIRI(baseIRIString));

        OGRelationshipStatement rel1 = new OGRelationshipStatement(bob, customDNLSN, bossIRI);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(alice, knows, bob);

        OGValue<?> value44 = new OGValue<>(literalInt44);

        OGPropertyStatement prp1 = new OGPropertyStatement(alice, height, value44);
        OGPropertyStatement prp2 = new OGPropertyStatement(alice, cashBalance, value44);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        return d;
    }

    @Override
    public OGDataset lpgMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeIRI bob = d.createOrObtainSimpleNode(bobVertex);

        OGSimpleNodeIRI customDNLSN = d.createOrObtainSimpleNode(customDNL);
        OGSimpleNodeIRI bossIRI = d.createOrObtainSimpleNode(getBossIRI(baseIRIString));
        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIString));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGSimpleNodeIRI vertex = d.createOrObtainSimpleNode(getVertexIRI(URLConstants.DEFAULT_NODE_LABEL));
        // Height iri will be http://aws.amazon.com/neptune/ogplayground/data/propertyName/height/http://example.com/height
        // Because the LPG contains the height IRI literally, and there is no mapping
        IRI combinedIri = SimpleValueFactory.getInstance().createIRI(URLConstants.PROPERTY_NAME + getHeightIRI(baseIRIString).toString());
        OGSimpleNodeIRI height = d.createOrObtainSimpleNode(combinedIri);
        OGSimpleNodeIRI cashBalance = d.createOrObtainSimpleNode(getCashBalanceIRI(baseIRIString));

        OGPredicateString age = new OGPredicateString(ageString);

        OGValue<?> value44 = new OGValue<>(int44);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(bob, customDNLSN, bossIRI);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(alice, knows, bob);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(alice, type, vertex);

        OGPropertyStatement prp1 = new OGPropertyStatement(alice, age, value44);
        OGPropertyStatement prp2 = new OGPropertyStatement(alice, height, value44);
        OGPropertyStatement prp3 = new OGPropertyStatement(alice, cashBalance, value44);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);
        d.addStatementAndAddToDefaultGraph(prp1);
        d.addStatementAndAddToDefaultGraph(prp2);
        d.addStatementAndAddToDefaultGraph(prp3);

        return d;
    }
}
