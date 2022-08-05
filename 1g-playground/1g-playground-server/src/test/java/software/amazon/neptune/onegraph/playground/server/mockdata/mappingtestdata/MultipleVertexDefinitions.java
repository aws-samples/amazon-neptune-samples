package software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;

/**
 * OG
 */
public class MultipleVertexDefinitions extends MappingTestData {

    @Override
    public Model ogMappedToRDF() {
        Model m = new LinkedHashModel();
        m.add(getID_A_IRI(), getTypeIRI(), getPersonIRI(baseIRIString));
        m.add(bNodeB, getTypeIRI(), getPersonIRI(baseIRIString));
        m.add(getID_A_IRI(), getTypeIRI(), getPersonIRI(baseIRIString));
        return m;
    }

    @Override
    public LPGGraph ogMappedToLPG() {
        LPGGraph g = new LPGGraph();
        g.addVertex(new LPGVertex(getPersonIRI(baseIRIString).toString()));
        g.addVertex(new LPGVertex(getPersonIRI(baseIRIString).toString()));
        g.addVertex(new LPGVertex(getPersonIRI(baseIRIString).toString()));
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
        OGSimpleNodeBNode bob = d.createOrObtainSimpleNode(bNodeB);
        OGSimpleNodeIRI charlie = d.createOrObtainSimpleNode(getID_C_IRI());

        OGSimpleNodeIRI person = d.createOrObtainSimpleNode(getPersonIRI(baseIRIString));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, type, person);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(bob, type, person);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(charlie, type, person);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);

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

        OGSimpleNodeIRI person = d.createOrObtainSimpleNode(getPersonIRI(baseIRIString));
        OGSimpleNodeIRI type = d.createOrObtainSimpleNode(getTypeIRI());

        OGRelationshipStatement rel1 = new OGRelationshipStatement(alice, type, person);
        OGRelationshipStatement rel2 = new OGRelationshipStatement(bob, type, person);
        OGRelationshipStatement rel3 = new OGRelationshipStatement(charlie, type, person);

        d.addStatementAndAddToDefaultGraph(rel1);
        d.addStatementAndAddToDefaultGraph(rel2);
        d.addStatementAndAddToDefaultGraph(rel3);

        return d;
    }
}
