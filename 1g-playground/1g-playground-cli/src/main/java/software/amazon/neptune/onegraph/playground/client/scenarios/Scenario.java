package software.amazon.neptune.onegraph.playground.client.scenarios;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import lombok.NonNull;
import software.amazon.neptune.onegraph.playground.client.api.DataFormat;
import software.amazon.neptune.onegraph.playground.client.service.DataService;
import software.amazon.neptune.onegraph.playground.client.service.ScenarioService;
import org.jline.reader.LineReader;
import org.apache.commons.lang.StringEscapeUtils;
import software.amazon.neptune.onegraph.playground.client.service.ServiceException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Superclass of all scenarios, contains common functionality
 * used across all scenarios.
 */
public abstract class Scenario {


    protected final static String H_LINE = "-----------------";
    protected final static String H_LINE_SMALL = "----";
    private final static String CLEAR_MESSAGE = "All current data was cleared.";

    // The steps of the scenarios are in separate files that are named step1.txt etc.
    private static final String STEP_FILE_NAME = "step%d.txt";
    // Every scenario should have a welcome.txt
    private static final String WELCOME_FILE_NAME = "welcome.txt";
    // Every scenario should have a conclusion.txt
    private static final String CONCLUSION_FILE_NAME =  "conclusion.txt";
    // Folder name where the steps are.
    private static final String STEPS_FOLDER = "steps/";


    public static class ScenarioException extends RuntimeException {
        ScenarioException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // To read key input from the CLI.
    public final LineReader reader;

    // The scenario service holds other services that can make requests
    // to the server.
    public final ScenarioService scenarioService;

    /**
     * Create a new Scenario with the given reader and scenario service.
     * @param reader The line reader, used to read key input.
     * @param scenarioService The scenario service, used to make request to the server.
     */
    public Scenario(@NonNull LineReader reader, @NonNull  ScenarioService scenarioService) {
        this.reader = reader;
        this.scenarioService = scenarioService;
    }

    /**
     * Run this scenario.
     * @throws ScenarioException When an exception is encountered during the
     * execution of the scenario.
     */
    public void run() throws ScenarioException {
        try {
            // Always clear the current data when running a new scenario.
            this.scenarioService.dataService.clear();
            printInBlue(CLEAR_MESSAGE);
            printEmptyLine();

            // Print the welcome message.
            printStringAtPathUnescaped(pathToWelcome());

            // Ask the user for input to continue.
            askForContinueInput();

            // Execute the steps in the concrete subclasses.
            execute();

            // Print the conclusion and ask the user for input to end.
            printStringAtPathUnescaped(pathToConclusion());
            printEmptyLine();
            askForEndInput();
        } catch (Exception e) {
            throw new ScenarioException(String.format("Running scenario failed, message: %s cause: %s", e.getMessage(), e.getCause()), e);
        }
    }

    /**
     * This method should contain the core functionality of the scenario.
     * @throws Exception When an exception is encountered during the execution
     * of the scenario.
     */
    protected abstract void execute() throws Exception;

    /**
     * Overridden by subclass to provide the base path of the scenario.
     * @return The base path of the concrete scenario.
     */
    protected abstract String basePath();

    /**
     * Gives relative path to the conclusion text.
     * @return A relative path to the conclusion text.
     */
    private String pathToConclusion() {
        return basePath() + CONCLUSION_FILE_NAME;
    }

    /**
     * Gives relative path to the welcome text.
     * @return A relative path to the welcome text.
     */
    private String pathToWelcome() {
        return basePath() + WELCOME_FILE_NAME;
    }

    /**
     * Gives relative path to the steps folder.
     * @return A relative path to the steps folder.
     */
    private String pathToStepsFolder() {
        return basePath() + STEPS_FOLDER;
    }


