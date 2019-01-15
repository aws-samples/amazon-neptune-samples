package com.amazonaws.services.neptune.examples.social;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.T;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.V;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.unfold;

class BatchAddEdges {

    private final AtomicReference<GraphTraversal<?, ?>> traversal;
    private final LambdaLogger logger;
    private final boolean conditionalCreate;

    BatchAddEdges(NeptuneClient neptuneClient, LambdaLogger logger, boolean conditionalCreate) {
        this.traversal = new AtomicReference<>(neptuneClient.newTraversal());
        this.logger = logger;
        this.conditionalCreate = conditionalCreate;

        logger.log("Begin batch query");
    }

    void addEdge(String fromVertexId, String toVertexId, String creationDate, long insertDateTime) {

        String edgeId = conditionalCreate ?
                String.format("%s-%s-%s", fromVertexId, creationDate, toVertexId) :
                UUID.randomUUID().toString() ;

        GraphTraversal<?, ?> t = traversal.get();

        if (conditionalCreate) {
            traversal.set(t.V(fromVertexId).outE("follows").hasId(edgeId).fold().coalesce(
                    unfold(),
                    V(fromVertexId).addE("follows").to(V(toVertexId)).
                            property(T.id, edgeId).
                            property("creationDate", creationDate).
                            property("insertDateTime", insertDateTime)
            ));
        } else {
            traversal.set(t.V(fromVertexId).addE("follows").to(V(toVertexId)).
                    property(T.id, edgeId).
                    property("creationDate", creationDate).
                    property("insertDateTime", insertDateTime)
            );
        }
    }

    void provokeError() {
        logger.log("Forcing a ConstraintViolationException (and rollback)");

        traversal.set(traversal.get().
                addV("error").property(T.id, "error").
                addV("error").property(T.id, "error"));
    }

    long execute() {
        GraphTraversal<?, ?> t = traversal.get();
        long start = System.nanoTime();
        t.forEachRemaining(
                e -> logger.log(e.toString())
        );
        long end = System.nanoTime();
        long duration = TimeUnit.NANOSECONDS.toMillis(end - start);
        logger.log("End batch query (" + duration + " ms)");
        return duration;
    }
}
