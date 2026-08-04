import { TaskHandler } from '../types/TaskHandler';
import { gremlinQuery, GraphTraversalSource } from './utils';

const NEPTUNE_ENDPOINT = process.env.NEPTUNE_ENDPOINT;
const NEPTUNE_PORT = process.env.NEPTUNE_PORT;

const dropAllHandler: TaskHandler = async (event: any, context: any) => {
  await gremlinQuery(NEPTUNE_ENDPOINT, NEPTUNE_PORT, dropAllVertices);
};

async function dropAllVertices(g: GraphTraversalSource): Promise<any> {
  return g.V().drop().iterate();
}

export { dropAllHandler };
