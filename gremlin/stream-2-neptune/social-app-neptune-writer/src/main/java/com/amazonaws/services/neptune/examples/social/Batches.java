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

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Batches implements Iterable<Batch> {

    private final int totalNumberRecords;
    private final List<Batch> batches;

    public Batches(List<KinesisEvent.KinesisEventRecord> records, int batchSize) {
        totalNumberRecords = records.size();
        batches = partition(records, batchSize);
    }

    private List<Batch> partition(List<KinesisEvent.KinesisEventRecord> list, int batchSize) {
        List<Batch> parts = new ArrayList<>();
        int originalListSize = list.size();
        int batchId = 0;
        for (int i = 0; i < originalListSize; i += batchSize) {
            parts.add(new Batch(batchId, new ArrayList<>(
                    list.subList(i, Math.min(originalListSize, i + batchSize))))
            );
            batchId++;
        }
        return parts;
    }

    public int totalNumberRecords() {
        return totalNumberRecords;
    }

    public int size(){
        return batches.size();
    }

    @Override
    public Iterator<Batch> iterator() {
        return batches.iterator();
    }

}
