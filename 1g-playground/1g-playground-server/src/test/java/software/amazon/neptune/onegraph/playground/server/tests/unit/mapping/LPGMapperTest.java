package software.amazon.neptune.onegraph.playground.server.tests.unit.mapping;

import software.amazon.neptune.onegraph.playground.server.mapping.LPGMapper;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.MappingTestData;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.DefaultMappingConfig;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.CustomMappingConfig;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.MultipleVertexDefinitions;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.helper.Equivalence;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.SimpleReification;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.UnassertedStatementsUsedInAsserted;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.MultipleGraphs;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.LPGEdgeCasesData;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.DefaultMappingConfigWithCustomConfig;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.DifferentPropertyValueTypes;
import software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata.ComplexReifications;
import org.junit.jupiter.api.Test;

class LPGMapperTest {

    // Test cases OG to LPG

    @Test
    public void simpleReificationOGtoLPG() {
        SimpleReification edgeCase = new SimpleReification();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void multipleGraphsOGtoLPG() {
        MappingTestData edgeCase = new MultipleGraphs();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }
    @Test
    public void defaultMappingConfigOGtoLPG() {
        MappingTestData edgeCase = new DefaultMappingConfig();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void customMappingConfigOGtoLPG() {
        MappingTestData edgeCase = new CustomMappingConfig();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void LPGEdgeCasesDataOGtoLPG() {
        MappingTestData edgeCase = new LPGEdgeCasesData();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }
    @Test
    public void defaultMappingConfigWithCustomConfigOGtoLPG() {
        MappingTestData edgeCase = new DefaultMappingConfigWithCustomConfig();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }
    @Test
    public void differentPropertyValueTypesOGtoLPG() {
        MappingTestData edgeCase = new DifferentPropertyValueTypes();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }
    @Test
    public void complexReificationsOGtoLPG() {
        MappingTestData edgeCase = new ComplexReifications();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void unassertedStatementsUsedInAssertedOGtoLPG() {
        MappingTestData edgeCase = new UnassertedStatementsUsedInAsserted();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void MultipleVertexDefinitionsOGToLPG() {
        MultipleVertexDefinitions edgeCase = new MultipleVertexDefinitions();

        OGDataset d = edgeCase.underlyingOG();
        LPGGraph expected = edgeCase.ogMappedToLPG();

        LPGMapper mapper = new LPGMapper(d, edgeCase.mappingConfiguration());
        LPGGraph result = mapper.createLPGFromOGDataset();

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    // Test cases LPG to OG

    @Test
    void simpleReificationLPGToOG() {
        MappingTestData edgeCase = new SimpleReification();

        LPGGraph g = edgeCase.ogMappedToLPG();
        OGDataset expected = edgeCase.lpgMappedToOG();

        LPGMapper mapper = new LPGMapper(edgeCase.mappingConfiguration());
        mapper.addLPGToOGDataset(g);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    void multipleGraphsLPGToOG() {
        MappingTestData edgeCase = new MultipleGraphs();

        LPGGraph g = edgeCase.ogMappedToLPG();
        OGDataset expected = edgeCase.lpgMappedToOG();

        LPGMapper mapper = new LPGMapper(edgeCase.mappingConfiguration());
        mapper.addLPGToOGDataset(g);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    void defaultMappingConfigLPGToOG() {
        MappingTestData edgeCase = new DefaultMappingConfig();

        LPGGraph g = edgeCase.ogMappedToLPG();
        OGDataset expected = edgeCase.lpgMappedToOG();

        LPGMapper mapper = new LPGMapper(edgeCase.mappingConfiguration());
        mapper.addLPGToOGDataset(g);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    void customMappingConfigLPGToOG() {
        MappingTestData edgeCase = new CustomMappingConfig();

        LPGGraph g = edgeCase.ogMappedToLPG();
        OGDataset expected = edgeCase.lpgMappedToOG();

        LPGMapper mapper = new LPGMapper(edgeCase.mappingConfiguration());
        mapper.addLPGToOGDataset(g);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    void LPGEdgeCasesDataLPGToOG() {
        MappingTestData edgeCase = new LPGEdgeCasesData();

        LPGGraph g = edgeCase.ogMappedToLPG();
        OGDataset expected = edgeCase.lpgMappedToOG();

        LPGMapper mapper = new LPGMapper(edgeCase.mappingConfiguration());
        mapper.addLPGToOGDataset(g);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    void defaultMappingConfigWithCustomConfigLPGToOG() {
        MappingTestData edgeCase = new DefaultMappingConfigWithCustomConfig();

        LPGGraph g = edgeCase.ogMappedToLPG();
        OGDataset expected = edgeCase.lpgMappedToOG();

        LPGMapper mapper = new LPGMapper(edgeCase.mappingConfiguration());
        mapper.addLPGToOGDataset(g);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    void differentPropertyValueTypesLPGToOG() {
        MappingTestData edgeCase = new DifferentPropertyValueTypes();

        LPGGraph g = edgeCase.ogMappedToLPG();
        OGDataset expected = edgeCase.lpgMappedToOG();

        LPGMapper mapper = new LPGMapper(edgeCase.mappingConfiguration());
        mapper.addLPGToOGDataset(g);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    void complexReificationsLPGToOG() {
        MappingTestData edgeCase = new ComplexReifications();

        LPGGraph g = edgeCase.ogMappedToLPG();
        OGDataset expected = edgeCase.lpgMappedToOG();

        LPGMapper mapper = new LPGMapper(edgeCase.mappingConfiguration());
        mapper.addLPGToOGDataset(g);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }

    @Test
    void unassertedStatementsUsedInAssertedLPGToOG() {
        MappingTestData edgeCase = new UnassertedStatementsUsedInAsserted();

        LPGGraph g = edgeCase.ogMappedToLPG();
        OGDataset expected = edgeCase.lpgMappedToOG();

        LPGMapper mapper = new LPGMapper(edgeCase.mappingConfiguration());
        mapper.addLPGToOGDataset(g);
        OGDataset result = mapper.dataset;

        Equivalence.assertOGDatasetsAreSuperficiallyEqual(expected, result);
    }
}