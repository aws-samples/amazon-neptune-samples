package software.amazon.neptune.onegraph.playground.server.querying;

import java.util.List;

/**
 * Delegate used in {@link GremlinQuerying}.
 */
public interface GremlinQueryingDelegate {

    /**
     * Called when the Gremlin query executed successfully.
     * @param result The results of the query.
     */
    void gremlinQueryFinishedWithResult(List<?> result);

    /**
     * Called when the Gremlin query failed to execute.
     * @param exception The exception that occurred.
     */
    void gremlinQueryFailedWithException(QueryException exception);
}
