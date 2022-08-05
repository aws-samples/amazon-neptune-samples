package software.amazon.neptune.onegraph.playground.client.scenarios;

import org.jline.reader.LineReader;
import software.amazon.neptune.onegraph.playground.client.api.DataFormat;
import software.amazon.neptune.onegraph.playground.client.service.ScenarioService;

/**
 * This scenario gives an introduction to the OneGraph data format and explains the
 * {@code formal} and {@code og} parameters.
 */
public class Scenario2 extends Scenario {

    private static final String BASE_PATH = "/scenario2/";
    private static final String PATH_TO_TURTLE = BASE_PATH + "data/scenario2Turtle.ttl";
    private static final String PATH_TO_NEPTUNECSV_NODES = BASE_PATH + "data/scenario2NeptuneCSV_nodes.csv";
    private static final String PATH_TO_NEPTUNECSV_EDGES = BASE_PATH + "data/scenario2NeptuneCSV_edges.csv";

    public Scenario2(LineReader reader, ScenarioService scenarioService) {
        super(reader, scenarioService);
    }

    @Override
    protected void execute() throws Exception {

        printStep(1);
        askForContinueInput();

        printStep(2);
        askForContinueInput();

        printStep(3);
        askForContinueInput();

        printStep(4);
        printDataInFile(PATH_TO_TURTLE);
        askForContinueInput();

        printStep(5);
        printLoadCommand(DataFormat.TURTLE);
        askForRunCommandInput();

        printInBlue(performLoad(DataFormat.TURTLE, PATH_TO_TURTLE, null));

        printStep(6);
        printViewCommand(DataFormat.FORMAL);
        askForRunCommandInput();

        printInBlue(performView(DataFormat.FORMAL));
        askForContinueInput();

        printStep(7);
        printEmptyLine();
        printDataInNeptuneCSVFile(PATH_TO_NEPTUNECSV_NODES, PATH_TO_NEPTUNECSV_EDGES);

        printLoadCommand(DataFormat.NEPTUNECSV);
        askForRunCommandInput();

        printStep(8);
        printViewCommand(DataFormat.FORMAL);
        askForRunCommandInput();

        printInBlue(performView(DataFormat.FORMAL));
        askForContinueInput();

        printStep(9);
        printViewCommand(DataFormat.OG);
        askForRunCommandInput();

        printInBlue(performView(DataFormat.OG));
        askForContinueInput();

        printStep(10);
        askForContinueInput();
    }

    @Override
    protected String basePath() {
        return BASE_PATH;
    }
}
