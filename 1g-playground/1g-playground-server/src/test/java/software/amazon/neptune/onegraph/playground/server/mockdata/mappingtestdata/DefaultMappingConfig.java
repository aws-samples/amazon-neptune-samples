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
 * Simple data model, with a default lpg mapping configuration.
 *
 * Data in this scenario (OG representation): <br />
 * <br />
 * Rel1: A(BNODE) type(IRI) Person(IRI) .<br />
 * Rel2: B(BNODE) type(IRI) Person(IRI) .<br />
 * Rel3: A(BNODE) knows(IRI) B(BNODE) .<br />
 * Prp1: A(BNODE) age(IRI) "44"(Literal) .<br />
 * Mem: All statements in default graph.
 */
public class DefaultMappingConfig extends MappingTestData {

    private final String baseIRIUsedELM = URLConstants.EDGE;
    private final String baseIRIUsedDNL = URLConstants.DEFAULT_NODE_LABEL;
    private final String baseIRIUsedPNM = URLConstants.PROPERTY_NAME;

    @Override
    public Model ogMappedToRDF() {
        Model m = new LinkedHashModel();
        m.add(bNodeA, getTypeIRI(), getPersonIRI(baseIRIUsedDNL), DEFAULT_GRAPH_IRI);
        m.add(bNodeB, getTypeIRI(), getPersonIRI(baseIRIUsedDNL), DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getKnowsIRI(baseIRIUsedELM), bNodeB, DEFAULT_GRAPH_IRI);
        m.add(bNodeA, getAgeIRI(baseIRIUsedPNM), literalInt44, DEFAULT_GRAPH_IRI);

        return m;
    }

    @Override
    public LPGGraph ogMappedToLPG() {
        LPGGraph g = new LPGGraph();

        LPGVertex alice = new LPGVertex(personString);
        LPGVertex bob = new LPGVertex(personString);
        LPGEdge knows = new LPGEdge(alice, bob, knowsString);

        LPGProperty aliceAge = new LPGProperty(ageString, int44);
        alice.addProperty(aliceAge);

        g.addVertices(alice, bob);
        g.addEdges(knows);

        return g;
    }

    @Override
    public LPGMappingConfiguration mappingConfiguration() {
        return LPGMappingConfiguration.defaultConfiguration();
    }

    @Override
    public OGDataset underlyingOG() {

        OGDataset d = new OGDataset();

        OGSimpleNodeBNode alice = d.createOrObtainSimpleNode(bNodeA);
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIUsedELM));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGSimpleNodeIRI age = d.createOrObtainSimpleNode(getAgeIRI(baseIRIUsedPNM));
        OGSimpleNodeIRI person = d.createOrObtainSimpleNode(getPersonIRI(baseIRIUsedDNL));

        OGValue<?> value44 = new OGValue<>(literalInt44);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, type, person);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(bob, type, person);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(alice, knows, bob);

        OGPropertyStatement prp1 = new OGPropertyStatement(alice, age, value44);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);
        d.addStatementAndAddToDefaultGraph(prp1);

        return d;
    }

    @Override
    public OGDataset rdfMappedToOG() {
        return underlyingOG();
    }

    @Override
    public OGDataset lpgMappedToOG() {
        OGDataset d = new OGDataset();

        OGSimpleNodeBNode alice = d.createOrObtainSimpleNode(bNodeA);
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);

        OGSimpleNodeIRI knows = d.createOrObtainSimpleNode(getKnowsIRI(baseIRIUsedELM));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());
        OGSimpleNodeIRI age = d.createOrObtainSimpleNode(getAgeIRI(baseIRIUsedPNM));
        OGSimpleNodeIRI person = d.createOrObtainSimpleNode(getPersonIRI(baseIRIUsedDNL));

        OGValue<?> value44 = new OGValue<>(int44);

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, type, person);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(bob, type, person);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(alice, knows, bob);

        OGPropertyStatement prp1 = new OGPropertyStatement(alice, age, value44);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);
        d.addStatementAndAddToDefaultGraph(prp1);

        return d;
    }
}
