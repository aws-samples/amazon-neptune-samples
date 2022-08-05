package software.amazon.neptune.onegraph.playground.server.tests.unit.converting;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.io.parsing.LPGParser;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.helper.Equivalence;
import software.amazon.neptune.onegraph.playground.server.mapping.NeptuneCSVConverter;
import software.amazon.neptune.onegraph.playground.server.tests.unit.io.parsing.ConfigurationParserTest;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NeptuneCSVConverterTest {

    private static final String rootPath = "/parserTestDatasets/LPG/";

    private Path getPathToResource(String resourceName) {
        URL pathURL = ConfigurationParserTest.class.getResource(rootPath + resourceName);
        assert pathURL != null;
        return Paths.get(pathURL.getPath());
    }

    @Test
    public void normalNeptuneCSV() throws Exception {
        Path nodes = getPathToResource("NeptuneCSV/nCSV_nodes_normal.csv");
        Path edges = getPathToResource("NeptuneCSV/nCSV_edges_normal.csv");

        try (InputStream s = Files.newInputStream(nodes);
             InputStream s2 = Files.newInputStream(edges)) {
            Model nodesResult = LPGParser.parseNeptuneCSV(s);
            Model edgesResult = LPGParser.parseNeptuneCSV(s2);

            String dateString1 = "2022-05-18T07:22:50Z";
            String dateString2 = "1958-06-12T00:00:00Z";
            String dateString3 = "1904-09-09T00:00:00Z";
            TemporalAccessor ta1 = DateTimeFormatter.ISO_INSTANT.parse(dateString1);
            TemporalAccessor ta2 = DateTimeFormatter.ISO_INSTANT.parse(dateString2);
            TemporalAccessor ta3 = DateTimeFormatter.ISO_INSTANT.parse(dateString3);
            Instant i1 = Instant.from(ta1);
            Instant i2 = Instant.from(ta2);
            Instant i3 = Instant.from(ta3);
            Date d1 = Date.from(i1);
            Date d2 = Date.from(i2);
            Date d3 = Date.from(i3);

            LPGGraph result = NeptuneCSVConverter.convertNeptuneCSVtoLPG(nodesResult, edgesResult);

            LPGGraph expected = new LPGGraph();
            LPGVertex v1 = new LPGVertex("Person","Queen");
            LPGVertex v2 = new LPGVertex("Person");
            LPGVertex v3 = new LPGVertex("Person");
            LPGVertex v4 = new LPGVertex("Dog");

            LPGEdge e1 = new LPGEdge(v1, v2, "knows");
            LPGEdge e2 = new LPGEdge(v2, v3, "likes");

            v1.addPropertyValue("age", 96);
            v1.addPropertyValue("name", "Elizabeth");
            v1.addPropertyValue("name", "Alexandra");
            v1.addPropertyValue("name", "Mary");
            v1.addPropertyValue("name", "Windsor");
            v1.addPropertyValue("birthDate", d1);
            v2.addPropertyValue("age", 39);
            v3.addPropertyValue("age", 22);
            v4.addPropertyValue("age",5);
            v2.addPropertyValue("name","Harry");

            e1.addPropertyValue("since",d2);
            e1.addPropertyValue("probability",0.6f);
            e2.addPropertyValue("since",d3);

            expected.addVertices(v1, v2, v3, v4);
            expected.addEdges(e1, e2);

            Equivalence.assertGraphsAreSuperficiallyEqual(expected, result);
        }
    }
}