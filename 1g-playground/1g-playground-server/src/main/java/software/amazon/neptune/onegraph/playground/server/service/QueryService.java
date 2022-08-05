package software.amazon.neptune.onegraph.playground.server.service;

import lombok.NonNull;
import org.eclipse.rdf4j.model.Value;
import software.amazon.neptune.onegraph.playground.server.api.response.QueryResponseType;
import software.amazon.neptune.onegraph.playground.server.querying.GremlinQuerying;
import software.amazon.neptune.onegraph.playground.server.querying.GremlinQueryingDelegate;
import software.amazon.neptune.onegraph.playground.server.querying.QueryException;
import software.amazon.neptune.onegraph.playground.server.querying.SPARQLQuerying;
import software.amazon.neptune.onegraph.playground.server.querying.SPARQLQueryingDelegate;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.api.response.QueryResponse;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Service that handles the query command.
 */
public class QueryService extends Service implements SPARQLQueryingDelegate, GremlinQueryingDelegate {

    private CompletableFuture<QueryResponse> future;

    private final GremlinQuerying gremlin = new GremlinQuerying(this);
    private final SPARQLQuerying sparql = new SPARQLQuerying(this);

    /**
     * Initialize the query service with the given state.
     * @param state The state.
     */
    public QueryService(State state) {
        super(state);
    }

    /**
     * Perform a Gremlin query on the data set in the {@link State}.
     * @param query The query to perform
     * @return A future containing the result of the query.
     */
    public Future<QueryResponse> performGremlinQuery(@NonNull String query) {
        this.future = new CompletableFuture<>();
        gremlin.queryGraph(state.getTinkerPopGraph(), query, state.getQueryTimeoutMillis());
        return this.future;
    }

    /**
     * Perform a SPARQL query on the data set in the {@link State}.
     * @param query The query to perform
     * @return A future containing the result of the query.
     */
    public Future<QueryResponse> performSPARQLQuery(@NonNull String query) {
        this.future = new CompletableFuture<>();
        sparql.queryRepository(state.getRdfRepository(), query, state.getQueryTimeoutMillis());

        return this.future;
    }

    @Override
    public void gremlinQueryFinishedWithResult(List<?> result) {
        QueryResponse qResult = new QueryResponse();
        qResult.type = QueryResponseType.GREMLINTRAVERSAL;
        qResult.traversalResult = result.stream().map(Object::toString).collect( Collectors.toList() );
        if (this.future != null) {
            this.future.complete(qResult);
        }
    }

    @Override
    public void gremlinQueryFailedWithException(QueryException exception) {
        this.future.completeExceptionally(exception);
    }

    @Override
    public void SPARQLQueryFinishedWithResult(boolean result) {
        QueryResponse qResult = new QueryResponse();
        qResult.type = QueryResponseType.SPARQLBOOLEAN;
        qResult.booleanResult = result;
        if (this.future != null) {
            this.future.complete(qResult);
        }
    }

    @Override
    public void SPARQLQueryFinishedWithResult(Model result) {
        QueryResponse qResult = new QueryResponse();
        qResult.type = QueryResponseType.SPARQLGRAPH;

        ArrayList<String> s = new ArrayList<>();
        ArrayList<String> p = new ArrayList<>();
        ArrayList<String> o = new ArrayList<>();
        ArrayList<String> g = new ArrayList<>();

        for (Statement stat : result) {
            s.add(String.valueOf(stat.getSubject()));
            p.add(String.valueOf(stat.getPredicate()));
            o.add(String.valueOf(stat.getObject()));
            g.add(String.valueOf(stat.getContext()));
        }
        qResult.graphResult = new HashMap<>();
        qResult.graphResult.put("s", s);
        qResult.graphResult.put("p", p);
        qResult.graphResult.put("o", o);
        qResult.graphResult.put("g", g);

        if (this.future != null) {
            this.future.complete(qResult);
        }
    }

    @Override
    public void SPARQLQueryFinishedWithResult(Map<String, List<Value>> result) {
        QueryResponse qResult = new QueryResponse();
        qResult.type = QueryResponseType.SPARQLTUPLE;
        qResult.tupleResult = valueMapToStringMap(result);

        if (this.future != null) {
            this.future.complete(qResult);
        }
    }

    @Override
    public void SPARQLQueryFailedWithException(QueryException exception) {
        if (this.future != null) {
            this.future.completeExceptionally(exception);
        }
    }

    // Converts the given map of form Map<String, List<Value>> to form Map<String, List<String>>
    private Map<String, List<String>> valueMapToStringMap(@NonNull Map<String, @NonNull  List<Value>> result) {
        Map<String, List<String>> stringResult = new HashMap<>();
        for (Entry<String, List<Value>> entry : result.entrySet()) {
            String columnName = entry.getKey();
            List<Value> values = entry.getValue();
            List<String> m = values.stream().map(Value::toString).collect(Collectors.toList());
            stringResult.put(columnName, m);
        }
        return stringResult;
    }
}
