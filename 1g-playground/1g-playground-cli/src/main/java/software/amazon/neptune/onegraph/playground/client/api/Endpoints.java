package software.amazon.neptune.onegraph.playground.client.api;

import software.amazon.neptune.onegraph.playground.client.service.ScenarioService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Contains endpoints for the tool, loads from resources/endpoints.properties
 */
public class Endpoints {

    /**
     * The base url to make requests to.
     */
    public final String BASE_URL;

    /**
     * Endpoint for the info command.
     */
    public final String INFO_ENDPOINT;

    /**
     * Endpoint for the settings command.
     */
    public final String SETTINGS_ENDPOINT;

    /**
     * Endpoint for the data command.
     */
    public final String DATA_ENDPOINT;

    /**
     * Endpoint for the export command.
     */
    public final String EXPORT_ENDPOINT;


    /**
     * SPARQL endpoint for the query command.
     */
    public final String QUERY_SPARQL_ENDPOINT;

    /**
     * Gremlin endpoint for the query command.
     */
    public final String QUERY_GREMLIN_ENDPOINT;

    private static Endpoints instance = null;

    private final String endpointsPath = "endpoints.properties";

    private final String BASE_URL_KEY = "baseURL";
    private final String INFO_ENDPOINT_KEY = "infoEndpoint";
    private final String SETTINGS_ENDPOINT_KEY = "settingsEndpoint";
    private final String DATA_ENDPOINT_KEY = "dataEndpoint";
    private final String EXPORT_ENDPOINT_KEY = "exportEndpoint";
    private final String QUERY_SPARQL_ENDPOINT_KEY = "sparqlEndpoint";
    private final String QUERY_GREMLIN_ENDPOINT_KEY = "gremlinEndpoint";

    protected Endpoints() throws IOException {
        Properties prop = new Properties();
        ClassLoader classLoader = Endpoints.class.getClassLoader();
        try (InputStream s = classLoader.getResourceAsStream(endpointsPath)) {
            prop.load(s);
        }

        BASE_URL = prop.getProperty(BASE_URL_KEY);
        INFO_ENDPOINT = BASE_URL + prop.getProperty(INFO_ENDPOINT_KEY);
        SETTINGS_ENDPOINT = BASE_URL + prop.getProperty(SETTINGS_ENDPOINT_KEY);
        DATA_ENDPOINT = BASE_URL + prop.getProperty(DATA_ENDPOINT_KEY);
        EXPORT_ENDPOINT = BASE_URL + prop.getProperty(EXPORT_ENDPOINT_KEY);
        QUERY_SPARQL_ENDPOINT = BASE_URL + prop.getProperty(QUERY_SPARQL_ENDPOINT_KEY);
        QUERY_GREMLIN_ENDPOINT = BASE_URL + prop.getProperty(QUERY_GREMLIN_ENDPOINT_KEY);
    }

    /**
     * Access the shared endpoints.
     * @return Singleton object that contains the endpoints for the playground.
     * @throws IOException When an exception occurs during the read of the enpoints from file.
     */
    public static Endpoints shared() throws IOException {
        if (instance == null) {
            instance = new Endpoints();
        }
        return instance;
    }

    private String getPathToResource(String relativePath) throws IOException {
        URL resource = ScenarioService.class.getResource(relativePath);
        if (resource == null) {
            throw new IOException("The relative path " + relativePath + "could not be resolved.");
        }
        return resource.getPath();
    }
}
