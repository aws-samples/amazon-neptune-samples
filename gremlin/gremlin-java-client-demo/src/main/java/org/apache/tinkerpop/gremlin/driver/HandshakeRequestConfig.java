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
