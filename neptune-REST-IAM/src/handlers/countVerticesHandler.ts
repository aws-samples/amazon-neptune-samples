import { TaskHandler } from '../types/TaskHandler';
import { gremlinQuery, GraphTraversalSource } from './utils';

const NEPTUNE_ENDPOINT = process.env.NEPTUNE_ENDPOINT;
const NEPTUNE_PORT = process.env.NEPTUNE_PORT;

const countVerticesHandler: TaskHandler = async (event: any, context: any) => {
  const result = await gremlinQuery<number>(
    NEPTUNE_ENDPOINT,
    NEPTUNE_PORT,
    countVertices,
    getVerticesCount
  );
  console.log('countVertices=', result);
  return result;
};

async function countVertices(g: GraphTraversalSource): Promise<any> {
  return g.V().count().next();
}

function getVerticesCount(result: any) {
  if (result) {
    return result.value;
  }

  console.error(
    'Something went wrong, the query result can not be null or undefined.'
  );
  return -1;
}

export { countVerticesHandler };
