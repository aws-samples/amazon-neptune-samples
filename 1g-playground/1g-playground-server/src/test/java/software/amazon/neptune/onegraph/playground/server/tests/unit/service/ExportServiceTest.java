package software.amazon.neptune.onegraph.playground.server.tests.unit.service;

import org.eclipse.rdf4j.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.helper.Equivalence;
import software.amazon.neptune.onegraph.playground.server.io.parsing.RDFParser;
import software.amazon.neptune.onegraph.playground.server.service.ExportService;
import software.amazon.neptune.onegraph.playground.server.service.LoadService;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.GeneralServiceTestData;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExportServiceTest {

    public static final State state = new State();

    @BeforeAll
    public static void loadData() {
        GeneralServiceTestData.addToDataSet(state.getOgDataset());
        state.reloadDerivativeData();
    }

    @Test
    public void exportToGraphMLRoundTrip() throws IOException {
        ExportService service = new ExportService(state);

        Path temp = Files.createTempFile("tempGraphML",".xml");
        service.exportData(DataFormat.GRAPHML, temp, null);

        State s = new State();
        LoadService l = new LoadService(s);
        try (InputStream stream = Files.newInputStream(temp)) {
            l.loadDataAndUpdateState(DataFormat.GRAPHML, stream, null);

            Equivalence.assertOGDatasetsAreSuperficiallyEqual(s.getOgDataset(), state.getOgDataset());
        }
    }

    @Test
    public void exportToGraphSONRoundTrip() throws IOException {
        ExportService service = new ExportService(state);

        Path temp = Files.createTempFile("tempGraphSON",".xml");
        service.exportData(DataFormat.GRAPHSON, temp, null);

        State s = new State();
        LoadService l = new LoadService(s);
        try (InputStream stream = Files.newInputStream(temp)) {
            l.loadDataAndUpdateState(DataFormat.GRAPHSON, stream, null);

            Equivalence.assertOGDatasetsAreSuperficiallyEqual(s.getOgDataset(), state.getOgDataset());
        }
    }

    @Test
    public void exportToOGRoundTrip() throws IOException {
        ExportService service = new ExportService(state);

        Path temp = Files.createTempFile("tempOG",".txt");
        service.exportData(DataFormat.OG, temp, null);

        State s = new State();
        LoadService l = new LoadService(s);
        try (InputStream stream = Files.newInputStream(temp)) {
            l.loadDataAndUpdateState(DataFormat.OG, stream, null);

            Equivalence.assertOGDatasetsAreSuperficiallyEqual(s.getOgDataset(), state.getOgDataset());
        }
    }

    @Test
    public void exportToTriGRoundTrip() throws IOException {
        ExportService service = new ExportService(state);

        Path temp = Files.createTempFile("tempTrig",".trig");
        service.exportData(DataFormat.TRIG, temp, null);

        State s = new State();
        LoadService l = new LoadService(s);
        try (InputStream stream = Files.newInputStream(temp)) {
            l.loadDataAndUpdateState(DataFormat.TRIG, stream, null);

            Equivalence.assertOGDatasetsAreSuperficiallyEqual(s.getOgDataset(), state.getOgDataset());
        }
    }

    @Test
    public void exportToNQuadsRoundTrip() throws IOException {
        ExportService service = new ExportService(state);

        Path temp = Files.createTempFile("tempNQ",".nq");
        service.exportData(DataFormat.NQUADS, temp, null);

        State s = new State();
        LoadService l = new LoadService(s);
        try (InputStream stream = Files.newInputStream(temp)) {
            l.loadDataAndUpdateState(DataFormat.NQUADS, stream, null);

            Equivalence.assertOGDatasetsAreSuperficiallyEqual(s.getOgDataset(), state.getOgDataset());
        }
    }

    @Test
    public void exportToNTriplesRoundTrip() throws IOException {
        ExportService service = new ExportService(state);

        Path temp = Files.createTempFile("tempNT",".nt");
        service.exportData(DataFormat.NTRIPLES, temp, null);

        State s = new State();
        LoadService l = new LoadService(s);
        try (InputStream stream = Files.newInputStream(temp)) {
            l.loadDataAndUpdateState(DataFormat.NTRIPLES, stream, null);

            Equivalence.assertOGDatasetsAreSuperficiallyEqual(s.getOgDataset(), state.getOgDataset());
        }
    }

    @Test
    public void exportToTurtleRoundTrip() throws IOException {
        ExportService service = new ExportService(state);

        Path temp = Files.createTempFile("tempTurtle",".ttl");
        service.exportData(DataFormat.TURTLE, temp, null);

        State s = new State();
        LoadService l = new LoadService(s);
        try (InputStream stream = Files.newInputStream(temp)) {
            l.loadDataAndUpdateState(DataFormat.TURTLE, stream, null);

            Equivalence.assertOGDatasetsAreSuperficiallyEqual(s.getOgDataset(), state.getOgDataset());
        }
    }

    @Test
    public void exportToRDFXMLRoundTrip() throws IOException {
        ExportService service = new ExportService(state);

        Path temp = Files.createTempFile("tempRDFXML",".rdf");
        service.exportData(DataFormat.RDFXML, temp, null);

        State s = new State();
        LoadService l = new LoadService(s);
        try (InputStream stream = Files.newInputStream(temp)) {
            l.loadDataAndUpdateState(DataFormat.RDFXML, stream, null);

            Equivalence.assertOGDatasetsAreSuperficiallyEqual(s.getOgDataset(), state.getOgDataset());
        }
    }

    @Test
    public void exportForceGraphMLError() throws IOException {
        State state = new State();

        GeneralServiceTestData.addGraphMLIncompatibleData(state.getOgDataset());
        state.reloadDerivativeData();

        ExportService service = new ExportService(state);
        Path temp = Files.createTempFile("tempGraphML",".xml");

        ExportService.ExportException exception = assertThrows(ExportService.ExportException.class,
                () -> service.exportData(DataFormat.GRAPHML, temp, null));
        String expectedMessage = "An exception occurred during export";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void exportNoSecondPathError() throws IOException {

        ExportService service = new ExportService(state);
        Path temp = Files.createTempFile("tempNodes",".csv");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.exportData(DataFormat.NEPTUNECSV, temp, null));
        String expectedMessage = "NeptuneCSV specified but no second path given";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
