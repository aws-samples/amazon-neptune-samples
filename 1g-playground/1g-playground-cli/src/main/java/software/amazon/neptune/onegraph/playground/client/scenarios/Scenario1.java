package software.amazon.neptune.onegraph.playground.client.scenarios;

import software.amazon.neptune.onegraph.playground.client.api.DataFormat;
import software.amazon.neptune.onegraph.playground.client.service.ScenarioService;
import org.jline.reader.LineReader;

/**
 * This scenario introduces the load, view, clear, and export commands.
 */
public class Scenario1 extends Scenario {

    private static final String BASE_PATH = "scenario1/";

    private static final String PATH_TO_NTRIPLES = BASE_PATH + "data/scenario1NTriples.nt";
    private static final String PATH_TO_NEPTUNECSV_NODES = BASE_PATH + "data/scenario1NeptuneCSV_nodes.csv";
    private static final String PATH_TO_NEPTUNECSV_EDGES = BASE_PATH + "data/scenario1NeptuneCSV_edges.csv";

    public Scenario1(LineReader reader, ScenarioService scenarioService) {
        super(reader, scenarioService);
    }

    @Override
    public void execute() throws Exception {

        printStep(1);
        printDataInFile(PATH_TO_NTRIPLES);
        askForContinueInput();

        printStep(2);
        printLoadCommand(DataFormat.NTRIPLES);
        askForRunCommandInput();

        printInBlue(performLoad(DataFormat.NTRIPLES, PATH_TO_NTRIPLES, null));

        printStep(3);
        printViewCommand(DataFormat.NTRIPLES);
        askForRunCommandInput();

        printInBlue(performView(DataFormat.NTRIPLES));
        askForContinueInput();

        printStep(4);
        printViewCommand(DataFormat.TURTLE);
        askForRunCommandInput();

        printInBlue(performView(DataFormat.TURTLE));
        askForContinueInput();

        printStep(5);
        printClearCommand();
        askForRunCommandInput();

        printInBlue(performClear());

        printStep(6);
        printDataInNeptuneCSVFile(PATH_TO_NEPTUNECSV_NODES, PATH_TO_NEPTUNECSV_EDGES);
        askForContinueInput();

        printStep(7);
        printLoadCommand(DataFormat.NEPTUNECSV);
        askForRunCommandInput();

        printInBlue(performLoad(DataFormat.NEPTUNECSV, PATH_TO_NEPTUNECSV_NODES, PATH_TO_NEPTUNECSV_EDGES));

        printStep(8);
        printViewCommand(DataFormat.NQUADS);
        askForRunCommandInput();

        printInBlue(performView(DataFormat.NQUADS));
        askForContinueInput();

        printStep(9);
        printExportCommand();
    }

    @Override
    protected String basePath() {
        return BASE_PATH;
    }
}
