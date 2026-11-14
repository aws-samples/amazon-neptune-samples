# Gremlin Java Client Demo

This app show how to use the Gremlin Java client to connect to Amazon Neptune via a Network Load Balancer (NLB) or Application Load Balancer (ALB) and with or without [IAM DB Authentication](https://docs.aws.amazon.com/neptune/latest/userguide/iam-auth.html) enabled.

Amazon Neptune only accepts SSL/TLS connections, so this demo always connects over SSL — there is no option to disable it.

You can use the `NeptuneClusterBuilder` class (in the `com.amazonaws.services.neptune` package) in your own application to facilitate connecting to Neptune via a load balancer. Use it where you would normally use `Cluster.build()` in your client code to configure the driver with IAM DB Auth properties (SSL is enabled automatically):

```
Cluster cluster = NeptuneClusterBuilder.build()
        .enableIamAuth(true)
        .addContactPoint(neptuneClusterEndpoint)
        .networkLoadBalancerEndpoint(networkLoadBalancerEndpoint)
        .create();
```

### How SigV4 signing works

IAM database authentication requires every Gremlin websocket connection to be SigV4-signed. This demo signs requests using the [`amazon-neptune-sigv4-signer`](https://github.com/aws/amazon-neptune-sigv4-signer) library together with Apache TinkerPop's built-in `Cluster.Builder.requestInterceptor(...)` extension point (available since TinkerPop 3.6.6).

Earlier versions of this sample depended on the `amazon-neptune-gremlin-java-sigv4` library, which supplied a custom `Channelizer` and `WebSocketClientHandshaker` to inject the SigV4-signed `Host` header into the handshake. That library is now [deprecated and archived](https://github.com/aws/amazon-neptune-gremlin-java-sigv4) — TinkerPop provides first-class request/handshake interception, so the custom channelizer is no longer needed. When connecting through a load balancer, `NeptuneClusterBuilder` rewrites the `Host` header to one of the supplied Neptune instance endpoints inside the request interceptor before signing, and leaves it in place so that Neptune receives the same `Host` value that was signed.

The signer uses AWS SDK for Java v2 credentials via `DefaultCredentialsProvider`, so it resolves credentials from the standard chain (environment variables, system properties, profile files, container/instance roles, etc.).

The demo app simply connects to your Neptune database and executes the following query:

```
 g.V().limit(10)
  .valueMap().with(WithOptions.tokens)
  .toList()
```

## Build the solution

This module requires **Java 17 or later** (the `amazon-neptune-sigv4-signer` dependency and the AWS SDK for Java v2 it pulls in are built for Java 17).

```
mvn clean install
```

## Examples

Connect to Neptune via NLB:

```
java -jar target/gremlin-java-client-demo.jar --nlb-endpoint <nlb_endpoint>
```

Connect to Neptune via NLB with IAM DB Auth enabled:

```
java -jar target/gremlin-java-client-demo.jar --nlb-endpoint <nlb_endpoint> --enable-iam-auth --neptune-endpoint <neptune_endpoint>
```

Connect to Neptune via ALB:

```
java -jar target/gremlin-java-client-demo.jar --alb-endpoint <alb_endpoint>
```

Connect to Neptune via ALB with IAM DB Auth enabled:

```
java -jar target/gremlin-java-client-demo.jar --alb-endpoint <alb_endpoint> --enable-iam-auth --neptune-endpoint <neptune_endpoint>
```

## Parameters

All connections use SSL/TLS; this is always on and cannot be disabled, because Amazon Neptune only accepts encrypted connections.

If you are connecting to Neptune via a NLB or ALB without IAM DB Auth enabled, you need only supply either an `--nlb-endpoint` or `--alb-endpoint`, and, optionally, an `--lb-port` (default 80). Because the client always uses TLS, set `--lb-port` to the port of your load balancer's TLS or HTTPS listener (for example, 443).

If you have IAM DB Auth enabled for your Neptune database, you must supply at least one `--neptune-endpoint`, and, optionally, a `--port` for Neptune (default 8182).

## Connecting through a load balancer

Clients outside the Neptune VPC can't reach Neptune's endpoints directly, so this demo connects through a load balancer instead. When IAM DB Auth is enabled, this creates a problem: the SigV4 signature covers the `Host` header, and Neptune only accepts a signature computed for its own endpoint, while the client is actually dialing the load balancer. The demo solves this by setting the `Host` header to a Neptune instance endpoint (one of the `--neptune-endpoint` values) before signing. Both NLB and ALB forward that header to Neptune unchanged, so you don't need a proxy such as HAProxy to rewrite it, which earlier versions of this architecture required.

The two load balancer types handle TLS differently. A Network Load Balancer works at Layer 4, so you can either pass TLS through to Neptune with a TCP listener or [terminate TLS](https://aws.amazon.com/blogs/aws/new-tls-termination-for-network-load-balancers/) with a TLS listener and your own certificate. With pass-through, the client receives Neptune's certificate, which is issued for Neptune's endpoint names rather than the load balancer's hostname. This demo still connects, because the Gremlin Java driver validates the certificate chain but not the hostname (verified with gremlin-driver 3.8.2). However, a client that verifies hostnames will reject the connection, in which case you should terminate TLS at the load balancer instead.

An Application Load Balancer works at Layer 7 and always terminates TLS, so it has the following limitations:

- It needs an HTTPS listener with its own certificate for the domain name that clients use to reach it, such as a DNS name that you point at the load balancer. The client must trust that certificate; if you use a private or self-signed certificate, add it to a Java truststore and pass `-Djavax.net.ssl.trustStore=<path>` when you run the demo.
- It opens a new TLS connection to the Neptune targets, so its target group must use HTTPS. With IAM DB Auth enabled, the load balancer's health checks are unsigned and Neptune answers them with `403`, so include 403 in the health check's success codes (for example, `200-499`).
- Its listener must allow WebSockets, which ALB HTTP and HTTPS listeners do, because Gremlin connections are WebSocket upgrades.

## Additional requirements

If you have IAM DB Auth enabled for your Neptune database, set the AWS region used for SigV4 signing. You can set the **SERVICE_REGION** environment variable before connecting from the client – e.g. `export SERVICE_REGION=us-east-1`. If `SERVICE_REGION` is not set, the AWS SDK's default region provider chain is used (e.g. `AWS_REGION`, the active profile's region, or instance/container metadata). Credentials are resolved from the AWS SDK for Java v2 default credentials provider chain.

## Further reading

For more details on connecting to Neptune via SSL, see [Encryption in Transit: Connecting to Neptune Using SSL/HTTPS](https://docs.aws.amazon.com/neptune/latest/userguide/security-ssl.html).

For more details on connecting to Neptune via a load balancer, see [Connecting to Amazon Neptune from Clients Outside the Neptune VPC](https://github.com/aws-samples/aws-dbs-refarch-graph/tree/master/src/connecting-using-a-load-balancer). Note that its ALB architecture still places HAProxy behind the load balancer to rewrite the `Host` header, which this demo no longer needs.

## License Summary

This sample code is made available under a modified MIT license. See the LICENSE file.