    /**
     * Prints the string contained in a "step" file to the console.
     * @param stepNumber The number of the step file to print, step files should be named step1.txt etc.
     * @throws IOException When an exception is encountered during the loading of the file.
     */
    protected void printStep(int stepNumber) throws IOException {
        String pathToStep = pathToStepsFolder() + String.format(STEP_FILE_NAME, stepNumber);
        printStringAtPathUnescaped(pathToStep);
    }

    /**
     * Prints the string from the file at {@code pathToFile} to the console, without escaping.
     * @param pathToFile The path pointing to the file to print.
     * @throws IOException When an exception is encountered during the loading of the file.
     */
    protected void printStringAtPathUnescaped(String pathToFile) throws IOException {
        printUnescaped(loadStringFromFile(pathToFile));
    }

    /**
     * Performs the load operation, using the {@link DataService}.
     * @param format The data format the file to load from is in.
     * @param pathToFile Path to file to load from.
     * @param pathToFile2 Second path to file to load from, only pass this when dataFormat is NEPTUNECSV,
     *                   this file should contain the edges, and pathToFile the nodes.
     * @return A string containing information about the load.
     * @throws IllegalArgumentException When path2 is provided but dataFormat is not NEPTUNECSV or,
     * when a path2 is not provided but the dataFormat is NEPTUNECSV
     * @throws IOException When there is exception during the resolving of {@code pathToFile} or {@code pathToFile2}.
     * @throws ServiceException When any other exception is encountered.
     */
    protected String performLoad(@NonNull DataFormat format, @NonNull String pathToFile, String pathToFile2) throws IllegalArgumentException, IOException, ServiceException {
        ClassLoader classLoader = Scenario.class.getClassLoader();
        try (InputStream stream1 = classLoader.getResourceAsStream(pathToFile)) {
            if (pathToFile2 != null) {
                try (InputStream stream2 = classLoader.getResourceAsStream(pathToFile2)) {
                    return this.scenarioService.dataService.load(format, stream1, stream2);
                }
            }
            return this.scenarioService.dataService.load(format, stream1, null);
        }
    }

    /**
     * Performs the view operation, using the {@link DataService}.
     * @param format The data format the returned data should be in.
     * @throws ServiceException When any exception is encountered.
     */
    protected String performView(@NonNull DataFormat format) throws ServiceException {
        return this.scenarioService.dataService.view(format);
    }

    /**
     * Performs the clear-data operation, using the {@link DataService}.
     * @throws ServiceException When any exception is encountered.
     */
    protected String performClear() {
        return this.scenarioService.dataService.clear();
    }

    /**
     * Asks the user to press {@code RETURN}. Waits for the user to press any key. Then returns.
     */
    protected void askForContinueInput() {
        printEmptyLine();
        this.reader.readLine("Press \033[7mRETURN\033[0m to continue.");
        printEmptyLine();
    }

    /**
     * Asks the user to press {@code RETURN}. Waits for the user to press any key. Then returns.
     */
    protected void askForRunCommandInput() {
        this.reader.readLine("Press \033[7mRETURN\033[0m to run this command.");
        printEmptyLine();
    }

    /**
     * Asks the user to press {@code RETURN}. Waits for the user to press any key. Then returns.
     */
    protected void askForEndInput() {
        this.reader.readLine("Press \033[7mRETURN\033[0m to end the scenario.");
    }

