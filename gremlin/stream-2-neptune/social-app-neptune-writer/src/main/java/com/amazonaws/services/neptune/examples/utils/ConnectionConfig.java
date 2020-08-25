/*
Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.

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

package com.amazonaws.services.neptune.examples.utils;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ConnectionConfig implements RetryCondition {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionConfig.class);

    private Cluster cluster = null;

    public TraversalSource traversalSource() {

        if (cluster() == null) {
            createCluster();
        }

        Client client = cluster().connect();
        DriverRemoteConnection connection = DriverRemoteConnection.using(client);
        GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(connection);

        return new ShortLivedTraversalSource(g, client);
    }

    @Override
    public boolean allowRetry(Throwable e) {

        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String message = stringWriter.toString();

        logger.info(String.format("Determining whether this is a retriable Cluster error: %s", message));

        if (message.contains("Timed out while waiting for an available host") ||
                message.contains("Connection reset by peer") ||
                message.contains(EnvironmentVariableUtils.getMandatoryEnv("neptuneEndpoint"))) {
            try {
                if (cluster != null) {
                    cluster.close();
                }
            } catch (Exception ex) {
                logger.error("Error closing cluster", ex);
            }
            cluster = null;
            return true;
        }
        return false;
    }

    Cluster cluster(){
        return cluster;
    }

    void createCluster() {

        logger.info("Creating cluster");
        cluster = Cluster.build()
                .addContactPoint(EnvironmentVariableUtils.getMandatoryEnv("neptune_endpoint"))
                .port(Integer.parseInt(EnvironmentVariableUtils.getOptionalEnv("neptune_port", "8182")))
                .enableSsl(true)
                .minConnectionPoolSize(1)
                .maxConnectionPoolSize(1)
                .maxSimultaneousUsagePerConnection(16)
                .maxInProcessPerConnection(16)
                .serializer(Serializers.GRAPHBINARY_V1D0)
                .create();
        logger.info(String.format(
                "minInProcessPerConnection: %s, " +
                        "maxInProcessPerConnection: %s, " +
                        "minSimultaneousUsagePerConnection: %s, " +
                        "maxSimultaneousUsagePerConnection: %s, " +
                        "minConnectionPoolSize: %s, " +
                        "maxConnectionPoolSize: %s",
                cluster.getMinInProcessPerConnection(),
                cluster.getMaxInProcessPerConnection(),
                cluster.minSimultaneousUsagePerConnection(),
                cluster.maxSimultaneousUsagePerConnection(),
                cluster.minConnectionPoolSize(),
                cluster.maxConnectionPoolSize()));
    }

    private static class ShortLivedTraversalSource implements TraversalSource {

        private static final Logger logger = LoggerFactory.getLogger(ShortLivedTraversalSource.class);

        private final GraphTraversalSource g;
        private final Client client;

        public ShortLivedTraversalSource(GraphTraversalSource g, Client client) {
            this.g = g;
            this.client = client;
        }

        @Override
        public GraphTraversalSource get() {
            return g;
        }

        @Override
        public void close() {
            try {
                client.close();
            } catch (Throwable e) {
                logger.error("Error closing client", e);
            }
        }
    }
}
