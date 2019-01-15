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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AddEdgesTaskMulithreaded extends TimerTask {

    private final Timer timer;
    private final Iterator<String> edgesIterator;
    private final KinesisClient kinesisClient;
    private final CountDownLatch waitHandle;
    private final int batchSize;
    private final int numberOfWorkers;
    private final ExecutorService executorService;

    public AddEdgesTaskMulithreaded(Timer timer,
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
        this.numberOfWorkers = numberOfWorkers;

        executorService = Executors.newFixedThreadPool(numberOfWorkers);
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
            executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            waitHandle.countDown();
        } else {

            List<List<EdgeInfo>> partitions = partition(edges, (int) Math.ceil(batchSize/numberOfWorkers));

            for (List<EdgeInfo> p : partitions) {
                executorService.submit(() -> kinesisClient.publishToStream(p));
            }
        }
    }

    private String clean(String s) {
        return s.trim().replace("\"", "");
    }

    private List<List<EdgeInfo>> partition(List<EdgeInfo> list, int batchSize) {
        List<List<EdgeInfo>> parts = new ArrayList<>();
        int originalListSize = list.size();
        for (int i = 0; i < originalListSize; i += batchSize) {
            parts.add(new ArrayList<>(
                    list.subList(i, Math.min(originalListSize, i + batchSize)))
            );
        }
        return parts;
    }
}
