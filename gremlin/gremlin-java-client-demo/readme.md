# Gremlin Java Client Demo

This app show how to use the Gremlin Java client to connect to Amazon Neptune via a Network Load Balancer (NLB) or Application Load Balancer (ALB), with or without SSL enabled, and with or without [IAM DB Authentication](https://docs.aws.amazon.com/neptune/latest/userguide/iam-auth.html) enabled.

You can use the four classes in the `org.apache.tinkerpop.gremlin.driver` package in your own application to facilitate connecting to Neptune via a load balancer. You can use the `NeptuneClusterBuilder` where normally you would use a `ClusterBuilder` in your client code to configure the driver for a load balancer with SSL and IAM DB Auth properties:

```
Cluster cluster = NeptuneClusterBuilder.build()
        .enableSsl(true)
        .enableIamAuth(true)
        .addContactPoint(neptuneClusterEndpoint)
        .networkLoadBalancerEndpoint(networkLoadBalancerEndpoint)
        .create();
```

The demo app simply connects to your Neptune database and executes the following query:

```
 g.V().limit(10)
  .valueMap().with(WithOptions.tokens)
  .toList()
```

## Build the solution
```
mvn clean install
```

## Examples

Connect to Neptune via NLB:

```
java -jar target/gremlin-java-client-demo.jar --nlb-endpoint <nlb_endpoint>
```

Connect to Neptune via NLB with SSL enabled:

```
java -jar target/gremlin-java-client-demo.jar --nlb-endpoint <nlb_endpoint> --enable-ssl
```

Connect to Neptune via NLB with SSL and IAM DB Auth enabled:

```
java -jar target/gremlin-java-client-demo.jar --nlb-endpoint <nlb_endpoint> --enable-ssl --enable-iam-auth --neptune-endpoint <neptune_endpoint>
```

Connect to Neptune via ALB:

```
java -jar target/gremlin-java-client-demo.jar --alb-endpoint <alb_endpoint>
```

Connect to Neptune via ALB with SSL enabled:

```
java -jar target/gremlin-java-client-demo.jar --alb-endpoint <alb_endpoint> --enable-ssl
```

Connect to Neptune via ALB with SSL and IAM DB Auth enabled:

```
java -jar target/gremlin-java-client-demo.jar --alb-endpoint <alb_endpoint> --enable-ssl --enable-iam-auth --neptune-endpoint <neptune_endpoint>
```

## Parameters

If you are connecting to Neptune via a NLB or ALB, without SSL or IAM DB Auth enabled, you need only supply either an `--nlb-endpoint` or `--alb-endpoint`, and, optionally, an `--lb-port` (default 80).

If you want to connect via SSL, you will also need to supply an `--enable-ssl` parameter.

If you have IAM DB Auth enabled for your Neptune database, you must supply at least one `--neptune-endpoint`, and, optionally, a `--neptune-port` (default 8182).

## Additional requirements

If you are using a load balancer or a proxy server (such as HAProxy), you must use SSL termination and have your own SSL certificate on the proxy server. SSL passthrough doesn't work because the provided SSL certificates don't match the proxy server hostname. Note that the Network Load Balancer, although a Layer 4 load balancer, now supports [TLS termination](https://aws.amazon.com/blogs/aws/new-tls-termination-for-network-load-balancers/).

If you have IAM DB Auth enabled for your Neptune database, you must set the **SERVICE_REGION** environment variable before connecting from client – e.g. `export SERVICE_REGION=us-east-1`.

## Further reading

For more details on connecting to Neptune via SSL, see [Encryption in Transit: Connecting to Neptune Using SSL/HTTPS](https://docs.aws.amazon.com/neptune/latest/userguide/security-ssl.html).

For more details on connecting to Neptune via a load balancer, see [Connecting to Amazon Neptune from Clients Outside the Neptune VPC](https://github.com/aws-samples/aws-dbs-refarch-graph/tree/master/src/connecting-using-a-load-balancer).

## License Summary

This sample code is made available under a modified MIT license. See the LICENSE file.