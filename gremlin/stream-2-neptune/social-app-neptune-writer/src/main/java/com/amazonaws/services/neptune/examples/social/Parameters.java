/*
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.

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

package com.amazonaws.services.neptune.examples.social;

import com.amazonaws.services.neptune.examples.utils.EnvironmentVariableUtils;

public class Parameters {

    private final String neptuneEndpoint;
    private final int percentError;
    private final int batchWriteSize;
    private final boolean conditionalCreate;
    private final String clusterId;
    private final boolean wait;

    public Parameters(){
        neptuneEndpoint = EnvironmentVariableUtils.getMandatoryEnv("neptune_endpoint");
        percentError = Integer.parseInt(EnvironmentVariableUtils.getOptionalEnv("percent_error", "0"));
        batchWriteSize = Integer.parseInt(EnvironmentVariableUtils.getOptionalEnv("batch_write_size", "100"));
        conditionalCreate = Boolean.parseBoolean(EnvironmentVariableUtils.getOptionalEnv("conditional_create", "true"));
        clusterId = EnvironmentVariableUtils.getMandatoryEnv("cluster_id");
        wait = Boolean.parseBoolean(EnvironmentVariableUtils.getOptionalEnv("wait", "true"));
    }

    public String neptuneEndpoint() {
        return neptuneEndpoint;
    }

    public int percentError() {
        return percentError;
    }

    public int batchWriteSize() {
        return batchWriteSize;
    }

    public boolean conditionalCreate() {
        return conditionalCreate;
    }

    public String clusterId() {
        return clusterId;
    }

    public boolean pause() {
        return wait;
    }
}
