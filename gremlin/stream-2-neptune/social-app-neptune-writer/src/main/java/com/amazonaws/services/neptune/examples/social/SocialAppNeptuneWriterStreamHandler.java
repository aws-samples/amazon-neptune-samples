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

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.neptune.examples.utils.ActivityTimer;

public class SocialAppNeptuneWriterStreamHandler implements RequestHandler<KinesisEvent, Void> {

    @Override
    public Void handleRequest(KinesisEvent event, Context context) {

        LambdaLogger logger = context.getLogger();
        Parameters parameters = new Parameters();

        if (parameters.pause()) {
            throw new RuntimeException("Waiting... [Set 'wait' Lambda environment variable to 'false' to continue processing]");
        }

        try (ActivityTimer timer = new ActivityTimer(logger, "All code")) {
            try (NeptuneClient neptuneClient = new NeptuneClient(parameters.neptuneEndpoint(), logger)) {

                Metrics metrics = new Metrics(parameters.clusterId(), logger);
                Batches batches = new Batches(event.getRecords(), parameters.batchWriteSize());

                logger.log("Number of records: " + batches.totalNumberRecords() + " [" + context.getAwsRequestId() + "]");
                logger.log("Number of batches: " + batches.size() + " [" + context.getAwsRequestId() + "]");

                for (Batch batch : batches) {
                    batch.writeToNeptune(neptuneClient, parameters, metrics, logger);
                }

                metrics.publish();

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        return null;
    }
}
