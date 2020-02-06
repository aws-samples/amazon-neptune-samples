package org.apache.tinkerpop.gremlin.driver;

import io.netty.handler.ssl.SslContext;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;

import java.util.ArrayList;
import java.util.List;

public class NeptuneClusterBuilder {

    public static NeptuneClusterBuilder build() {
        return new NeptuneClusterBuilder();
    }

    private final Cluster.Builder innerBuilder = Cluster.build();

    private List<String> addresses = new ArrayList<>();
    private String networkLoadBalancerEndpoint;
    private String applicationLoadBalancerEndpoint;
    private boolean enableIamAuth = false;
    private int port = 8182;
    private int loadBalancerPort = 80;

    private NeptuneClusterBuilder() {
    }

    public NeptuneClusterBuilder nioPoolSize(final int nioPoolSize) {
        innerBuilder.nioPoolSize(nioPoolSize);
        return this;
    }

    public NeptuneClusterBuilder workerPoolSize(final int workerPoolSize) {
        innerBuilder.workerPoolSize(workerPoolSize);
        return this;
    }

    public NeptuneClusterBuilder path(final String path) {
        innerBuilder.path(path);
        return this;
    }

    public NeptuneClusterBuilder serializer(final String mimeType) {
        innerBuilder.serializer(mimeType);
        return this;
    }

    public NeptuneClusterBuilder serializer(final Serializers mimeType) {
        innerBuilder.serializer(mimeType);
        return this;
    }

    public NeptuneClusterBuilder serializer(final MessageSerializer serializer) {
        innerBuilder.serializer(serializer);
        return this;
    }

    public NeptuneClusterBuilder enableSsl(final boolean enable) {
        innerBuilder.enableSsl(enable);
        return this;
    }

    public NeptuneClusterBuilder sslContext(final SslContext sslContext) {
        innerBuilder.sslContext(sslContext);
        return this;
    }

    public NeptuneClusterBuilder keepAliveInterval(final long keepAliveInterval) {
        innerBuilder.keepAliveInterval(keepAliveInterval);
        return this;
    }

    public NeptuneClusterBuilder keyStore(final String keyStore) {
        innerBuilder.keyStore(keyStore);
        return this;
    }

    public NeptuneClusterBuilder keyStorePassword(final String keyStorePassword) {
        innerBuilder.keyStorePassword(keyStorePassword);
        return this;
    }

    public NeptuneClusterBuilder trustStore(final String trustStore) {
        innerBuilder.trustStore(trustStore);
        return this;
    }

    public NeptuneClusterBuilder trustStorePassword(final String trustStorePassword) {
        innerBuilder.trustStorePassword(trustStorePassword);
        return this;
    }

    public NeptuneClusterBuilder keyStoreType(final String keyStoreType) {
        innerBuilder.keyStoreType(keyStoreType);
        return this;
    }

    public NeptuneClusterBuilder sslEnabledProtocols(final List<String> sslEnabledProtocols) {
        innerBuilder.sslEnabledProtocols(sslEnabledProtocols);
        return this;
    }

    public NeptuneClusterBuilder sslCipherSuites(final List<String> sslCipherSuites) {
        innerBuilder.sslCipherSuites(sslCipherSuites);
        return this;
    }

    public NeptuneClusterBuilder sslSkipCertValidation(final boolean sslSkipCertValidation) {
        innerBuilder.sslSkipCertValidation(sslSkipCertValidation);
        return this;
    }

    public NeptuneClusterBuilder minInProcessPerConnection(final int minInProcessPerConnection) {
        innerBuilder.minInProcessPerConnection(minInProcessPerConnection);
        return this;
    }

    public NeptuneClusterBuilder maxInProcessPerConnection(final int maxInProcessPerConnection) {
        innerBuilder.maxInProcessPerConnection(maxInProcessPerConnection);
        return this;
    }

    public NeptuneClusterBuilder maxSimultaneousUsagePerConnection(final int maxSimultaneousUsagePerConnection) {
        innerBuilder.maxSimultaneousUsagePerConnection(maxSimultaneousUsagePerConnection);
        return this;
    }

    public NeptuneClusterBuilder minSimultaneousUsagePerConnection(final int minSimultaneousUsagePerConnection) {
        innerBuilder.minSimultaneousUsagePerConnection(minSimultaneousUsagePerConnection);
        return this;
    }

