/*
 *   Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.tinkerpop.gremlin.driver;

import com.amazon.neptune.gremlin.driver.sigv4.AwsSigV4ClientHandshaker;
import com.amazon.neptune.gremlin.driver.sigv4.ChainedSigV4PropertiesProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.apache.tinkerpop.gremlin.driver.exception.ConnectionException;
import org.apache.tinkerpop.gremlin.driver.handler.WebSocketClientHandler;
import org.apache.tinkerpop.gremlin.driver.handler.WebSocketGremlinRequestEncoder;
import org.apache.tinkerpop.gremlin.driver.handler.WebSocketGremlinResponseDecoder;

import java.util.concurrent.TimeUnit;

public class LBAwareSigV4WebSocketChannelizer extends Channelizer.AbstractChannelizer {
    /**
     * Constant to denote the websocket protocol.
     */
    private static final String WEB_SOCKET = "ws";

    /**
     * Constant to denote the websocket secure protocol.
     */

    private static final String WEB_SOCKET_SECURE = "wss";
    /**
     * Name of the HttpCodec handler.
     */

    private static final String HTTP_CODEC = "http-codec";
    /**
     * Name of the HttpAggregator handler.
     */

    private static final String AGGREGATOR = "aggregator";
    /**
     * Name of the WebSocket handler.
     */

    private static final String WEB_SOCKET_HANDLER = "ws-handler";
    /**
     * Name of the GremlinEncoder handler.
     */

    private static final String GREMLIN_ENCODER = "gremlin-encoder";
    /**
     * Name of the GremlinDecoder handler.
     */
    private static final String GRELIN_DECODER = "gremlin-decoder";

    /**
     * Handshake timeout.
     */
    private static final int HANDSHAKE_TIMEOUT_MILLIS = 15000;

    /**
     * The handler to process websocket messages from the server.
     */
    private WebSocketClientHandler handler;

    /**
     * Encoder to encode websocket requests.
     */
    private WebSocketGremlinRequestEncoder webSocketGremlinRequestEncoder;

    /**
     * Decoder to decode websocket requests.
     */
    private WebSocketGremlinResponseDecoder webSocketGremlinResponseDecoder;

    /**
     * Initializes the channelizer.
     * @param connection the {@link Connection} object.
     */
    @Override
    public void init(final Connection connection) {
        super.init(connection);
        webSocketGremlinRequestEncoder = new WebSocketGremlinRequestEncoder(true, cluster.getSerializer());
        webSocketGremlinResponseDecoder = new WebSocketGremlinResponseDecoder(cluster.getSerializer());
    }

    /**
     * Keep-alive is supported through the ping/pong websocket protocol.
     * @see <a href=https://tools.ietf.org/html/rfc6455#section-5.5.2>IETF RFC 6455</a>
     */
    @Override
    public boolean supportsKeepAlive() {
        return true;
    }

    @Override
    public Object createKeepAliveMessage() {
        return new PingWebSocketFrame();
    }

    /**
     * Sends a {@code CloseWebSocketFrame} to the server for the specified channel.
     */
    @Override
    public void close(final Channel channel) {
        if (channel.isOpen()) {
            channel.writeAndFlush(new CloseWebSocketFrame());
        }
    }

    @Override
    public boolean supportsSsl() {
        final String scheme = connection.getUri().getScheme();
        return "wss".equalsIgnoreCase(scheme);
    }

    @Override
    public void configure(final ChannelPipeline pipeline) {
        final String scheme = connection.getUri().getScheme();
        if (!WEB_SOCKET.equalsIgnoreCase(scheme) && !WEB_SOCKET_SECURE.equalsIgnoreCase(scheme)) {
            throw new IllegalStateException(String.format("Unsupported scheme (only %s: or %s: supported): %s",
                    WEB_SOCKET, WEB_SOCKET_SECURE, scheme));
        }

        if (!supportsSsl() && WEB_SOCKET_SECURE.equalsIgnoreCase(scheme)) {
            throw new IllegalStateException(String.format("To use %s scheme ensure that enableSsl is set to true in "
                            + "configuration",
                    WEB_SOCKET_SECURE));
        }

        final int maxContentLength = cluster.connectionPoolSettings().maxContentLength;
        handler = createHandler();

        pipeline.addLast(HTTP_CODEC, new HttpClientCodec());
        pipeline.addLast(AGGREGATOR, new HttpObjectAggregator(maxContentLength));
        pipeline.addLast(WEB_SOCKET_HANDLER, handler);
        pipeline.addLast(GREMLIN_ENCODER, webSocketGremlinRequestEncoder);
        pipeline.addLast(GRELIN_DECODER, webSocketGremlinResponseDecoder);
    }

    @Override
    public void connected() {
        try {
            // block for a few seconds - if the handshake takes longer 15 seconds than there's gotta be issues with that
            // server. more than likely, SSL is enabled on the server, but the client forgot to enable it or
            // perhaps the server is not configured for websockets.
            handler.handshakeFuture().get(HANDSHAKE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            throw new RuntimeException(new ConnectionException(connection.getUri(),
                    "Could not complete websocket handshake - ensure that client protocol matches server", ex));
        }
    }

    /**
     * Creates an instance of {@link WebSocketClientHandler} with {@link AwsSigV4ClientHandshaker} as the handshaker
     * for SigV4 auth.
     * @return the instance of clientHandler.
     */
    private WebSocketClientHandler createHandler() {

        HandshakeRequestConfig handshakeRequestConfig =
                HandshakeRequestConfig.parse(cluster.authProperties().get(AuthProperties.Property.JAAS_ENTRY));

        WebSocketClientHandshaker handshaker = new LBAwareAwsSigV4ClientHandshaker(
                connection.getUri(),
                WebSocketVersion.V13,
                null,
                false,
                EmptyHttpHeaders.INSTANCE,
                cluster.getMaxContentLength(),
                new ChainedSigV4PropertiesProvider(),
                handshakeRequestConfig);

        return new WebSocketClientHandler(handshaker);
    }
}
