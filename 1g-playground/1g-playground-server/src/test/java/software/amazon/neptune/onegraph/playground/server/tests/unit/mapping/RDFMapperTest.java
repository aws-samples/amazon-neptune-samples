package software.amazon.neptune.onegraph.playground.server.tests.unit.mapping;

import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.mapping.RDFMapper;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.MappingTestData;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.helper.Equivalence;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.SimpleReification;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.MultipleGraphs;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.DefaultMappingConfig;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.CustomMappingConfig;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.LPGEdgeCasesData;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.DefaultMappingConfigWithCustomConfig;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.DifferentPropertyValueTypes;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.ComplexReifications;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.UnassertedStatementsUsedInAsserted;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RDFMapperTest {

    // Testing OG to RDF

    @Test
    public void simpleReificationOGToRDF() {
        SimpleReification edgeCase = new SimpleReification();

        OGDataset d = edgeCase.underlyingOG();
        Model expected = edgeCase.ogMappedToRDF();

        RDFMapper mapper = new RDFMapper(d);
        Model result = mapper.createRDFModelFromOGDataset();

        assertEquals(expected, result);
    }

    @Test
    public void multipleGraphsOGToRDF() {
        MappingTestData edgeCase = new MultipleGraphs();

        OGDataset d = edgeCase.underlyingOG();
        Model expected = edgeCase.ogMappedToRDF();

        RDFMapper mapper = new RDFMapper(d);
        Model result = mapper.createRDFModelFromOGDataset();

        assertEquals(expected, result);
    }
    @Test
    public void defaultMappingConfigOGToRDF() {
        MappingTestData edgeCase = new DefaultMappingConfig();

        OGDataset d = edgeCase.underlyingOG();
        Model expected = edgeCase.ogMappedToRDF();

        RDFMapper mapper = new RDFMapper(d);
        Model result = mapper.createRDFModelFromOGDataset();

        assertEquals(expected, result);
    }

    @Test
    public void customMappingConfigOGToRDF() {
        MappingTestData edgeCase = new CustomMappingConfig();

        OGDataset d = edgeCase.underlyingOG();
        Model expected = edgeCase.ogMappedToRDF();

        RDFMapper mapper = new RDFMapper(d);
        Model result = mapper.createRDFModelFromOGDataset();

        assertEquals(expected, result);
    }

    @Test
    public void LPGEdgeCasesDataOGToRDF() {
        MappingTestData edgeCase = new LPGEdgeCasesData();

        OGDataset d = edgeCase.underlyingOG();
        Model expected = edgeCase.ogMappedToRDF();

        RDFMapper mapper = new RDFMapper(d);
        Model result = mapper.createRDFModelFromOGDataset();

        assertEquals(expected, result);
    }
    @Test
    public void defaultMappingConfigWithCustomConfigOGToRDF() {
        MappingTestData edgeCase = new DefaultMappingConfigWithCustomConfig();

        OGDataset d = edgeCase.underlyingOG();
        Model expected = edgeCase.ogMappedToRDF();

        RDFMapper mapper = new RDFMapper(d);
        Model result = mapper.createRDFModelFromOGDataset();

        assertEquals(expected, result);
    }
    @Test
    public void differentPropertyValueTypesOGToRDF() {
        MappingTestData edgeCase = new DifferentPropertyValueTypes();

        OGDataset d = edgeCase.underlyingOG();
        Model expected = edgeCase.ogMappedToRDF();

        RDFMapper mapper = new RDFMapper(d);
        Model result = mapper.createRDFModelFromOGDataset();

        assertEquals(expected, result);
    }
    @Test
    public void complexReificationsOGToRDF() {
        MappingTestData edgeCase = new ComplexReifications();

        OGDataset d = edgeCase.underlyingOG();
        Model expected = edgeCase.ogMappedToRDF();

        RDFMapper mapper = new RDFMapper(d);
        Model result = mapper.createRDFModelFromOGDataset();

        assertEquals(expected, result);
    }

    @Test
    public void unassertedStatementsUsedInAssertedOGToRDF() {
        MappingTestData edgeCase = new UnassertedStatementsUsedInAsserted();

        OGDataset d = edgeCase.underlyingOG();
        Model expected = edgeCase.ogMappedToRDF();

        RDFMapper mapper = new RDFMapper(d);
        Model result = mapper.createRDFModelFromOGDataset();

        assertEquals(expected, result);
    }


    // Testing RDF to OG

    @Test
    public void simpleReificationRDFToOG() {
        MappingTestData edgeCase = new SimpleReification();

        Model d = edgeCase.ogMappedToRDF();
        OGDataset expected = edgeCase.rdfMappedToOG();

        RDFMapper mapper = new RDFMapper();
        mapper.addRDFModelToOG(d);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void multipleGraphsRDFToOG() {
        MappingTestData edgeCase = new MultipleGraphs();

        Model d = edgeCase.ogMappedToRDF();
        OGDataset expected = edgeCase.rdfMappedToOG();

        RDFMapper mapper = new RDFMapper();
        mapper.addRDFModelToOG(d);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void defaultMappingConfigRDFToOG() {
        MappingTestData edgeCase = new DefaultMappingConfig();

        Model d = edgeCase.ogMappedToRDF();
        OGDataset expected = edgeCase.rdfMappedToOG();

        RDFMapper mapper = new RDFMapper();
        mapper.addRDFModelToOG(d);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void customMappingConfigRDFToOG() {
        MappingTestData edgeCase = new CustomMappingConfig();

        Model d = edgeCase.ogMappedToRDF();
        OGDataset expected = edgeCase.rdfMappedToOG();

        RDFMapper mapper = new RDFMapper();
        mapper.addRDFModelToOG(d);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void LPGEdgeCasesDataRDFToOG() {
        MappingTestData edgeCase = new LPGEdgeCasesData();

        Model d = edgeCase.ogMappedToRDF();
        OGDataset expected = edgeCase.rdfMappedToOG();

        RDFMapper mapper = new RDFMapper();
        mapper.addRDFModelToOG(d);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void defaultMappingConfigWithCustomConfigRDFToOG() {
        MappingTestData edgeCase = new DefaultMappingConfigWithCustomConfig();

        Model d = edgeCase.ogMappedToRDF();
        OGDataset expected = edgeCase.rdfMappedToOG();

        RDFMapper mapper = new RDFMapper();
        mapper.addRDFModelToOG(d);
        OGDataset result = mapper.dataset;


        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void differentPropertyValueTypesRDFToOG() {
        MappingTestData edgeCase = new DifferentPropertyValueTypes();

        Model d = edgeCase.ogMappedToRDF();
        OGDataset expected = edgeCase.rdfMappedToOG();

        RDFMapper mapper = new RDFMapper();
        mapper.addRDFModelToOG(d);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void complexReificationsRDFToOG() {
        MappingTestData edgeCase = new ComplexReifications();

        Model d = edgeCase.ogMappedToRDF();
        OGDataset expected = edgeCase.rdfMappedToOG();

        RDFMapper mapper = new RDFMapper();
        mapper.addRDFModelToOG(d);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void unassertedStatementsUsedInAssertedRDFToOG() {
        MappingTestData edgeCase = new UnassertedStatementsUsedInAsserted();

        Model d = edgeCase.ogMappedToRDF();
        OGDataset expected = edgeCase.rdfMappedToOG();

        RDFMapper mapper = new RDFMapper();
        mapper.addRDFModelToOG(d);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }
}