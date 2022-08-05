package software.amazon.neptune.onegraph.playground.server.tests.unit.converting;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.mapping.TinkerPopConverter;
import software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion.TinkerConversionTestCase;
import software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion.SimpleGraph;
import software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion.SupportedPropertyValueTypes;
import software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion.SetAndListCardinalities;
import software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion.MultiLabeledVertex;
import software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion.MetaProperties;
import software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion.UnsupportedPropertyValueTypes;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.helper.Equivalence;
import software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion.NonExplicitLabels;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TinkerPopConverterTest {

    // From Tinker to LPG

    public TinkerPopConverter.TinkerPopConverterDelegate anonDelegate = new TinkerPopConverter.TinkerPopConverterDelegate() {
        @Override
        public void unsupportedValueFound(String infoString) {}

        @Override
        public void metaPropertyFound(String infoString) {}

        @Override
        public void multiLabeledVertexFound(String infoString) {}
    };
    
    @Test
    public void simpleGraphTinkerToLPG() {
        TinkerConversionTestCase t = new SimpleGraph();

        LPGGraph expected = t.lpgGraph();
        LPGGraph result = (new TinkerPopConverter(anonDelegate)).convertTinkerGraphToLPG(t.tinkerPopGraph());

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void supportedPropertyValueTypesTinkerToLPG() {
        TinkerConversionTestCase t = new SupportedPropertyValueTypes();

        LPGGraph expected = t.lpgGraph();
        LPGGraph result = (new TinkerPopConverter(anonDelegate)).convertTinkerGraphToLPG(t.tinkerPopGraph());

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void setAndListCardinalitiesTinkerToLPG() {
        TinkerConversionTestCase t = new SetAndListCardinalities();

        LPGGraph expected = t.lpgGraph();
        LPGGraph result = (new TinkerPopConverter(anonDelegate)).convertTinkerGraphToLPG(t.tinkerPopGraph());

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void nonExplicitLabelsTinkerToLPG() {
        TinkerConversionTestCase t = new NonExplicitLabels();

        LPGGraph expected = t.lpgGraph();
        LPGGraph result = (new TinkerPopConverter(anonDelegate)).convertTinkerGraphToLPG(t.tinkerPopGraph());

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void metaPropertiesTinkerToLPG() {
        TinkerConversionTestCase t = new MetaProperties();

        LPGGraph expected = t.lpgGraph();
        TinkerPopConverter c = new TinkerPopConverter(new TinkerPopConverter.TinkerPopConverterDelegate() {
            @Override
            public void unsupportedValueFound(String infoString) {
                fail();
            }

            @Override
            public void metaPropertyFound(String infoString) {
                assertTrue(infoString.contains("The meta-property on the property with key"));
            }

            @Override
            public void multiLabeledVertexFound(String infoString) {
                fail();
            }
        });
        LPGGraph result = c.convertTinkerGraphToLPG(t.tinkerPopGraph());

        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void unsupportedPropertyValueTypesTinkerToLPG() {
        TinkerConversionTestCase t = new UnsupportedPropertyValueTypes();

        LPGGraph expected = t.lpgGraph();
        TinkerPopConverter c = new TinkerPopConverter(new TinkerPopConverter.TinkerPopConverterDelegate() {
            @Override
            public void unsupportedValueFound(String infoString) {
                assertTrue(infoString.contains("Property values of type LIST, SET, and MAP are currently not supported"));
            }

            @Override
            public void metaPropertyFound(String infoString) {
                fail();
            }

            @Override
            public void multiLabeledVertexFound(String infoString) {
                fail();
            }
        });

        LPGGraph result = c.convertTinkerGraphToLPG(t.tinkerPopGraph());
        Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
    }

    // From LPG to Tinker

    @Test
    public void simpleGraphLPGToTinker() {
        TinkerConversionTestCase t = new SimpleGraph();

        Graph expected = t.tinkerPopGraph();
        Graph result = (new TinkerPopConverter(anonDelegate)).convertLPGToTinkerGraph(t.lpgGraph());

        Equivalence.assertTinkerPopGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void supportedPropertyValueTypesLPGToTinker() {
        TinkerConversionTestCase t = new SupportedPropertyValueTypes();

        Graph expected = t.tinkerPopGraph();
        Graph result = (new TinkerPopConverter(anonDelegate)).convertLPGToTinkerGraph(t.lpgGraph());

        Equivalence.assertTinkerPopGraphsAreSuperficiallyEqual(expected, result);
    }
    
    @Test
    public void setAndListCardinalitiesLPGToTinker() {
        TinkerConversionTestCase t = new SetAndListCardinalities();

        Graph expected = t.tinkerPopGraph();
        Graph result = (new TinkerPopConverter(anonDelegate)).convertLPGToTinkerGraph(t.lpgGraph());

        Equivalence.assertTinkerPopGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void nonExplicitLabelsLPGToTinker() {
        TinkerConversionTestCase t = new NonExplicitLabels();

        Graph expected = t.tinkerPopGraph();
        Graph result = (new TinkerPopConverter(anonDelegate)).convertLPGToTinkerGraph(t.lpgGraph());

        Equivalence.assertTinkerPopGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void multiLabeledVertexLPGToTinker() {
        TinkerConversionTestCase t = new MultiLabeledVertex();

        Graph expected = t.tinkerPopGraph();

        TinkerPopConverter c = new TinkerPopConverter(new TinkerPopConverter.TinkerPopConverterDelegate() {
            @Override
            public void unsupportedValueFound(String infoString) {
                fail();
            }

            @Override
            public void metaPropertyFound(String infoString) {
                fail();
            }

            @Override
            public void multiLabeledVertexFound(String infoString) {
                assertTrue(infoString.contains("Multi-labeled vertex encountered"));
            }
        });
        Graph result = c.convertLPGToTinkerGraph(t.lpgGraph());

        Equivalence.assertTinkerPopGraphsAreSuperficiallyEqual(expected, result);
    }

    @Test
    public void metaPropertiesLPGToTinker() {
        TinkerConversionTestCase t = new MetaProperties();

        Graph expected = t.tinkerPopGraph();
        Graph result = (new TinkerPopConverter(anonDelegate)).convertLPGToTinkerGraph(t.lpgGraph());

        Equivalence.assertTinkerPopGraphsAreSuperficiallyEqual(expected, result);
    }
}
