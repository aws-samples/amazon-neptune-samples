package software.amazon.neptune.onegraph.playground.server.tests.unit.io.parsing;

import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;
import software.amazon.neptune.onegraph.playground.server.io.parsing.RDFParser;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RDFParserTest {
    private static final String rootPath = "/parserTestDatasets/RDF/";

    private Path getPathToResource(String resource) {
        URL pathURL = ConfigurationParserTest.class.getResource(rootPath + resource);
        assert pathURL != null;
        return Paths.get(pathURL.getPath());
    }

    @Test
    void parseNQuads() throws Exception {
        Path path = getPathToResource("nQuads/nquads_normal.nq");
        try (InputStream s = Files.newInputStream(path)) {
            Model result = RDFParser.parseRDF(s, DataFormat.NQUADS);
            assertEquals(5, result.size());
        }
    }

    @Test
    void parseNTriples() throws Exception {
        Path path = getPathToResource("NTriples/ntriples_normal.nt");
        try (InputStream s = Files.newInputStream(path)) {
            Model result = RDFParser.parseRDF(s, DataFormat.NTRIPLES);
            assertEquals(3, result.size());
        }
    }

    @Test
    void parseRDFXML() throws Exception {
        Path path = getPathToResource("rdfxml/rdfxml_normal.rdf");
        try (InputStream s = Files.newInputStream(path)) {
            Model result = RDFParser.parseRDF(s, DataFormat.RDFXML);
            assertEquals(11, result.size());
        }
    }

    @Test
    void parseTriG() throws Exception {
        Path path = getPathToResource("TriG/trig_normal.trig");
        try (InputStream s = Files.newInputStream(path)) {
            Model result = RDFParser.parseRDF(s, DataFormat.TRIG);
            assertEquals(15, result.size());
        }
    }

    @Test
    void parseTurtle() throws Exception {
        Path path = getPathToResource("Turtle/turtle_normal.ttl");
        try (InputStream s = Files.newInputStream(path)) {
            Model result = RDFParser.parseRDF(s, DataFormat.TURTLE);
            assertEquals(4, result.size());
        }
    }

    @Test
    void parseWrongDataFormat() {

        Path path = getPathToResource("TriG/trig_normal.trig");

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(path)) {
                Model result = RDFParser.parseRDF(s, DataFormat.TURTLE);
            }
        });
        String expectedMessage = "failed";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
