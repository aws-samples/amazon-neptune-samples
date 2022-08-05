package software.amazon.neptune.onegraph.playground.server.querying;

import lombok.NonNull;
import org.apache.tinkerpop.gremlin.groovy.engine.GremlinExecutor;
import org.apache.tinkerpop.gremlin.jsr223.ConcurrentBindings;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Facilitates querying {@link Graph}s in Gremlin.
 */
public class GremlinQuerying {

    /**
     * The querying delegate the query responses will be called on.
     */
    public final GremlinQueryingDelegate delegate;

    /**
     * Create a new gremlin query facilitator with given delegate.
     * @param delegate The delegate the query responses will be called on.
     */
    public GremlinQuerying(@NonNull GremlinQueryingDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Queries the given graph with given traversal, calls result on {@link #delegate}.
     * @param graph The graph to query.
     * @param traversalString The traversal query string.
     */
    public void queryGraph(@NonNull Graph graph, @NonNull String traversalString, long timeoutMillis) {
        ConcurrentBindings b = new ConcurrentBindings();
        b.putIfAbsent("g", graph.traversal());

        try (GremlinExecutor ge = GremlinExecutor.build().globalBindings(b).create()) {
            CompletableFuture<Object> r1 = ge.eval(traversalString);
            r1.exceptionally(ex -> {
                this.delegate.gremlinQueryFailedWithException(new QueryException("An exception occurred during the execution of the traversal: "
                        + traversalString, ex));
                return null;
            });
            GraphTraversal<?,?> r2 = (GraphTraversal<?,?>) r1.get(timeoutMillis, TimeUnit.MILLISECONDS);
            if (r2 != null) {
                this.delegate.gremlinQueryFinishedWithResult(r2.toList());
            } else {
                this.delegate.gremlinQueryFailedWithException(new QueryException(String.format("An exception occurred " +
                                "during the execution of the traversal: %s",traversalString), new TimeoutException()));
            }

        } catch (TimeoutException e) {
            this.delegate.gremlinQueryFailedWithException(
                    new QueryException(String.format("The traversal %s timed out",traversalString), e));
        } catch (Exception e) {
            this.delegate.gremlinQueryFailedWithException(new QueryException(String.format("An exception occurred " +
                    "during the execution of the traversal: %s",traversalString), e));
        }
    }
}
