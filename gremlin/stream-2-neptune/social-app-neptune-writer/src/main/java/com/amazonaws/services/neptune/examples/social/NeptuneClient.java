package com.amazonaws.services.neptune.examples.social;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.util.empty.EmptyGraph;

public class NeptuneClient implements AutoCloseable {
    private final String endpoint;
    private final LambdaLogger logger;

    private Cluster cluster;
    private GraphTraversalSource g;

    NeptuneClient(String endpoint, LambdaLogger logger) {
        this.endpoint = endpoint;
        this.logger = logger;

        init();
    }

    private void init() {
        try {
            cluster = Cluster.build()
                    .addContactPoint(endpoint)
                    .port(8182)
                    .create();
            g = EmptyGraph.instance()
                    .traversal()
                    .withRemote(DriverRemoteConnection.using(cluster));

        } catch (Exception e) {
            logger.log("Error: " + e.getMessage());
            if (cluster != null && !cluster.isClosed() && !cluster.isClosing()) {
                cluster.close();
            }
            throw new RuntimeException(e);
        }
    }

    GraphTraversal<?, ?> newTraversal(){
        return g.inject(0);
    }

    @Override
    public void close() throws Exception {
        g.close();
        if (cluster != null && !cluster.isClosed() && !cluster.isClosing()) {
            cluster.close();
        }
    }
}
