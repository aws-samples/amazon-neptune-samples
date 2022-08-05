package software.amazon.neptune.onegraph.playground.server.api.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Response format of client for query command.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResponse {

    public QueryResponse() {}

    /**
     * Type of query response, see {@link QueryResponseType} for an explanation.
     */
    public QueryResponseType type;

    /**
     * The boolean result if {@link #type} is {@code SPARQLBOOLEAN} else {@code null}.
     */
    public Boolean booleanResult;

    /**
     * The tuple result if {@link #type} is {@code SPARLQTUPLE}, else {@code null}.
     */
    public Map<String, List<String>> tupleResult;

    /**
     * The graph result if {@link #type} is {@code SPARQLGRAPH}, else {@code null}.
     */
    public Map<String, List<String>> graphResult;

    /**
     * The traversal result if {@link #type} is {@code GREMLINTRAVERSAL}, else {@code null}.
     */
    public List<String> traversalResult;
}