    /**
     * Returns the string contained in the file at {@code relativePath}.
     * @param relativePath The path relative to /resources pointing to the file.
     * @return The string contained in the file at the {@code relativePath}.
     * @throws IOException When an exception is encountered during the loading of the file.
     */
    protected String loadStringFromFile(String relativePath) throws IOException {
        ClassLoader classLoader = Scenario.class.getClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(relativePath)) {
            if (stream == null) {
                throw new IOException(String.format("Could not make input stream from %s", relativePath));
            }
            return CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8));
        }
    }

    // Methods for printing to the console.

    /**
     * Prints the given string to the console in bold.
     * @param toPrint The string to print to the console in bold.
     */
    protected void printInBold(String toPrint) {
        System.out.println("\u001b[1m" + toPrint + "\u001b[0m");
    }

    /**
     * Prints the given string to the console in bold.
     * @param toPrint The string to print to the console in bold.
     */
    protected void printInBlue(String toPrint) {
        System.out.printf("\u001B[34m%s\u001B[0m%n", toPrint);
    }

    /**
     * Prints the text "load -t {@code format} path_to_file" to the console.
     * @param format The format the put in the load argument.
     */
    protected void printLoadCommand(DataFormat format) {
        String formatString = format.getUnderlying().toLowerCase();
        String command;
        if (format == DataFormat.NEPTUNECSV) {
            command = String.format("load -t %s <path_to_nodes_file> <path_to_edges_file>", formatString);
        } else {
            command = String.format("load -t %s <path_to_file>", formatString);
        }
        printEmptyLine();
        printInBold(command);
        printEmptyLine();
    }

    /**
     * Prints an explanation of the export command to the console.
     */
    protected void printExportCommand() {
        printEmptyLine();
        printInBold("export -t <format> <path_to_file>");
        printEmptyLine();
        print("or");
        printEmptyLine();
        printInBold("export -t neptunecsv <path_to_nodes_file> <path_to_edges_file>");
        printEmptyLine();
    }

    /**
     * Prints the text "load -{@code format} path_to_file" to the console.
     * @param format The format the put in the load argument.
     */
    protected void printViewCommand(DataFormat format) {
        String formatString = format.getUnderlying().toLowerCase();
        String command = String.format("view -t %s", formatString);
        printEmptyLine();
        printInBold(command);
        printEmptyLine();
    }

    /**
     * Prints the text clear-data to the console.
     */
    protected void printClearCommand() {
        printInBold("clear-data");
    }

    /**
     * Prints given string to the console.
     * @param toPrint The string to print.
     */
    protected void print(String toPrint) {
        System.out.println(toPrint);
    }

    /**
     * Prints the given string to the console, without unescaping characters.
     * @param toPrint The string to print.
     */
    protected void printUnescaped(String toPrint) {
        print(StringEscapeUtils.unescapeJava(toPrint));
    }

    /**
     * Prints a file containing data (RDF or LPG data) to the console, with proper formatting and escaping.
     * @param relativePath The path relative to /resources pointing to the file.
     * @throws IOException When an exception is encountered during the loading of the file.
     */
    protected void printDataInFile(String relativePath) throws IOException {
        print(H_LINE);
        print(loadStringFromFile(relativePath));
        print(H_LINE);
    }

    /**
     * Prints NeptuneCSV data to the console.
     * @param nodesPath The path pointing to the file containing the nodes.
     * @param edgesPath The path pointing to the file containing the edges.
     * @throws IOException When an exception is encountered during the loading one of the files.
     */
    protected void printDataInNeptuneCSVFile(String nodesPath, String edgesPath) throws IOException {
        printNodesInNeptuneCSVFile(nodesPath);
        printEdgesInNeptuneCSVFile(edgesPath);
    }

    /**
     * Prints the nodes of NeptuneCSV data to the console.
     * @param nodesPath The path pointing to the file containing the nodes.
     * @throws IOException When an exception is encountered during the loading of the file.
     */
    protected void printNodesInNeptuneCSVFile(String nodesPath) throws IOException {
        print(H_LINE_SMALL + " Nodes " + H_LINE_SMALL);
        print(loadStringFromFile(nodesPath));
    }

    /**
     * Prints the edges of NeptuneCSV data to the console.
     * @param edgesPath The path pointing to the file containing the edges.
     * @throws IOException When an exception is encountered during the loading of the file.
     */
    protected void printEdgesInNeptuneCSVFile(String edgesPath) throws IOException {
        print(H_LINE_SMALL + " Edges " + H_LINE_SMALL);
        print(loadStringFromFile(edgesPath));
    }

    /**
     * Prints a newline to the console.
     */
    protected void printEmptyLine() {
        print("");
    }
}
