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

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.neptune.examples.utils.ActivityTimer;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class Batch {
    private final int batchId;
    private final List<KinesisEvent.KinesisEventRecord> records;
    private final Random random = new Random(DateTime.now().getMillis());

    Batch(int batchId, List<KinesisEvent.KinesisEventRecord> records) {
        this.batchId = batchId;
        this.records = records;
    }

    public void writeToNeptune(
            NeptuneClient neptuneClient,
            Parameters parameters,
            Metrics metrics,
            LambdaLogger logger) throws IOException {

        try (ActivityTimer batchTimer = new ActivityTimer(logger, "TOTAL write batch [" + batchId + "]")) {

            AddBatchEdgesQuery query = new AddBatchEdgesQuery(neptuneClient, logger, parameters.conditionalCreate());

            try (ActivityTimer timer = new ActivityTimer(logger, "Parse batch [" + batchId + "]")) {
                for (KinesisEvent.KinesisEventRecord record : records) {

                    String data = new String(record.getKinesis().getData().array());
                    String[] columns = data.split(",");

                    String fromVertexId = columns[0];
                    String toVertexId = columns[1];
                    String creationDate = columns[2];
                    long insertDateTime = DateTime.now().getMillis();

                    query.addEdge(fromVertexId, toVertexId, creationDate, insertDateTime);
                }
            }

            if (random.nextInt(100) < parameters.percentError()) {
                query.provokeError();
            }

            long duration = query.execute(batchId);

            metrics.add(records.size(), duration);
        }
    }
}
