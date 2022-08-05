package software.amazon.neptune.onegraph.playground.client.api;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import software.amazon.neptune.onegraph.playground.client.request.Request;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Provides HTTP functionality.
 */
public class API {

    /**
     * Http methods used in the client.
     */
    public interface HttpMethod {
        String GET = "GET";
        String POST = "POST";
        String DELETE = "DELETE";
    }

    /**
     * Http request property keys used in the client.
     */
    public interface HttpRequestPropertyKey {
        String contentType = "Content-Type";
        String accept = "Accept";
    }

    /**
     * Http request property values used in the client.
     */
    public interface HttpRequestPropertyValue {
        String json = "application/json";
    }

    public final HttpClient client;

    /**
     * The endpoints for the playground.
     */
    public final Endpoints endpoints;


    /**
     * JSON translator.
     */
    public final ObjectMapper mapper = new ObjectMapper();

    /**
     * Create a new API.
     * @throws IOException When an exception occurred while reading the endpoints.
     */
    public API() throws IOException {
        endpoints = Endpoints.shared();
        client = HttpClients.createDefault();
    }

    /**
     * Creates a new POST connection for the given {@code url}.
     * @param url The url to POST to.
     * @return An url connection.
     * @throws IOException When an error occurs during the connection process.
     */
    public HttpURLConnection setupPOSTConnection(String url) throws IOException {
        HttpURLConnection con = setupHttpConnection(url, HttpMethod.POST);
        con.setRequestProperty(HttpRequestPropertyKey.contentType, HttpRequestPropertyValue.json);
        con.setRequestProperty(HttpRequestPropertyKey.accept, HttpRequestPropertyValue.json);
        return con;
    }

    public String setupMultiPartPOSTConnection(String url,
                                               InputStream data1,
                                               InputStream data2,
                                               DataFormat format) throws IOException, URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.setParameter("format", format.toString());

        HttpPost post = new HttpPost(uriBuilder.build());
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("data", new InputStreamBody(data1, "name"));
        if (data2 != null) {
            builder.addPart("additionalData", new InputStreamBody(data2, "name2"));
        }
        post.setEntity(builder.build());
        HttpResponse response = client.execute(post);
        HttpEntity result = response.getEntity();
        return EntityUtils.toString(result, "UTF-8");
    }

    /**
     * Creates a new POST connection for the given {@code url}, setting the body of the request to {@code req}.
     * @param url The url to POST to.
     * @param req The request to put in the body.
     * @return An url connection.
     * @throws IOException When an error occurs during the connection process.
     */
    public HttpURLConnection setupPOSTConnection(String url, Request req) throws IOException {
        HttpURLConnection con = setupPOSTConnection(url);
        String requestString = mapper.writeValueAsString(req);
        try(OutputStream os = con.getOutputStream()) {
            byte[] input = requestString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return con;
    }

    /**
     * Creates a new GET connection for the given {@code url}.
     * @param url The url to GET from.
     * @return An url connection.
     * @throws IOException When an error occurs during the connection process.
     */
    public HttpURLConnection setupGETConnection(String url) throws IOException {
        return setupHttpConnection(url, HttpMethod.GET);
    }

    /**
     * Creates a new DELETE connection for the given {@code url}.
     * @param url The url to post the DELETE request to.
     * @return An url connection.
     * @throws IOException When an error occurs during the connection process.
     */
    public HttpURLConnection setupDELETEConnection(String url) throws IOException {
        return setupHttpConnection(url, HttpMethod.DELETE);
    }

    /**
     * Sets up generic Http Connection
     * @param url The url to set up the connection for.
     * @param method A http method keyword such as DELETE or POST.
     * @return The http connection to the given {@code url}.
     * @throws IOException When an error occurs during the connection process.
     */
    public HttpURLConnection setupHttpConnection(String url, String method) throws IOException {
        URL baseURL = new URL(url);
        HttpURLConnection con = (HttpURLConnection) baseURL.openConnection();
        con.setRequestMethod(method);
        con.setDoOutput(true);
        return con;
    }

    /**
     * Transforms the response from the given connection to a string.
     * @param con The connection to transform the response from.
     * @return The serialized response.
     * @throws IOException When an error occurs during the conversion process,
     * or when there was a non-200 status code returned from the connection.
     */
    public String serializeResponseToString(HttpURLConnection con) throws IOException {
        int code = con.getResponseCode();
        if (200 <= code && code < 300) {
            return this.serializeStream(con.getInputStream());
        } else {
            String errorBody = this.serializeStream(con.getErrorStream());
            throw new IOException(errorBody);
        }
    }

    /**
     * Creates string from input stream.
     * @param s Stream to read from.
     * @return The string created from the stream.
     * @throws IOException When an exception is encountered during the process.
     */
    public String serializeStream(InputStream s) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(s, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
                response.append("\n");
            }
            return response.toString();
        }
    }

    /**
     * URL encodes argument.
     * @param toEncode Argument to encode.
     * @return Encoded argument
     * @throws UnsupportedEncodingException when an error occurs during encoding.
     */
    public String encode(String toEncode) throws UnsupportedEncodingException {
        return URLEncoder.encode(toEncode, String.valueOf(StandardCharsets.UTF_8));
    }
}
