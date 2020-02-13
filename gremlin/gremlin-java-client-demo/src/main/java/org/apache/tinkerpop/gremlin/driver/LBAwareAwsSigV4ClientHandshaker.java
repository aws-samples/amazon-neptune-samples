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

