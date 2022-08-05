package software.amazon.neptune.onegraph.playground.server.tests.unit.io.parsing;

import com.google.common.collect.Iterators;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.io.parsing.LPGParser;
import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LPGParserTest {

    private static final String rootPath = "/parserTestDatasets/LPG/";

    private Path getPathToResource(String resourceName) {
        URL pathURL = ConfigurationParserTest.class.getResource(rootPath + resourceName);
        assert pathURL != null;
        return Paths.get(pathURL.getPath());
    }

    @Test
    void parseNormalGraphML() throws Exception {
        Path graph = getPathToResource("graphML/graphML_normal.xml");

        try (InputStream s = Files.newInputStream(graph)) {
            Graph result = LPGParser.parseGraphML(s);
            assertEquals(6, Iterators.size(result.vertices()));
            assertEquals(6, Iterators.size(result.edges()));
        }
    }
    @Test
    void parseMalformedGraphML() {
        Path graph  = getPathToResource("graphML/graphML_malformed.xml");

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(graph)) {
                Graph result = LPGParser.parseGraphML(s);
                assertEquals(6, Iterators.size(result.vertices()));
                assertEquals(6, Iterators.size(result.edges()));
            }
        });
        String expectedMessage = "failed";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void parseNormalGraphSON() throws Exception {

        Path graph = getPathToResource("graphSON/graphSON_normal.json");
        try (InputStream s = Files.newInputStream(graph)) {
            Graph result = LPGParser.parseGraphSON(s);
            assertEquals(6, Iterators.size(result.vertices()));
            assertEquals(6, Iterators.size(result.edges()));
        }
    }

    @Test
    void parseMalformedGraphSON() {
        Path graph = getPathToResource("graphSON/graphSON_malformed.json");
        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(graph)) {
                Graph result = LPGParser.parseGraphSON(s);
            }
        });
        String expectedMessage = "failed";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void parseNormalNeptuneCSV() throws Exception {
        Path nodes = getPathToResource("NeptuneCSV/nCSV_nodes_normal.csv");
        Path edges = getPathToResource("NeptuneCSV/nCSV_edges_normal.csv");

        try (InputStream s = Files.newInputStream(nodes);
             InputStream s2 = Files.newInputStream(edges)) {
            Model nodesResult = LPGParser.parseNeptuneCSV(s);
            Model edgesResult = LPGParser.parseNeptuneCSV(s2);
            assertEquals(15, nodesResult.size());
            assertEquals(5, edgesResult.size());
        }
    }

    @Test
    void parseSamePropertyNamesCSV() throws Exception {
        Path nodes = getPathToResource("NeptuneCSV/nCSV_same_prop_names.csv");

        try (InputStream s = Files.newInputStream(nodes)) {
            Model nodesResult = LPGParser.parseNeptuneCSV(s);
            assertEquals(4, nodesResult.size());
        }
    }

    @Test
    void parseMalformedNeptuneCSV() {
        Path nodes = getPathToResource("NeptuneCSV/nCSV_malformed.csv");

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(nodes)) {
                Model nodesResult = LPGParser.parseNeptuneCSV(s);
            }
        });
        String expectedMessage = "failed";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void parseMultiEdgeNeptuneCSV() throws Exception {
        Path edges = getPathToResource("NeptuneCSV/nCSV_edges_multi_edge.csv");

        try (InputStream s = Files.newInputStream(edges)) {
            Model edgesResult = LPGParser.parseNeptuneCSV(s);
            assertEquals(2, edgesResult.size());
        }
    }
}
