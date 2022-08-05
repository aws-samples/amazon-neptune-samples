package software.amazon.neptune.onegraph.playground.client.response;

/**
 * Specifies the type of the {@link QueryResponse}.
 */
public enum QueryResponseType {

    /**
     * The query response was of a Gremlin traversal.
     */
    GREMLINTRAVERSAL,

    /**
     * The query response was of a SPARQL CONSTRUCT query.
     */
    SPARQLGRAPH,

    /**
     * The query response was of a SPARQL SELECT query.
     */
    SPARQLTUPLE,

    /**
     * The query response was of a SPARQL ASK query.
     */
    SPARQLBOOLEAN;
}
