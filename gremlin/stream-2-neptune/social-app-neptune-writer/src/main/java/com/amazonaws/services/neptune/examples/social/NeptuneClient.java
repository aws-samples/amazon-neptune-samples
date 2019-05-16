/*
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.amazonaws.services.neptune.examples.social;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.neptune.examples.utils.ActivityTimer;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

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
        try (ActivityTimer timer = new ActivityTimer(logger, "Create Neptune client")) {
            cluster = Cluster.build()
                    .addContactPoint(endpoint)
                    .port(8182)
                    .maxInProcessPerConnection(1)
                    .minInProcessPerConnection(1)
                    .maxConnectionPoolSize(1)
                    .minConnectionPoolSize(1)
                    .maxSimultaneousUsagePerConnection(1)
                    .minSimultaneousUsagePerConnection(1)
                    .create();

            g = AnonymousTraversalSource
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

    GraphTraversalSource newTraversal() {
        try (ActivityTimer timer = new ActivityTimer(logger, "New traversal")) {
            return g;
        }
    }

    @Override
    public void close() throws Exception {
        try (ActivityTimer timer = new ActivityTimer(logger, "Close Neptune client")) {
            if (cluster != null && !cluster.isClosed() && !cluster.isClosing()) {
                cluster.closeAsync();
            }
        }
    }
}
