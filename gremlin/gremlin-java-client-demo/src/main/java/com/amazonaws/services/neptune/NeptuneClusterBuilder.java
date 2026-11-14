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

import com.amazonaws.neptune.auth.NeptuneNettyHttpSigV4Signer;
import com.amazonaws.neptune.auth.NeptuneSigV4SignerException;
import io.netty.handler.codec.http.FullHttpRequest;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.RequestInterceptor;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a Gremlin {@link Cluster} configured to connect to Amazon Neptune, optionally
 * via a Network Load Balancer (NLB) or Application Load Balancer (ALB), and with or without IAM
 * database authentication. SSL/TLS is always enabled, because Amazon Neptune only accepts
 * encrypted connections.
 *
 * <p>SigV4 request signing is performed using {@code amazon-neptune-sigv4-signer} together
 * with TinkerPop's built-in {@link Cluster.Builder#requestInterceptor(RequestInterceptor)}
 * extension point (available since TinkerPop 3.6.6). The previously required
 * {@code amazon-neptune-gremlin-java-sigv4} library and its custom {@code Channelizer} are no
 * longer needed — see
 * <a href="https://github.com/aws/amazon-neptune-gremlin-java-sigv4/pull/54">this deprecation</a>.
 *
 * <p>When connecting through a load balancer (NLB or ALB) with IAM auth enabled, the
 * {@code Host} header of the handshake request is rewritten to one of the supplied Neptune
 * instance endpoints before signing, because the SigV4 signature is computed over the
 * {@code Host} header. The header is left in place: the load balancer forwards it unchanged, so
 * Neptune receives the same {@code Host} value that was signed. No proxy (such as HAProxy) is
 * needed to rewrite it.
 */
public class NeptuneClusterBuilder {

    /**
     * Environment variable used to supply the AWS region for SigV4 signing. Retained for
     * backwards compatibility with the original demo; if unset, the AWS SDK's default region
     * provider chain is used.
     */
    private static final String SERVICE_REGION_ENV_VAR = "SERVICE_REGION";

    public static NeptuneClusterBuilder build() {
        return new NeptuneClusterBuilder();
    }

    private final Cluster.Builder innerBuilder = Cluster.build();

    private final List<String> addresses = new ArrayList<>();
    private String networkLoadBalancerEndpoint;
    private String applicationLoadBalancerEndpoint;
    private boolean enableIamAuth = false;
    private int port = 8182;
    private int loadBalancerPort = 80;

    private NeptuneClusterBuilder() {
    }

    public NeptuneClusterBuilder addContactPoint(final String address) {
        this.addresses.add(address);
        return this;
    }

    public NeptuneClusterBuilder addContactPoints(final String... addresses) {
        for (String address : addresses) {
            addContactPoint(address);
        }
        return this;
    }

    public NeptuneClusterBuilder addContactPoints(final List<String> addresses) {
        for (String address : addresses) {
            addContactPoint(address);
        }
        return this;
    }

    public NeptuneClusterBuilder port(final int port) {
        this.port = port;
        return this;
    }

    public NeptuneClusterBuilder loadBalancerPort(final int port) {
        this.loadBalancerPort = port;
        return this;
    }

    public NeptuneClusterBuilder networkLoadBalancerEndpoint(final String endpoint) {
        this.networkLoadBalancerEndpoint = endpoint;
        return this;
    }

    public NeptuneClusterBuilder applicationLoadBalancerEndpoint(final String endpoint) {
        this.applicationLoadBalancerEndpoint = endpoint;
        return this;
    }

    public NeptuneClusterBuilder enableIamAuth(final boolean enable) {
        this.enableIamAuth = enable;
        return this;
    }

    private boolean isDirectConnection() {
        return networkLoadBalancerEndpoint == null && applicationLoadBalancerEndpoint == null;
    }

    public Cluster create() {

        // Amazon Neptune only accepts SSL/TLS connections, so SSL is always enabled.
        innerBuilder.enableSsl(true);

        if (addresses.isEmpty()) {
            if (isDirectConnection()) {
                throw new IllegalArgumentException("You must supply one or more Neptune endpoints");
            } else if (enableIamAuth) {
                throw new IllegalArgumentException(
                        "You must supply one or more Neptune endpoints to sign the Host header");
            }
        }

        if (isDirectConnection()) {
            innerBuilder.port(port);
            for (String address : addresses) {
                innerBuilder.addContactPoint(address);
            }
        } else {
            innerBuilder.port(loadBalancerPort);
            if (networkLoadBalancerEndpoint != null) {
                System.out.println("Adding " + networkLoadBalancerEndpoint);
                innerBuilder.addContactPoint(networkLoadBalancerEndpoint);
            } else {
                innerBuilder.addContactPoint(applicationLoadBalancerEndpoint);
            }
        }

        if (enableIamAuth) {
            // For a direct connection the Host header already carries the Neptune endpoint. For a
            // load balancer (NLB or ALB) it carries the load balancer's name, so it is rewritten to
            // a Neptune endpoint before signing and then left in place for Neptune to verify.
            final boolean rewriteHostHeader = !isDirectConnection();

            innerBuilder.requestInterceptor(sigV4RequestInterceptor(rewriteHostHeader));
        }

        return innerBuilder.create();
    }

    /**
     * Builds a {@link RequestInterceptor} that rewrites the {@code Host} header (when connecting
     * through a load balancer) and SigV4-signs each Gremlin WebSocket handshake request.
     */
    private RequestInterceptor sigV4RequestInterceptor(final boolean rewriteHostHeader) {

        final String region = resolveRegion();
        final AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.create();
        final Random random = new Random();

        return (FullHttpRequest request) -> {

            if (rewriteHostHeader) {
                final String endpoint = addresses.get(random.nextInt(addresses.size()));
                request.headers().remove("Host");
                request.headers().add("Host", String.format("%s:%s", endpoint, port));
            }

            try {
                final NeptuneNettyHttpSigV4Signer signer =
                        new NeptuneNettyHttpSigV4Signer(region, credentialsProvider);
                signer.signRequest(request);
            } catch (NeptuneSigV4SignerException e) {
                throw new RuntimeException("Exception occurred while signing the request", e);
            }

            return request;
        };
    }

    /**
     * Resolves the AWS region for signing. The {@code SERVICE_REGION} environment variable takes
     * precedence (as documented by the original demo); otherwise the AWS SDK default region
     * provider chain is used.
     */
    private static String resolveRegion() {
        final String fromEnv = System.getenv(SERVICE_REGION_ENV_VAR);
        if (fromEnv != null && !fromEnv.trim().isEmpty()) {
            return fromEnv.trim();
        }
        final Region region = new DefaultAwsRegionProviderChain().getRegion();
        return region.id();
    }
}
