package software.amazon.neptune.onegraph.playground.server.querying;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;

import java.util.List;
import java.util.Map;

/**
 * Delegate used in {@link SPARQLQuerying}.
 */
public interface SPARQLQueryingDelegate {

    /**
     * Called when the SPARQL query finished with a boolean result.
     * @param result The boolean result of the query.
     */
    void SPARQLQueryFinishedWithResult(boolean result);

    /**
     * Called when the SPARQL query finished with an RDF {@link Model} result.
     * @param result The {@link Model} result of the query.
     */
    void SPARQLQueryFinishedWithResult(Model result);

    /**
     * Called when the SPARQL query finished with a key-value result.
     * @param result The key-value result of the query.
     */
    void SPARQLQueryFinishedWithResult(Map<String, List<Value>> result);

    /**
     * Called when the SPARQL query failed to execute.
     * @param exception The exception that occurred.
     */
    void SPARQLQueryFailedWithException(QueryException exception);
}
