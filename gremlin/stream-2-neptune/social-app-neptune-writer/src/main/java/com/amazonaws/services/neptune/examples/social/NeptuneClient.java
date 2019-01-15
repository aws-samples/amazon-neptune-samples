/*
Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

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