    public NeptuneClusterBuilder maxConnectionPoolSize(final int maxSize) {
        innerBuilder.maxConnectionPoolSize(maxSize);
        return this;
    }

    public NeptuneClusterBuilder minConnectionPoolSize(final int minSize) {
        innerBuilder.minConnectionPoolSize(minSize);
        return this;
    }

    public NeptuneClusterBuilder resultIterationBatchSize(final int size) {
        innerBuilder.resultIterationBatchSize(size);
        return this;
    }

    public NeptuneClusterBuilder maxWaitForConnection(final int maxWait) {
        innerBuilder.maxWaitForConnection(maxWait);
        return this;
    }

    public NeptuneClusterBuilder maxWaitForSessionClose(final int maxWait) {
        innerBuilder.maxWaitForSessionClose(maxWait);
        return this;
    }

    public NeptuneClusterBuilder maxContentLength(final int maxContentLength) {
        innerBuilder.maxContentLength(maxContentLength);
        return this;
    }

    public NeptuneClusterBuilder channelizer(final String channelizerClass) {
        innerBuilder.channelizer(channelizerClass);
        return this;
    }

    public NeptuneClusterBuilder channelizer(final Class channelizerClass) {
        return channelizer(channelizerClass.getCanonicalName());
    }

    public NeptuneClusterBuilder validationRequest(final String script) {
        innerBuilder.validationRequest(script);
        return this;
    }

    public NeptuneClusterBuilder reconnectInterval(final int interval) {
        innerBuilder.reconnectInterval(interval);
        return this;
    }

    public NeptuneClusterBuilder loadBalancingStrategy(final LoadBalancingStrategy loadBalancingStrategy) {
        innerBuilder.loadBalancingStrategy(loadBalancingStrategy);
        return this;
    }

    public NeptuneClusterBuilder authProperties(final AuthProperties authProps) {
        innerBuilder.authProperties(authProps);
        return this;
    }

    public NeptuneClusterBuilder credentials(final String username, final String password) {
        innerBuilder.credentials(username, password);
        return this;
    }

    public NeptuneClusterBuilder protocol(final String protocol) {
        innerBuilder.protocol(protocol);
        return this;
    }

    public NeptuneClusterBuilder jaasEntry(final String jaasEntry) {
        innerBuilder.jaasEntry(jaasEntry);
        return this;
    }

    public NeptuneClusterBuilder addContactPoint(final String address) {
        this.addresses.add(address);
        return this;
    }

    public NeptuneClusterBuilder addContactPoints(final String... addresses) {
        for (String address : addresses)
            addContactPoint(address);
        return this;
    }

    public NeptuneClusterBuilder addContactPoints(final List<String> addresses) {
        for (String address : addresses)
            addContactPoint(address);
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

        if (addresses.isEmpty()) {
            if (isDirectConnection()) {
                throw new IllegalArgumentException("You must supply one or more Neptune endpoints");
            } else if (enableIamAuth) {
                throw new IllegalArgumentException("You must supply one or more Neptune endpoints to sign the Host header");
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
            } else if (applicationLoadBalancerEndpoint != null) {
                innerBuilder.addContactPoint(applicationLoadBalancerEndpoint);
            }
        }

        if (enableIamAuth) {

            if (isDirectConnection()) {
                innerBuilder.channelizer(SigV4WebSocketChannelizer.class);
            } else {

                HandshakeRequestConfig.HandshakeRequestConfigBuilder handshakeRequestConfigBuilder =
                        HandshakeRequestConfig.builder()
                                .addNeptuneEndpoints(addresses)
                                .setNeptunePort(port);

                if (applicationLoadBalancerEndpoint != null) {
                    handshakeRequestConfigBuilder.removeHostHeaderAfterSigning();
                }

                HandshakeRequestConfig handshakeRequestConfig = handshakeRequestConfigBuilder.build();

                innerBuilder
                        // We're using the JAAS_ENTRY auth property to tunnel Host header info to the channelizer
                        .jaasEntry(handshakeRequestConfig.value())
                        .channelizer(LBAwareSigV4WebSocketChannelizer.class);
            }
        }

        return innerBuilder.create();
    }
}
