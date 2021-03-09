import { TaskHandler } from '../types/TaskHandler';
import gremlin from 'gremlin-aws-sigv4';
import { gremlinQuery, defaultGremlinOpts } from './utils';

const NEPTUNE_ENDPOINT = process.env.NEPTUNE_ENDPOINT;
const NEPTUNE_PORT = process.env.NEPTUNE_PORT;

interface CountVerticesResult {
  value: number;
  done: boolean;
}

const countVerticesHandler: TaskHandler = async (event: any, context: any) => {
  const result: CountVerticesResult = await gremlinQuery(
    NEPTUNE_ENDPOINT,
    NEPTUNE_PORT,
    defaultGremlinOpts,
    context,
    countVertices,
    getVerticesCount
  );
  console.log('countVertices=', result);
  return result;
};

async function countVertices(
  g: gremlin.driver.AwsSigV4DriverRemoteConnection
): Promise<any> {
  return g.V().count().next();
}

function getVerticesCount(result: CountVerticesResult) {
  if (result) {
    return result.value;
  }

  console.error(
    'Something went wrong, the query result can not be null or undefined.'
  );
  return -1;
}

export { countVerticesHandler };
