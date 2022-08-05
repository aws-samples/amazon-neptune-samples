package software.amazon.neptune.onegraph.playground.client.response;

import java.util.List;
import java.util.Map;

/**
 * Response format for GET requests to /query.
 */
public class QueryResponse {

    public QueryResponse() {}

    /**
     * What type of query response this is.
     */
    public QueryResponseType type;

    /**
     * Set when the request was a SPARQL ASK query, else {@code null}.
     */
    public Boolean booleanResult;

    /**
     * Set when the request was a SPARQL SELECT query, else {@code null}.
     * The result of a SELECT query can be viewed as a table.
     * The keys of this map are the columns of this table.
     * The value of this map are the rows of a column.
     */
    public Map<String, List<String>> tupleResult;

    /**
     * Set when the request was a SPARQL CONSTRUCT query, else {@code null}.
     * A CONSTRUCT query returns an RDF model (List of statements) <br />
     * This map always contains only the following keys: <br />
     * s, p, o, and g.  <br />
     * s contains all subjects <br />
     * p contains all predicates <br />
     * o contains all objects <br />
     * g contains all graphs
     */
    public Map<String, List<String>> graphResult;

    /**
     * Set when the request was a Gremlin query, else {@code null}.
     * Contains the results of the Gremlin traversal in a list.
     */
    public List<String> traversalResult;
}
