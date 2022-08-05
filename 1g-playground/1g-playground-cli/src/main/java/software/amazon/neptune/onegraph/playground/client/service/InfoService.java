package software.amazon.neptune.onegraph.playground.client.service;

import software.amazon.neptune.onegraph.playground.client.api.API;
import software.amazon.neptune.onegraph.playground.client.response.InfoResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Scanner;

/**
 * Service for requests to the /info endpoint.
 * It makes a request to the endpoint using the {@link API} and
 * formats the response for the user.
 */
@Service
public class InfoService {

    private final API api;

    private final static String check = "\u001b[32m\u2713\u001b[0m";
    private final static String cross = "\u001b[31m\u2717\u001b[0m";

    private final static String squiggly = "\u001b[33m\u2307\u001b[0m";

    /**
     * Creates a new instance of the info service.
     * @throws IOException When an exception occurs during creation.
     */
    public InfoService() throws IOException {
        api = new API();
    }

    /**
     * Makes GET request to the {@code /info} endpoint to obtain info about data currently stored on the server.
     * @return The response from the server formatted for the user.
     * @throws ServiceException When any exception is encountered.
     */
    public String info() throws ServiceException {
        try {
            HttpURLConnection connection = api.setupGETConnection(api.endpoints.INFO_ENDPOINT);
            connection.connect();
            String responseString = api.serializeResponseToString(connection);
            InfoResponse response = api.mapper.readValue(responseString, InfoResponse.class);
            return formatInfoResponse(response);
        } catch (IOException e) {
            throw new ServiceException("info", e);
        }
    }

    private String formatInfoResponse(InfoResponse response) {
        StringBuilder builder = new StringBuilder();
        builder.append("SHOWING INFO FOR CURRENT DATASET\n\n");
        appendLegend(builder);
        appendRDFCompatibility(response, builder);
        appendLPGCompatability(response, builder);
        builder.append("\n");
        builder.append("[RDF|LPG]\n");

        int lineCount = 0;
        Scanner scanner = new Scanner(response.serializedOG);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            appendStatementCompatability(response, builder, lineCount);
            builder.append(" ").append(line).append("\n");
            lineCount++;
        }
        scanner.close();

        return builder.toString();
    }

    private static void appendLegend(StringBuilder builder) {
        builder.append("LEGEND:\n");
        builder.append(check + " Indicates that a statement is RDF/LPG compatible\n");
        builder.append(cross + " Indicates that a statement is RDF/LPG incompatible\n");
        builder.append(squiggly + " Indicates that a statement is RDF pairwise incompatible\n\n");
    }

    private static void appendStatementCompatability(InfoResponse response, StringBuilder builder, int lineCount) {
        builder.append("[");
        if (response.rdfNonCompatibleLines.contains(lineCount)) {
            builder.append(" " + cross + " ");
        } else if (response.pairwiseNonCompatibleLines.contains(lineCount)) {
            builder.append(" " + squiggly + " ");
        } else {
            builder.append(" " + check + " ");
        }
        builder.append("|");
        if (response.lpgNonCompatibleLines.contains(lineCount)) {
            builder.append(" " + cross + " ");
        } else {
            builder.append(" " + check + " ");
        }
        builder.append("]");
    }

    private static void appendRDFCompatibility(InfoResponse response, StringBuilder builder) {
        if (response.rdfNonCompatibleLines.isEmpty() &&
                response.pairwiseNonCompatibleLines.isEmpty()) {
            builder.append("- This dataset is fully RDF compatible\n");
        } else {
            builder.append("- This dataset is not fully RDF compatible\n");
        }

    }

    private static void appendLPGCompatability(InfoResponse response, StringBuilder builder) {
        if (response.lpgNonCompatibleLines.isEmpty()) {
            builder.append("- This dataset is fully LPG compatible\n");
        } else {
            builder.append("- This dataset is not fully LPG compatible\n");
        }
    }
}
