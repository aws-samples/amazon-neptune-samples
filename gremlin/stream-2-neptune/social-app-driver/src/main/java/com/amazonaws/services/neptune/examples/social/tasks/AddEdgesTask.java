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

package com.amazonaws.services.neptune.examples.social.tasks;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class AddEdgesTask extends TimerTask {

    private final Timer timer;
    private final Iterator<String> edgesIterator;
    private final KinesisClient kinesisClient;
    private final CountDownLatch waitHandle;
    private final int batchSize;

    public AddEdgesTask(Timer timer,
                        Iterator<String> edgesIterator,
                        KinesisClient kinesisClient,
                        CountDownLatch waitHandle,
                        int batchSize,
                        int numberOfWorkers) {

        this.timer = timer;
        this.edgesIterator = edgesIterator;
        this.kinesisClient = kinesisClient;
        this.waitHandle = waitHandle;
        this.batchSize = batchSize;
    }

    @Override
    public void run() {

        List<EdgeInfo> edges = new ArrayList<>();
        int i = 0;

        while (edgesIterator.hasNext() && i < batchSize) {

            String line = edgesIterator.next();

            String[] columns = line.split(",");
            String fromVertexId = clean(columns[2]);
            String toVertexId = clean(columns[3]);
            String creationDate = clean(columns[4]);

            EdgeInfo edgeInfo = new EdgeInfo(fromVertexId, toVertexId, creationDate);
            edges.add(edgeInfo);

            i++;
        }

        if (edges.isEmpty()) {
            timer.cancel();
            waitHandle.countDown();
        } else {
            kinesisClient.publishToStream(edges);
        }
    }

    private String clean(String s){
        return s.trim().replace("\"", "");
    }

}