import { TaskHandler } from '../types/TaskHandler';
import gremlin from 'gremlin-aws-sigv4';
import { gremlinQuery, defaultGremlinOpts } from './utils';

const NEPTUNE_ENDPOINT = process.env.NEPTUNE_ENDPOINT;
const NEPTUNE_PORT = process.env.NEPTUNE_PORT;

const dropAllHandler: TaskHandler = async (event: any, context: any) => {
  await gremlinQuery(
    NEPTUNE_ENDPOINT,
    NEPTUNE_PORT,
    defaultGremlinOpts,
    context,
    dropAllVertices
  );
};

async function dropAllVertices(
  g: gremlin.driver.AwsSigV4DriverRemoteConnection
): Promise<any> {
  return g.V().drop().iterate();
}

export { dropAllHandler };
