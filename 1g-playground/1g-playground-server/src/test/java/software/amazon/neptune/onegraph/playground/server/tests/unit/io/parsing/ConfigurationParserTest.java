package software.amazon.neptune.onegraph.playground.server.tests.unit.io.parsing;

import software.amazon.neptune.onegraph.playground.server.io.parsing.ConfigurationParser;
import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigurationParserTest {

    private static final String rootPath = "/parserTestDatasets/LPG/LPGMappingConfigurations/";

    private Path getPathToResource(String resourceName) {
        URL pathURL = ConfigurationParserTest.class.getResource(rootPath + resourceName);
        assert pathURL != null;
        return Paths.get(pathURL.getPath());
    }

    @Test
    void parseNormalConfig() {
        Path og = getPathToResource("normal_lpg_config.json");

        LPGMappingConfiguration config = new LPGMappingConfiguration();
        ConfigurationParser.parseConfiguration(og, config);

        IRI dnl = SimpleValueFactory.getInstance().createIRI("http://example.com/dnl");
        IRI person = SimpleValueFactory.getInstance().createIRI("http://example.com/Person");
        IRI is = SimpleValueFactory.getInstance().createIRI("http://example.com/is");
        IRI knows = SimpleValueFactory.getInstance().createIRI("http://example.com/knows");

        String connected = "Connected to";

        assertEquals(dnl, config.defaultNodeLabelPredicate);
        assertTrue(config.edgeLabelMapping("knows").isPresent());
        assertTrue(config.edgeLabelMapping("connected").isPresent());
        assertTrue(config.propertyNameMapping("age").isPresent());
        assertTrue(config.propertyNameMapping("probability").isPresent());
        assertTrue(config.nodeLabelMapping("Person").isPresent());
        assertTrue(config.nodeLabelMapping("Boss").isPresent());
        assertTrue(config.nodeLabelMapping("Dog").isPresent());

        assertEquals(knows, config.edgeLabelMapping("knows").get().predicate);
        assertEquals(connected, config.edgeLabelMapping("connected").get().predicate);

        assertEquals(is, config.nodeLabelMapping("Person").get().predicate);
        assertEquals(person, config.nodeLabelMapping("Person").get().object);

        assertTrue(config.nodeLabelMappingInverse(is, person).isPresent());
        assertEquals("Person",config.nodeLabelMappingInverse(is, person).get());
    }

    @Test
    void parseConfigWhereDNLandNLMIsNotIRI() {
        Path og = getPathToResource("not_an_iri_lpg_config.json");

        //Path og = rootPath.resolve("not_an_iri_lpg_config.json");

        LPGMappingConfiguration config = new LPGMappingConfiguration();

        ParserException exception = assertThrows(ParserException.class,
                () -> ConfigurationParser.parseConfiguration(og, config));
        String expectedMessage = "IRI";
        String actualMessage = exception.getCause().getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void parseMalformedConfig() {
        Path og = getPathToResource("malformed_lpg_config.json");

        //Path og = rootPath.resolve("malformed_lpg_config.json");

        LPGMappingConfiguration config = new LPGMappingConfiguration();

        ParserException exception = assertThrows(ParserException.class,
                () -> ConfigurationParser.parseConfiguration(og, config));
        String expectedMessage = "failed";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void parseIncorrectArraySize() {

        Path og = getPathToResource("malformed_lpg_config_incorrect_array_size.json");

        //Path og = rootPath.resolve("malformed_lpg_config_incorrect_array_size.json");

        LPGMappingConfiguration config = new LPGMappingConfiguration();

        ParserException exception = assertThrows(ParserException.class,
                () -> ConfigurationParser.parseConfiguration(og, config));
        String expectedMessage = "size";
        String actualMessage = exception.getCause().getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
