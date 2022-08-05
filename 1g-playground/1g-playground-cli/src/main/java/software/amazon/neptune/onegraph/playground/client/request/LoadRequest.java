package software.amazon.neptune.onegraph.playground.client.request;

import lombok.NonNull;
import software.amazon.neptune.onegraph.playground.client.api.DataFormat;

/**
 * Expected content of the POST to /data.
 */
public class LoadRequest extends Request {

    /**
     * Path to the file to load.
     */
    public String path1;

    /**
     * Path to the second file to load, this should only be set if the {@link #dataFormat} is {@code NEPTUNECSV}.
     */
    public String path2;

    /**
     * The data format of the file to load.
     */
    public DataFormat dataFormat;

    /**
     * Creates a new load request with the given parameters.
     * @param dataFormat Used to set the {@link LoadRequest#dataFormat}.
     * @param path1 Used to set the {@link LoadRequest#path1}.
     * @param path2 Used to set the {@link LoadRequest#path2, can be{@code null}.
     */
    public LoadRequest(@NonNull DataFormat dataFormat,
                       @NonNull String path1,
                       String path2) {
        this.dataFormat = dataFormat;
        this.path1 = path1;
        if (path2 != null && path2.length() != 0) {
            this.path2 = path2;
        }
    }
}
