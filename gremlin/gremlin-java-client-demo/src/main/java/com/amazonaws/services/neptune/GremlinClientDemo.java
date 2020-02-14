/*
Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.

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

package com.amazonaws.services.neptune;

import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.*;
import org.apache.tinkerpop.gremlin.driver.*;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.WithOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Command(name = "connect", description = "Connect to Neptune")
public class GremlinClientDemo implements Runnable {

    @Option(name = {"--neptune-endpoint"}, description = "Neptune endpoint(s) – supply multiple instance endpoints if you want to load balance requests across a cluster")
    private List<String> neptuneEndpoints = new ArrayList<>();

    @Option(name = {"--port"}, description = "Neptune port (optional, default 8182)")
    @Port(acceptablePorts = {PortType.SYSTEM, PortType.USER})
    @Once
    private int neptunePort = 8182;

    @Option(name = {"--enable-iam-auth"}, description = "Use IAM database authentication to authenticate to Neptune (remember to set SERVICE_REGION environment variable, and, if using a load balancer, set the --host-header option as well)")
    @Once
    private boolean enableIamAuth = false;

    @Option(name = {"--enable-ssl"}, description = "Enables connectivity over SSL (optional, default false)")
    @Once
    private boolean enableSsl = false;

    @Option(name = {"--nlb-endpoint"}, description = "Network load balancer endpoint (optional: use only if connecting to an IAM DB enabled Neptune cluster through a network load balancer (NLB) – see https://github.com/aws-samples/aws-dbs-refarch-graph/tree/master/src/connecting-using-a-load-balancer#connecting-to-amazon-neptune-from-clients-outside-the-neptune-vpc-using-aws-network-load-balancer)")
    @Once
    @MutuallyExclusiveWith(tag = "load-balancer")
    private String networkLoadBalancerEndpoint;

    @Option(name = {"--alb-endpoint"}, description = "Application load balancer endpoint (optional: use only if connecting to an IAM DB enabled Neptune cluster through an application load balancer (ALB) – see https://github.com/aws-samples/aws-dbs-refarch-graph/tree/master/src/connecting-using-a-load-balancer#connecting-to-amazon-neptune-from-clients-outside-the-neptune-vpc-using-aws-application-load-balancer)")
    @Once
    @MutuallyExclusiveWith(tag = "load-balancer")
    private String applicationLoadBalancerEndpoint;

    @Option(name = {"--lb-port"}, description = "Load balancer port (optional, default 80)")
    @Port(acceptablePorts = {PortType.SYSTEM, PortType.USER})
    @Once
    private int loadBalancerPort = 80;

    @Override
    public void run() {

        // Turn on low-level Gremlin driver logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");

        try {

            Cluster cluster = NeptuneClusterBuilder.build()
                    .enableSsl(enableSsl)
                    .enableIamAuth(enableIamAuth)
                    .addContactPoints(neptuneEndpoints)
                    .port(neptunePort)
                    .networkLoadBalancerEndpoint(networkLoadBalancerEndpoint)
                    .applicationLoadBalancerEndpoint(applicationLoadBalancerEndpoint)
                    .loadBalancerPort(loadBalancerPort)
                    .create();

            Client client = cluster.connect();
            DriverRemoteConnection connection = DriverRemoteConnection.using(client);
            GraphTraversalSource g = AnonymousTraversalSource.traversal().withRemote(connection);

            List<Map<Object, Object>> results =
                    g.V().limit(10)
                            .valueMap().with(WithOptions.tokens)
                            .toList();

            for (Map<Object, Object> result : results) {
                System.out.println(result);
            }

            client.close();
            cluster.closeAsync();

        } catch (Exception e) {
            System.err.println("An error occurred while connecting to Neptune:");
            e.printStackTrace();
            System.exit(-1);
        }

    }

    public static void main(String[] args) {
        SingleCommand<GremlinClientDemo> parser = SingleCommand.singleCommand(GremlinClientDemo.class);
        GremlinClientDemo cmd = parser.parse(args);
        cmd.run();
    }
}
