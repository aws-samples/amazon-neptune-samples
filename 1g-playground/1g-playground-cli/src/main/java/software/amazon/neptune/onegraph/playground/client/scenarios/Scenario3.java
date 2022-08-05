package software.amazon.neptune.onegraph.playground.client.scenarios;

import org.jline.reader.LineReader;
import software.amazon.neptune.onegraph.playground.client.api.DataFormat;
import software.amazon.neptune.onegraph.playground.client.api.QueryType;
import software.amazon.neptune.onegraph.playground.client.service.ScenarioService;
import software.amazon.neptune.onegraph.playground.client.service.ServiceException;

import java.io.IOException;

/**
 * This scenario explains cross-model querying and the query command.
 */
public class Scenario3 extends Scenario {

    private static final String BASE_PATH = "/scenario3/";

    private static final String PATH_TO_TURTLE = BASE_PATH + "data/scenario3Turtle.ttl";
    private static final String PATH_TO_NEPTUNECSV_NODES = BASE_PATH + "data/scenario3NeptuneCSV_nodes.csv";
    private static final String PATH_TO_NEPTUNECSV_EDGES = BASE_PATH + "data/scenario3NeptuneCSV_edges.csv";

    private static final String PATH_TO_CONSTRUCT = BASE_PATH + "queries/sparql_construct.rq";
    private static final String PATH_TO_ASK = BASE_PATH + "queries/sparql_ask.rq";
    private static final String PATH_TO_SELECT = BASE_PATH + "queries/sparql_select.rq";
    private static final String PATH_TO_GREMLIN = BASE_PATH + "queries/gremlin.txt";

    public Scenario3(LineReader reader, ScenarioService scenarioService) {
        super(reader, scenarioService);
    }

    @Override
    protected void execute() throws Exception {
        printStep(1);
        printDataInFile(PATH_TO_TURTLE);

        printLoadCommand(DataFormat.TURTLE);
        askForRunCommandInput();

        printInBlue(performLoad(DataFormat.TURTLE, PATH_TO_TURTLE, null));

        printStep(2);
        askForContinueInput();

        printStep(3);
        printDataInFile(PATH_TO_SELECT);

        printQueryCommand(QueryType.SPARQL);
        askForRunCommandInput();

        printInBlue(performQuery(QueryType.SPARQL, loadStringFromFile(PATH_TO_SELECT)));
        askForContinueInput();

        printStep(4);

        printQueryCommand(QueryType.SPARQL, loadStringFromFile(PATH_TO_CONSTRUCT));
        askForRunCommandInput();

        printInBlue(performQuery(QueryType.SPARQL, loadStringFromFile(PATH_TO_CONSTRUCT)));
        askForContinueInput();

        printStep(5);

        printQueryCommand(QueryType.SPARQL, loadStringFromFile(PATH_TO_ASK));
        askForRunCommandInput();

        printInBlue(performQuery(QueryType.SPARQL, loadStringFromFile(PATH_TO_ASK)));
        askForContinueInput();

        printStep(6);

        printQueryCommand(QueryType.GREMLIN, loadStringFromFile(PATH_TO_GREMLIN));
        askForRunCommandInput();

        printInBlue(performQuery(QueryType.GREMLIN, loadStringFromFile(PATH_TO_GREMLIN)));
        askForContinueInput();

        printStep(7);
        printViewCommand(DataFormat.NEPTUNECSV);
        askForRunCommandInput();

        printInBlue(performView(DataFormat.NEPTUNECSV));
        askForContinueInput();

        printStep(8);
        printDataInNeptuneCSVFile(PATH_TO_NEPTUNECSV_NODES, PATH_TO_NEPTUNECSV_EDGES);

        printLoadCommand(DataFormat.NEPTUNECSV);
        askForRunCommandInput();

        printInBlue(performLoad(DataFormat.NEPTUNECSV, PATH_TO_NEPTUNECSV_NODES, PATH_TO_NEPTUNECSV_EDGES));
        askForContinueInput();

        printStep(9);
        printDataInFile(PATH_TO_SELECT);

        printQueryCommand(QueryType.SPARQL);
        askForRunCommandInput();

        printInBlue(performQuery(QueryType.SPARQL, loadStringFromFile(PATH_TO_SELECT)));
        askForContinueInput();
    }

    @Override
    protected String basePath() {
        return BASE_PATH;
    }

    private void printQueryCommand(QueryType type, String query) throws IOException {
        String command;
        if (type == QueryType.GREMLIN) {
            command = String.format("query -gremlin \"%s\"", query);
        } else {
            command = String.format("query -sparql \"%s\"", query);
        }
        printEmptyLine();
        printInBold(command);
        printEmptyLine();
    }

    private void printQueryCommand(QueryType type) throws IOException {
        String command;
        if (type == QueryType.GREMLIN) {
            command = "query -gremlin -f <path_to_query>";
        } else {
            command = "query -sparql -f <path_to_query>";
        }
        printEmptyLine();
        printInBold(command);
        printEmptyLine();
    }

    private String performQuery(QueryType type, String query) throws  IllegalArgumentException, ServiceException {
        return this.scenarioService.queryService.query(type, false, query);
    }
}
