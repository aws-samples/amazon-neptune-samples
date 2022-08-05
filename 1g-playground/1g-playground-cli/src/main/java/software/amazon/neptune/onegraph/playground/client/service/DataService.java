package software.amazon.neptune.onegraph.playground.client.service;

import lombok.NonNull;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.client.api.DataFormat;
import software.amazon.neptune.onegraph.playground.client.api.API;
import software.amazon.neptune.onegraph.playground.client.request.LoadRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;

/**
 * Service for requests to the /data endpoint.
 * It makes a request to the endpoint using the {@link API} and
 * formats the response for the user.
 */
@Service
public class DataService {

    private final API api;

    /**
     * Creates a new instance of the data service.
     * @throws IOException When an exception occurs during creation.
     */
    public DataService() throws IOException {
        api = new API();
    }

    /**
     * Makes DELETE request to the {@code /data} endpoint to clear all data currently on the server.
     * @return A string containing information about the clear.
     * @throws ServiceException When an exception is encountered during the clear process.
     */
    public String clear() throws ServiceException {
        try {
            HttpURLConnection connection = api.setupDELETEConnection(api.endpoints.DATA_ENDPOINT);
            connection.connect();
            return api.serializeResponseToString(connection);
        } catch (IOException e) {
            throw new ServiceException("clear", e);
        }
    }

    /**
     * Makes GET request to the {@code /data/export} endpoint to export the data currently on the server to file.
     * @param dataFormat The data format to export the current data in.
     * @param path1 Path to file to export to.
     * @param path2 Second path to file to export to, only pass this when {@code dataFormat} is {@code NEPTUNECSV},
     *              the nodes of the {@code NEPTUNECSV} will be exported to {@code path1}, the edges to {@code path2}.
     * @throws IllegalArgumentException When {@code path2} is provided but {@code dataFormat} is not {@code NEPTUNECSV} or,
     * when a {@code path2} is not provided but the {@code dataFormat} is {@code NEPTUNECSV}.
     * @throws ServiceException When any other exception is encountered.
     * @return A string containing information about the export.
     */
    public String export(@NonNull DataFormat dataFormat,
                         @NonNull String path1,
                         String path2) throws IllegalArgumentException, ServiceException {
        this.validateExportParameters(dataFormat, path2);
        try {
            String url = String.format(api.endpoints.DATA_ENDPOINT + "/export?dataFormat=%s&path1=%s",
                    dataFormat, api.encode(path1));
            if (path2 != null) {
                url += String.format("&path2=%s", api.encode(path2));
            }
            HttpURLConnection connection = api.setupGETConnection(url);
            connection.connect();
            return api.serializeResponseToString(connection);
        } catch (IOException e) {
            throw new ServiceException("export", e);
        }
    }

    /**
     * Makes POST request to the {@code /data} endpoint to load new data on the server.
     * @param dataFormat The data format the file to load from is in.
     * @param stream1 Path to file to load from.
     * @param stream2 Second path to file to load from, only pass this when {@code dataFormat} is {@code NEPTUNECSV},
     *              this file should contain the edges, and {@code path1} the nodes.
     * @throws IllegalArgumentException When {@code path2} is provided but {@code dataFormat} is not {@code NEPTUNECSV} or,
     * when a {@code path2} is not provided but the {@code dataFormat} is {@code NEPTUNECSV}.
     * @throws ServiceException When any other exception is encountered.
     * @return A string containing information about the load.
     */
    public String load(@NonNull DataFormat dataFormat,
                       @NonNull InputStream stream1,
                       InputStream stream2) throws IllegalArgumentException, ServiceException {
        try {
            return api.setupMultiPartPOSTConnection(api.endpoints.DATA_ENDPOINT, stream1, stream2, dataFormat);
        } catch (Exception e) {
            throw new ServiceException("load", e);
        }
    }

    /**
     * Makes GET request to the {@code /data} endpoint to obtain the data currently on the server as a response.
     * @param dataFormat The data format the returned data should be in.
     * @return The serialized data currently on the server in the given {@code dataFormat}.
     * @throws ServiceException When any exception is encountered.
     */
    public String view(@NonNull DataFormat dataFormat) throws ServiceException  {
        try {
            String url = String.format(api.endpoints.DATA_ENDPOINT + "?dataFormat=%s", dataFormat.toString());

            HttpURLConnection connection = api.setupGETConnection(url);
            connection.connect();
            return api.serializeResponseToString(connection);
        } catch (IOException e) {
            throw new ServiceException("view", e);
        }
    }

    private void validateLoadParameters(@NonNull DataFormat dataFormat, String path2) throws IllegalArgumentException {
        if (dataFormat == DataFormat.FORMAL) {
            throw new IllegalArgumentException("Data can not be loaded in FORMAL format");
        }
        validateExportParameters(dataFormat, path2);
    }

    private void validateExportParameters(@NonNull DataFormat dataFormat, String path2) throws IllegalArgumentException {
        if (dataFormat == DataFormat.NEPTUNECSV && path2 == null) {
            throw new IllegalArgumentException("Incorrect amount of arguments: please pass 2 path arguments when format equal to NEPTUNECSV.");
        } else if (dataFormat != DataFormat.NEPTUNECSV && path2 != null && !path2.isEmpty()) {
            throw new IllegalArgumentException("Incorrect amount of arguments: please pass 1 path argument when format different from NEPTUNECSV.");
        }
    }
}
