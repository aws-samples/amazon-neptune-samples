package software.amazon.neptune.onegraph.playground.server.querying;

import lombok.NonNull;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Facilitates querying an RDF {@link Repository} in SPARQL.
 */
public class SPARQLQuerying {

    /**
     * The querying delegate the query responses will be called on.
     */
    public final SPARQLQueryingDelegate delegate;

    /**
     * Create a new gremlin query facilitator with given delegate.
     * @param delegate The delegate the query responses will be called on.
     */
    public SPARQLQuerying(@NonNull SPARQLQueryingDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Queries the given repo with given query, calls result on {@link #delegate}.
     * @param repo The repository to query.
     * @param query The SPARQL query string.
     */
    public void queryRepository(@NonNull Repository repo, @NonNull  String query, long timeoutMillis) {
        try (RepositoryConnection con = repo.getConnection()) {

            Query q = con.prepareQuery(QueryLanguage.SPARQL, query);
            q.setMaxExecutionTime((int) Math.ceil(timeoutMillis / 1000f));

            if (q instanceof TupleQuery) {
                this.performQuery((TupleQuery) q);
            } else if (q instanceof BooleanQuery) {
                this.performQuery((BooleanQuery) q);
            } else if (q instanceof GraphQuery) {
                this.performQuery((GraphQuery) q);
            } else {
                this.delegate.SPARQLQueryFailedWithException(new QueryException(String.format("\"Unsupported query exception;" +
                                " the query %s is of unsupported format.", query), new IllegalArgumentException()));
            }
        } catch (Exception e) {
            this.delegate.SPARQLQueryFailedWithException(
                    new QueryException("An exception occurred during the execution of the query: " + query, e));
        }
    }

    private void performQuery(TupleQuery query) throws QueryEvaluationException {
        Map<String, List<Value>> valueMap = new HashMap<>();
        try (TupleQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                BindingSet bindingSet = result.next();
                for (String name : bindingSet.getBindingNames()) {
                    Value value = bindingSet.getValue(name);
                    valueMap.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
                }
            }
        }
        this.delegate.SPARQLQueryFinishedWithResult(valueMap);
    }

    private void performQuery(BooleanQuery query) throws QueryEvaluationException {
        boolean result = query.evaluate();
        this.delegate.SPARQLQueryFinishedWithResult(result);
    }

    private void performQuery(GraphQuery query) throws QueryEvaluationException {
        Model m = new LinkedHashModel();

        try (GraphQueryResult result = query.evaluate()) {
            while (result.hasNext()) {
                m.add(result.next());
            }
        }
        this.delegate.SPARQLQueryFinishedWithResult(m);
    }
}
