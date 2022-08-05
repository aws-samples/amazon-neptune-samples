package software.amazon.neptune.onegraph.playground.server.tests.unit.service;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.helper.Equivalence;
import software.amazon.neptune.onegraph.playground.server.service.LoadService;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.tests.unit.io.parsing.ConfigurationParserTest;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LoadServiceTest {


    private static final String rootPath = "/parserTestDatasets/";

    private Path getPathToResource(String resourceName) {
        URL pathURL = ConfigurationParserTest.class.getResource(rootPath + resourceName);
        assert pathURL != null;
        return Paths.get(pathURL.getPath());
    }

    @Test
    public void loadRDF() throws Exception {
        Path rdf = getPathToResource("RDF/rdfxml/rdfxml_normal.rdf");

        State state = new State();
        LoadService service = new LoadService(state);

        Assertions.assertTrue(state.getOgDataset().isEmpty());
        assertEquals(0, state.getRdfModel().size());
        assertEquals(0, Iterators.size(state.getTinkerPopGraph().vertices()));

        try (InputStream stream = Files.newInputStream(rdf)) {
            service.loadDataAndUpdateState(DataFormat.RDFXML, stream, null);

            assertEquals(5, Iterables.size(state.getOgDataset().getRelationshipStatements()));
            assertEquals(6, Iterables.size(state.getOgDataset().getPropertyStatements()));
            assertEquals(11, Iterables.size(state.getOgDataset().getMembershipStatements()));
            assertEquals(1, Iterables.size(state.getOgDataset().getGraphs()));
            assertEquals(9, Iterables.size(state.getOgDataset().getSimpleNodes()));

            assertEquals(11, state.getRdfModel().size());
            assertEquals(4, Iterators.size(state.getTinkerPopGraph().vertices()));
            assertEquals(2, Iterators.size(state.getTinkerPopGraph().edges()));
        }
    }

    @Test
    public void loadGraphML() throws Exception {
        Path graphML = getPathToResource("LPG/graphML/graphML_normal.xml");

        State state = new State();
        LoadService service = new LoadService(state);

        assertTrue(state.getOgDataset().isEmpty());
        assertEquals(0, state.getRdfModel().size());
        assertEquals(0, Iterators.size(state.getTinkerPopGraph().vertices()));

        try (InputStream stream = Files.newInputStream(graphML)) {
            service.loadDataAndUpdateState(DataFormat.GRAPHML, stream, null);

            assertEquals(12, Iterables.size(state.getOgDataset().getRelationshipStatements()));
            assertEquals(18, Iterables.size(state.getOgDataset().getPropertyStatements()));
            assertEquals(30, Iterables.size(state.getOgDataset().getMembershipStatements()));
            assertEquals(1, Iterables.size(state.getOgDataset().getGraphs()));
            assertEquals(15, Iterables.size(state.getOgDataset().getSimpleNodes()));

            assertEquals(24, state.getRdfModel().size());
            assertEquals(6, Iterators.size(state.getTinkerPopGraph().vertices()));
            assertEquals(6, Iterators.size(state.getTinkerPopGraph().edges()));
        }
    }

    @Test
    public void loadGraphSON() throws Exception {
        Path graphSON = getPathToResource("LPG/graphSON/graphSON_normal.json");

        State state = new State();
        LoadService service = new LoadService(state);

        Assertions.assertTrue(state.getOgDataset().isEmpty());
        assertEquals(0, state.getRdfModel().size());
        assertEquals(0, Iterators.size(state.getTinkerPopGraph().vertices()));

        try (InputStream stream = Files.newInputStream(graphSON)) {
            service.loadDataAndUpdateState(DataFormat.GRAPHSON, stream, null);

            assertEquals(12, Iterables.size(state.getOgDataset().getRelationshipStatements()));
            assertEquals(18, Iterables.size(state.getOgDataset().getPropertyStatements()));
            assertEquals(30, Iterables.size(state.getOgDataset().getMembershipStatements()));
            assertEquals(1, Iterables.size(state.getOgDataset().getGraphs()));
            assertEquals(15, Iterables.size(state.getOgDataset().getSimpleNodes()));

            assertEquals(24, state.getRdfModel().size());
            assertEquals(6, Iterators.size(state.getTinkerPopGraph().vertices()));
            assertEquals(6, Iterators.size(state.getTinkerPopGraph().edges()));
        }
    }

    @Test
    public void loadNeptuneCSV() throws Exception {
        Path neptuneCSVNodes = getPathToResource("LPG/NeptuneCSV/nCSV_nodes_normal.csv");
        Path neptuneCSVEdges = getPathToResource("LPG/NeptuneCSV/nCSV_edges_normal.csv");

        State state = new State();
        LoadService service = new LoadService(state);

        Assertions.assertTrue(state.getOgDataset().isEmpty());
        assertEquals(0, state.getRdfModel().size());
        assertEquals(0, Iterators.size(state.getTinkerPopGraph().vertices()));

        try (InputStream stream1 = Files.newInputStream(neptuneCSVNodes);
             InputStream stream2 = Files.newInputStream(neptuneCSVEdges)) {
            service.loadDataAndUpdateState(DataFormat.NEPTUNECSV, stream1, stream2);

            assertEquals(7, Iterables.size(state.getOgDataset().getRelationshipStatements()));
            assertEquals(13, Iterables.size(state.getOgDataset().getPropertyStatements()));
            assertEquals(20, Iterables.size(state.getOgDataset().getMembershipStatements()));
            assertEquals(1, Iterables.size(state.getOgDataset().getGraphs()));
            assertEquals(15, Iterables.size(state.getOgDataset().getSimpleNodes()));

            assertEquals(17, state.getRdfModel().size());
            assertEquals(4, Iterators.size(state.getTinkerPopGraph().vertices()));
            assertEquals(2, Iterators.size(state.getTinkerPopGraph().edges()));
        }
    }

    @Test
    public void loadOG() throws Exception {
        Path og = getPathToResource("oneGraph/normal/og_normal_1.txt");

        State state = new State();
        LoadService service = new LoadService(state);

        Assertions.assertTrue(state.getOgDataset().isEmpty());
        assertEquals(0, state.getRdfModel().size());
        assertEquals(0, Iterators.size(state.getTinkerPopGraph().vertices()));

        try (InputStream stream = Files.newInputStream(og)) {
            service.loadDataAndUpdateState(DataFormat.OG, stream, null);

            assertEquals(7, Iterables.size(state.getOgDataset().getRelationshipStatements()));
            assertEquals(2, Iterables.size(state.getOgDataset().getPropertyStatements()));
            assertEquals(19, Iterables.size(state.getOgDataset().getMembershipStatements()));
            assertEquals(4, Iterables.size(state.getOgDataset().getGraphs()));
            assertEquals(4, Iterables.size(state.getOgDataset().getSimpleNodes()));

            assertEquals(10, state.getRdfModel().size());
            assertEquals(3, Iterators.size(state.getTinkerPopGraph().vertices()));
            assertEquals(4, Iterators.size(state.getTinkerPopGraph().edges()));
        }
    }

    @Test
    public void loadWrongFormat() {

        Path graphSON = getPathToResource("LPG/graphSON/graphSON_normal.json");

        State state = new State();
        LoadService service = new LoadService(state);

        LoadService.LoadException exception = assertThrows(LoadService.LoadException.class, () -> {
            try (InputStream stream = Files.newInputStream(graphSON)) {
                service.loadDataAndUpdateState(DataFormat.RDFXML, stream, null);
            }
        });
        String expectedMessage = "An exception occurred during load";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void loadNoSecondPath() {
        Path neptuneCSVNodes = getPathToResource("LPG/NeptuneCSV/nCSV_nodes_normal.csv");

        State state = new State();
        LoadService service = new LoadService(state);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            try (InputStream stream = Files.newInputStream(neptuneCSVNodes)) {
                service.loadDataAndUpdateState(DataFormat.NEPTUNECSV, stream, null);
            }
        });

        String expectedMessage = "NeptuneCSV specified but no second input stream given";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
