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
 * Simple data model, with a customized lpg mapping configuration.
 * The data model contains lpg centric data, not every statement can be mapped to rdf.
 *
 * Data in this scenario (OG representation): <br />
 * <br />
 * Rel1: A(Vertex) knows(String) B(Vertex) .<br />
 * Prp1: A(Vertex) type(IRI) Person(String) .<br />
 * Prp2: B(Vertex) type(IRI) Person(String) .<br />
 * Prp3: C(Vertex) type(IRI) Person(String) .<br />
 * Prp4: A(Vertex) age(String) "44"(Integer) .<br />
 * Mem: All statements in default graph.
 */
public class CustomMappingConfig extends MappingTestData {
    @Override
    public Model ogMappedToRDF() {
        Model m = new LinkedHashModel();
        m.add(getID_A_IRI(), getTypeIRI(), literalFriendsOfString, DEFAULT_GRAPH_IRI);
        m.add(getID_B_IRI(), getTypeIRI(), literalPersonString, DEFAULT_GRAPH_IRI);
        m.add(getID_C_IRI(), getTypeIRI(), literalPersonString, DEFAULT_GRAPH_IRI);

        return m;
    }

    @Override
    public LPGGraph ogMappedToLPG() {

        LPGGraph g = new LPGGraph();

        LPGVertex alice = aliceVertex;
        alice.addLabel(bossString);
        LPGVertex bob = bobVertex;
        bob.addLabel(personString);
        LPGVertex charlie = charlieVertex;
        charlie.addLabel(personString);
        LPGEdge knows = new LPGEdge(alice, bob, friendsOfString);

        LPGProperty aliceAge = new LPGProperty(addressString, int44);
        alice.addProperty(aliceAge);
        g.addVertices(alice, bob, charlie);
        g.addEdges(knows);

        return g;
    }

    @Override
    public LPGMappingConfiguration mappingConfiguration() {
        LPGMappingConfiguration config = new LPGMappingConfiguration();
        // Bosses become friends of
        config.addToNodeLabelMapping(bossString, getTypeIRI(), friendsOfString);
        // Friends of becomes knows
        config.addToEdgeLabelMapping(friendsOfString, knowsString);
        // Address becomes age.
        config.addToPropertyNameMapping(addressString, ageString);
        return config;
    }

    @Override
    public OGDataset underlyingOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(aliceVertex);
        OGSimpleNodeIRI bob = d.createOrObtainSimpleNode(bobVertex);
        OGSimpleNodeIRI charlie = d.createOrObtainSimpleNode(charlieVertex);

        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());

        OGPredicateString knows = new OGPredicateString(knowsString);

        OGValue<?> person = new OGValue<>(personString);
        OGValue<?> friendsOf = new OGValue<>(friendsOfString);
        OGValue<?> ageValue = new OGValue<>(int44);
        OGPredicateString age = new OGPredicateString(ageString);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, knows, bob);
        OGPropertyStatement prp1 = new OGPropertyStatement(alice, type, friendsOf);
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

    @Override
    public OGDataset rdfMappedToOG() {

        OGDataset d = new OGDataset();
        OGSimpleNodeIRI alice = d.createOrObtainSimpleNode(getID_A_IRI());
        OGSimpleNodeIRI bob = d.createOrObtainSimpleNode(getID_B_IRI());
        OGSimpleNodeIRI charlie = d.createOrObtainSimpleNode(getID_C_IRI());

        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());

        OGValue<?> person = new OGValue<>(literalPersonString);
        OGValue<?> friendsOf = new OGValue<>(literalFriendsOfString);

        OGPropertyStatement prp1 = new OGPropertyStatement(alice, type, friendsOf);
        OGPropertyStatement prp2 = new OGPropertyStatement(bob, type, person);
        OGPropertyStatement prp3 = new OGPropertyStatement(charlie, type, person);

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
