package software.amazon.neptune.onegraph.playground.client.service;

import lombok.NonNull;
import org.springframework.shell.table.ArrayTableModel;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.TableBuilder;
import software.amazon.neptune.onegraph.playground.client.api.API;
import software.amazon.neptune.onegraph.playground.client.api.QueryType;
import software.amazon.neptune.onegraph.playground.client.response.QueryResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Service for requests to the /query endpoint.
 * It makes a request to the endpoint using the {@link API} and
 * formats the response for the user.
 */
@Service
public class QueryService {

    private final API api;

    /**
     * Creates a new instance of the query service.
     * @throws IOException When an exception occurs during creation.
     */
    public QueryService() throws IOException {
        api = new API();
    }

    /**
     * Queries the data currently stored on the server.
     * @param type The type of query
     * @param input The actual query.
     * @return The result of the query formatted as a string, for direct output to the user.
     * @throws ServiceException When an exception occurred during the query operation.
     */
    public String query(@NonNull QueryType type, boolean file, @NonNull String input) throws ServiceException {

        String query = input;
        if (file) {
            try {
                query = loadQueryFromFile(input);
            } catch (IOException e) {
                throw new ServiceException("query", e);
            }
        }

        try {
            String url;
            if (type == QueryType.GREMLIN) {
                url = String.format(api.endpoints.QUERY_GREMLIN_ENDPOINT + "?query=%s", api.encode(query));
            } else {
                url = String.format(api.endpoints.QUERY_SPARQL_ENDPOINT + "?query=%s", api.encode(query));
            }
            HttpURLConnection connection = api.setupGETConnection(url);
            connection.connect();
            String responseString = api.serializeResponseToString(connection);
            QueryResponse response = api.mapper.readValue(responseString, QueryResponse.class);
            if (type == QueryType.GREMLIN) {
                return formatGremlinResponse(response);
            } else {
                return formatSPARQLResponse(response);
            }
        } catch (Exception e) {
            throw new ServiceException("query", e);
        }
    }

    private static String loadQueryFromFile(@NonNull String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String (bytes);
    }

    private static String formatGremlinResponse(@NonNull QueryResponse response) {
        StringBuilder builder = new StringBuilder();
        for (String value : response.traversalResult) {
            builder.append(value).append("\n");
        }
        return builder.toString();
    }

    private static String formatSPARQLResponse(@NonNull QueryResponse response) {

        switch (response.type) {
            case SPARQLGRAPH:
                return formatMapAsTable(response.graphResult);
            case SPARQLTUPLE:
                return formatMapAsTable(response.tupleResult);
            case SPARQLBOOLEAN:
            default:
                return response.booleanResult.toString();
        }
    }

    // Makes sure the column headers starting with S, P, O and G get first in the list, and the rest is alphabetical.
    private static void sortSPOGFirst(List<String> headers) {
        final String SPOGFIRST = "spogabcdefghijklnmopqrstuvwxyz";
        headers.sort(Comparator.comparingInt(o -> SPOGFIRST.indexOf(o.toLowerCase())));
    }

    private static String formatMapAsTable(Map<String, List<String>> map) {

        if (map.size() == 0) {
            return "No data.";
        }

        List<String> headers = new ArrayList<>(map.keySet());
        sortSPOGFirst(headers);

        List<List<String>> table = new ArrayList<>();
        table.add(headers);
        int[] columnWidths = new int[headers.size()];

        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            columnWidths[i] = Math.max(columnWidths[i], header.length());

            for (int j = 0; j < map.get(header).size(); j++) {
                String value = map.get(header).get(j);
                if (table.size() > j + 1) {
                    table.get(j + 1).add(value);
                } else {
                    table.add(new ArrayList<>(Collections.singletonList(value)));
                }
                columnWidths[i] = Math.max(columnWidths[i], value.length());
            }
        }
        TableBuilder tableBuilder = new TableBuilder(new ArrayTableModel(nestedListToArray(table)));
        tableBuilder.addFullBorder(BorderStyle.fancy_light);
        return "\n" + tableBuilder.build().render(0);
    }

    private static String[][] nestedListToArray(List<List<String>> nestedLists) {
        String[][] arrayResult = new String[nestedLists.size()][];

        for(int i=0; i < nestedLists.size(); i++) {
            arrayResult[i] = nestedLists.get(i).toArray(new String[0]);
        }
        return arrayResult;
    }
}
