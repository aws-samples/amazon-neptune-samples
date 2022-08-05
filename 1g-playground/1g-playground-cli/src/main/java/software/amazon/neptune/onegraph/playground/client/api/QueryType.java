package software.amazon.neptune.onegraph.playground.client.api;

/**
 * Queries are either SPARQL or Gremlin.
 */
public enum QueryType {

    SPARQL,
    GREMLIN;

    /**
     * Creates a query type from the given arguments
     * @param sparql True if this query is a SPARQL query.
     * @param gremlin True if this query is a Gremlin query.
     * @return The query type according to the given arguments
     * @throws IllegalArgumentException When both {@code sparql} and {@code gremlin} are true or when both are false.
     */
    public static QueryType queryTypeFromCLIArguments(boolean sparql, boolean gremlin) throws IllegalArgumentException {
        if ((sparql && gremlin) || (!sparql && !gremlin)) {
            throw new IllegalArgumentException("Incorrect amount of flags: please pass exactly 1 query type flag");
        }
        if (sparql) {
            return SPARQL;
        } else {
            return GREMLIN;
        }
    }
}
