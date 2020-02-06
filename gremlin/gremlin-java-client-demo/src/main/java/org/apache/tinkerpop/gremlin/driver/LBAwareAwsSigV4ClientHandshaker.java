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

package org.apache.tinkerpop.gremlin.driver;

import com.amazon.neptune.gremlin.driver.sigv4.ChainedSigV4PropertiesProvider;
import com.amazon.neptune.gremlin.driver.sigv4.SigV4Properties;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.neptune.auth.NeptuneNettyHttpSigV4Signer;
import com.amazonaws.neptune.auth.NeptuneSigV4SignerException;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker13;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;

import java.net.URI;

public class LBAwareAwsSigV4ClientHandshaker extends WebSocketClientHandshaker13 {

    private final ChainedSigV4PropertiesProvider sigV4PropertiesProvider;
    private final HandshakeRequestConfig handshakeRequestConfig;
    private final SigV4Properties sigV4Properties;

    public LBAwareAwsSigV4ClientHandshaker(URI webSocketURL,
                                           WebSocketVersion version,
                                           String subprotocol,
                                           boolean allowExtensions,
                                           HttpHeaders customHeaders,
                                           int maxFramePayloadLength,
                                           ChainedSigV4PropertiesProvider sigV4PropertiesProvider,
                                           HandshakeRequestConfig handshakeRequestConfig) {

        super(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength);

        this.sigV4PropertiesProvider = sigV4PropertiesProvider;
        this.handshakeRequestConfig = handshakeRequestConfig;
        this.sigV4Properties = this.loadProperties();
    }

    protected FullHttpRequest newHandshakeRequest() {
        FullHttpRequest request = super.newHandshakeRequest();

        request.headers().remove("Host");
        request.headers().add("Host", handshakeRequestConfig.chooseHostHeader());

        try {

            NeptuneNettyHttpSigV4Signer sigV4Signer = new NeptuneNettyHttpSigV4Signer(
                    this.sigV4Properties.getServiceRegion(),
                    new DefaultAWSCredentialsProviderChain());

            sigV4Signer.signRequest(request);

            if (handshakeRequestConfig.removeHostHeaderAfterSigning()) {
                request.headers().remove("Host");
            }

            return request;

        } catch (NeptuneSigV4SignerException e) {
            throw new RuntimeException("Exception occurred while signing the request", e);
        }
    }

    private SigV4Properties loadProperties() {
        return this.sigV4PropertiesProvider.getSigV4Properties();
    }
}

