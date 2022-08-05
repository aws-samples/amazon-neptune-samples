package software.amazon.neptune.onegraph.playground.server.tests.unit.io.parsing;

import org.eclipse.rdf4j.model.Literal;
import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;
import software.amazon.neptune.onegraph.playground.server.io.parsing.ogparser.OGParser;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OGParserTest {

    String rootPathLiterals = "/parserTestDatasets/oneGraph/literals/";
    String rootPathMalformed = "/parserTestDatasets/oneGraph/malformed/";
    String rootPathNormal = "/parserTestDatasets/oneGraph/normal/";
    String rootPathReferenceCycles = "/parserTestDatasets/oneGraph/referenceCycles/";

    private Path getPathToResource(String resource) {
        URL pathURL = ConfigurationParserTest.class.getResource(resource);
        assert pathURL != null;
        return Paths.get(pathURL.getPath());
    }

    private Path getPathToLiterals(String resourceName) {
        return getPathToResource(rootPathLiterals + resourceName);
    }

    private Path getPathToMalformed(String resourceName) {
        return getPathToResource(rootPathMalformed + resourceName);
    }

    private Path getPathToNormal(String resourceName) {
        return getPathToResource(rootPathNormal + resourceName);
    }

    private Path getPathToReferenceCycles(String resourceName) {
        return getPathToResource(rootPathReferenceCycles + resourceName);
    }

    @Test
    void literalDataTypes() throws Exception {
        Path og = getPathToLiterals("og_lit_dataTypes.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        try (InputStream s = Files.newInputStream(og)) {
            p.parseOGTriples(s, d);

            assertEquals(3, Iterables.size(d.getPropertyStatements()));
            assertEquals(2, Iterables.size(d.getSimpleNodes()));
            assertEquals(1, Iterables.size(d.getGraphs()));
        }
    }

    @Test
    void literalEscapedCharacters() throws Exception {
        Path og = getPathToLiterals("og_lit_escapedCharacters.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        try (InputStream s = Files.newInputStream(og)) {
            p.parseOGTriples(s, d);

            assertEquals(1, Iterables.size(d.getPropertyStatements()));
            assertTrue(d.getPropertyStatements().iterator().next().object.value instanceof Literal);
            Literal l = (Literal) d.getPropertyStatements().iterator().next().object.value;
            assertTrue(l.getLabel().contains("\""));
        }
    }

    @Test
    void literalLanguageTags() throws Exception {
        Path og = getPathToLiterals("og_lit_languageTags.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();
        try (InputStream s = Files.newInputStream(og)) {
            p.parseOGTriples(s, d);

            assertEquals(4, Iterables.size(d.getPropertyStatements()));
            assertEquals(1, Iterables.size(d.getGraphs()));
            assertTrue(d.getPropertyStatements().iterator().next().object.value instanceof Literal);
            Literal l = (Literal) d.getPropertyStatements().iterator().next().object.value;
            assertTrue(l.getLanguage().isPresent());
        }
    }

    @Test
    void literalUnicodeEscaped() throws Exception {
        Path og = getPathToLiterals("og_lit_unicodeEscaped.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();
        try (InputStream s = Files.newInputStream(og)) {
            p.parseOGTriples(s, d);

            assertEquals(1, Iterables.size(d.getPropertyStatements()));
            assertTrue(d.getPropertyStatements().iterator().next().object.value instanceof Literal);
            Literal l = (Literal) d.getPropertyStatements().iterator().next().object.value;
            assertTrue(l.getLabel().contains("¾"));
        }
    }

    @Test
    void malformedIRI() {
        Path og = getPathToMalformed("og_malformed_iri.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "IRI";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void malformedLiteral1() {
        Path og = getPathToMalformed("og_malformed_literal.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "literal";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void malformedLiteral2() {
        Path og = getPathToMalformed("og_malformed_literal_2.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "Unexpected";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void malformedLiteral3() {
        Path og = getPathToMalformed("og_malformed_literal_3.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "end of line";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void malformedLiteral4() {
        Path og = getPathToMalformed("og_malformed_literal_4.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "valid IRI";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void malformedLiteral5() {
        Path og = getPathToMalformed("og_malformed_literal_5.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "is not a supported";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void malformedLiteral6() {
        Path og = getPathToMalformed("og_malformed_literal_6.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "Unexpected end of line";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void unexpectedEntry1() {
        Path og = getPathToMalformed("og_unexpected_entry.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "allowed";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void unexpectedEntry2() {
        Path og = getPathToMalformed("og_unexpected_entry_2.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "Blank node or IRI";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void unexpectedEntry3() {
        Path og = getPathToMalformed("og_unexpected_entry_3.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "Unexpected";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void unexpectedEntry4() {
        Path og = getPathToMalformed("og_unexpected_entry_4.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "Blank";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void unexpectedEntry5() {
        Path og = getPathToMalformed("og_unexpected_entry_5.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "Unexpected";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void unexpectedEOL() {
        Path og = getPathToMalformed("og_unexpected_eol.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "end of";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void unexpectedEOL2() {
        Path og = getPathToMalformed("og_unexpected_eol_2.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "end of";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void missingSID() {
        Path og = getPathToMalformed("og_missing_sid.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "present";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void normal1() throws Exception {
        Path og = getPathToNormal("og_normal_1.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        try (InputStream s = Files.newInputStream(og)) {
            p.parseOGTriples(s, d);

            assertEquals(19, Iterables.size(d.getMembershipStatements()));
            assertEquals(7, Iterables.size(d.getRelationshipStatements()));
            assertEquals(2, Iterables.size(d.getPropertyStatements()));
            assertEquals(4, Iterables.size(d.getSimpleNodes()));
            assertEquals(4, Iterables.size(d.getGraphs()));
        }
    }

    @Test
    void normal2() throws Exception {
        Path og = getPathToNormal("og_normal_2.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        try (InputStream s = Files.newInputStream(og)) {
            p.parseOGTriples(s, d);

            assertEquals(7, Iterables.size(d.getMembershipStatements()));
            assertEquals(3, Iterables.size(d.getRelationshipStatements()));
            assertEquals(1, Iterables.size(d.getPropertyStatements()));
            assertEquals(7, Iterables.size(d.getSimpleNodes()));
            assertEquals(3, Iterables.size(d.getGraphs()));
        }
    }

    @Test
    void normal3() throws Exception {
        Path og = getPathToNormal("og_normal_3.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        try (InputStream s = Files.newInputStream(og)) {
            p.parseOGTriples(s, d);

            assertEquals(19, Iterables.size(d.getMembershipStatements()));
            assertEquals(6, Iterables.size(d.getRelationshipStatements()));
            assertEquals(0, Iterables.size(d.getPropertyStatements()));
            assertEquals(7, Iterables.size(d.getSimpleNodes()));
            assertEquals(6, Iterables.size(d.getGraphs()));
        }
    }

    @Test
    void refCycleComplex() {
        Path og = getPathToReferenceCycles("og_ref_cycle_complex.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "reference cycle";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void refCycleSelf() {
        Path og = getPathToReferenceCycles("og_ref_cycle_self.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "reference cycle";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void loadNonExistentOG() {
        Path og = getPathToReferenceCycles("og_ref_cycle_simple.txt");
        OGDataset d = new OGDataset();
        OGParser p = new OGParser();

        ParserException exception = assertThrows(ParserException.class, () -> {
            try (InputStream s = Files.newInputStream(og)) {
                p.parseOGTriples(s, d);
            }
        });
        String expectedMessage = "reference cycle";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
