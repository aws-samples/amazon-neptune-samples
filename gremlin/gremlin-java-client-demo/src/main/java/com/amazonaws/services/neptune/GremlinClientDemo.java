/*
Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at
    http://www.apache.org/licenses/LICENSE-2.0
or in the "license" file accompanying this file. This file is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing
permissions and limitations under the License.
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
