import { TaskHandler } from '../types/TaskHandler';
import gremlin from 'gremlin-aws-sigv4';
import {
  gremlinQuery,
  defaultGremlinOpts,
  buildGremlinResponse,
} from './utils';

const NEPTUNE_ENDPOINT = process.env.NEPTUNE_ENDPOINT;
const NEPTUNE_PORT = process.env.NEPTUNE_PORT;

async function addData(
  g: gremlin.driver.AwsSigV4DriverRemoteConnection
): Promise<any> {
  const toWrite = g
    .addV('person')
    .property('name', 'dan')
    .addV('person')
    .property('name', 'mike')
    .addV('person')
    .property('name', 'saikiran');
  return buildGremlinResponse(toWrite);
}

const addDataToNeptuneHandler: TaskHandler = async (
  event: any,
  context: any
) => {
  await gremlinQuery(
    NEPTUNE_ENDPOINT,
    NEPTUNE_PORT,
    defaultGremlinOpts,
    context,
    addData
  );
};

export { addDataToNeptuneHandler };
