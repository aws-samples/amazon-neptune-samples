import { TaskHandler } from '../types/TaskHandler';
import { neptuneGet } from './utils';

const getClusterStatus: TaskHandler = async (event: any): Promise<any> => {
  console.debug(
    `Tries to process the event: ${JSON.stringify(event, null, 2)}`
  );
  return await neptuneGet(
    process.env.NEPTUNE_ENDPOINT,
    process.env.NEPTUNE_PORT,
    process.env.AWS_DEFAULT_REGION,
    '/status'
  );
};

export { getClusterStatus };
