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

import org.joda.time.DateTime;

import java.util.*;

public class HandshakeRequestConfig {

    public static HandshakeRequestConfigBuilder builder(){
        return new HandshakeRequestConfigBuilder();
    }

    public static HandshakeRequestConfig parse(String s) {
        String[] values = s.split(",");

        boolean removeHostHeaderAfterSigning = Boolean.parseBoolean(values[0]);
        int port = Integer.parseInt(values[1]);

        Collection<String> endpoints = new ArrayList<>(Arrays.asList(values).subList(2, values.length));

        return new HandshakeRequestConfig(endpoints, port, removeHostHeaderAfterSigning);
    }

    private final List<String> endpoints;
    private final int port;
    private final boolean removeHostHeaderAfterSigning;
    private final Random random = new Random(DateTime.now().getMillis());

    public HandshakeRequestConfig(Collection<String> endpoints, int port, boolean removeHostHeaderAfterSigning) {
        this.endpoints = new ArrayList<>(endpoints);
        this.port = port;
        this.removeHostHeaderAfterSigning = removeHostHeaderAfterSigning;
    }

    public String chooseHostHeader() {
        return String.format("%s:%s", endpoints.get(random.nextInt(endpoints.size())), port);
    }

    public boolean removeHostHeaderAfterSigning() {
        return removeHostHeaderAfterSigning;
    }

    public String value() {
        return String.format("%s,%s,%s", removeHostHeaderAfterSigning, port, String.join(",", endpoints));
    }

    @Override
    public String toString() {
        return value();
    }

    public static final class HandshakeRequestConfigBuilder {

        private final List<String> endpoints = new ArrayList<>();
        private int port;
        private boolean removeHostHeaderAfterSigning = false;

        public HandshakeRequestConfigBuilder addNeptuneEndpoints(List<String> endpoints){
            this.endpoints.addAll(endpoints);
            return this;
        }

        public HandshakeRequestConfigBuilder setNeptunePort(int port){
            this.port = port;
            return this;
        }

        public HandshakeRequestConfigBuilder removeHostHeaderAfterSigning(){
            this.removeHostHeaderAfterSigning = true;
            return this;
        }

        public HandshakeRequestConfig build(){
            return new HandshakeRequestConfig(endpoints, port, removeHostHeaderAfterSigning);
        }
    }
}
