package software.amazon.neptune.onegraph.playground.server.api.response;

/**
 * Used to indicate what kind of query response the {@link QueryResponse} is.
 */
public enum QueryResponseType {

    /**
     * The query response is of a Gremlin traversal.
     */
    GREMLINTRAVERSAL,

    /**
     * The query response is of a SPARQL graph query.
     */
    SPARQLGRAPH,

    /**
     * The query response is of a SPARQL tuple query.
     */
    SPARQLTUPLE,

    /**
     * The query response is of a SPARQL boolean query.
     */
    SPARQLBOOLEAN;
}
