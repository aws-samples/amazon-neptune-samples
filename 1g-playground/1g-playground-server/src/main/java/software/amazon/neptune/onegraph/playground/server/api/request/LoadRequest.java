package software.amazon.neptune.onegraph.playground.server.api.request;


/**
 * Request format of client for load command.
 */
public class LoadRequest {

    public LoadRequest() {}

    /**
     * Path to the file to load data from.
     */
    public String path1;

    /**
     * Path to the second file to load, this should only be applied if the {@link #dataFormat} is {@code NEPTUNECSV},
     * can be {@code null}.
     */
    public String path2;

    /**
     * The data format of the file to load.
     */
    public DataFormat dataFormat;
}
