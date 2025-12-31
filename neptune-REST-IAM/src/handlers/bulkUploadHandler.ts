import { TaskHandler } from '../types/TaskHandler';
import { S3Event } from 'aws-lambda';
import { neptunePost, buildBulkUploadBody } from './utils';

const bulkUploadHandler: TaskHandler = async (event: S3Event): Promise<any> => {
  console.debug(
    `Tries to process the event: ${JSON.stringify(event, null, 2)}`
  );

  for (const record of event.Records) {
    const s3 = record.s3;
    const bucket = s3.bucket.name;
    const key = s3.object.key;
    const body = buildBulkUploadBody(
      bucket,
      key,
      process.env.FORMAT,
      process.env.AWS_DEFAULT_REGION,
      process.env.IAM_ROLE_ARN
    );
    try {
      const response = await neptunePost(
        process.env.NEPTUNE_ENDPOINT,
        process.env.NEPTUNE_PORT,
        process.env.AWS_DEFAULT_REGION,
        '/loader',
        body
      );
      console.log(
        `File ${key} was processed with response response ${JSON.stringify(
          response,
          null,
          2
        )}.`
      );
    } catch (e) {
      console.error(`File ${key} can not be processed.`);
      console.error(e);
    }
  }
};

export { bulkUploadHandler };
