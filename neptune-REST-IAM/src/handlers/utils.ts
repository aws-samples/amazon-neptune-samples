import gremlin from 'gremlin-aws-sigv4';
import axios from 'axios';
import { aws4Interceptor } from 'aws4-axios';

const defaultGremlinOpts = { secure: true, autoReconnect: true, maxRetry: 3 };

const gremlinQuery = <T>(
  neptuneEndpoint: string,
  neptunePort: string,
  neptuneOpts: any,
  context: any,
  runInContext: any,
  transform?: any
): Promise<T> => {
  return new Promise<T>((success, failure) => {
    const graph = new gremlin.structure.Graph();
    let result = null;
    const connection = new gremlin.driver.AwsSigV4DriverRemoteConnection(
      neptuneEndpoint,
      neptunePort,
      neptuneOpts,
      // connected callback
      async () => {
        const g = graph.traversal().withRemote(connection);
        result = await runInContext(g);
        console.log(`Query result: ${JSON.stringify(result, null, 2)}`);
        // WebSocket is not open: readyState 2 (CLOSING)
        await connection.close();
      },
      // disconnected callback
      (code: any, message: any) => {
        console.log(code, message);
        const transformResult = transform ? transform(result) : result;
        success(transformResult);
        context.succeed(transformResult);
      },
      // error callback
      (error: any) => {
        console.log(error);
        context.fail(error);
        failure(error);
      }
    );
  });
};

const buildGremlinResponse = (queryResult: any): Promise<any> => {
  return new Promise((resolve, reject) => {
    const results = queryResult.toList();
    results
      .then((results: any) => {
        const resultSet: gremlin.driver.ResultSet = new gremlin.driver.ResultSet(
          results
        );
        const created: gremlin.structure.Vertex = <gremlin.structure.Vertex>(
          (<unknown>resultSet.first())
        );
        resolve(created);
      })
      .catch((err: any) => {
        console.log(err);
        reject(err);
      });
  });
};

const neptuneGet = async (
  neptuneEndpoint: string,
  neptunePort: string,
  neptuneRegion: string,
  path: string
) => {
  const interceptor = aws4Interceptor({
    region: neptuneRegion,
    service: 'neptune-db',
  });
  axios.interceptors.request.use(interceptor);
  const response = await axios.get(
    `https://${neptuneEndpoint}:${neptunePort}${path}`
  );
  return {
    data: response.data,
    status: response.status,
    statusText: response.statusText,
  };
};

const neptunePost = async (
  neptuneEndpoint: string,
  neptunePort: string,
  neptuneRegion: string,
  path: string,
  body: any
) => {
  const interceptor = aws4Interceptor({
    region: neptuneRegion,
    service: 'neptune-db',
  });
  axios.interceptors.request.use(interceptor);
  const response = await axios.post(
    `https://${neptuneEndpoint}:${neptunePort}${path}`,
    body
  );
  return {
    data: response.data,
    status: response.status,
    statusText: response.statusText,
  };
};

const buildBulkUploadBody = (
  bucket: string,
  file: string,
  fileFormat: string,
  neptuneRegion: string,
  s3UpdateRoleArn: string
) => {
  return {
    source: `s3://${bucket}/${file}`,
    format: fileFormat,
    region: neptuneRegion,
    iamRoleArn: s3UpdateRoleArn,
    mode: 'AUTO',
    parallelism: 'OVERSUBSCRIBE',
    updateSingleCardinalityProperties: 'TRUE',
    parserConfiguration: {
      baseUri: 'http://aws.amazon.com/neptune/default',
      namedGraphUri:
        'http://aws.amazon.com/neptune/vocab/v01/DefaultNamedGraph',
    },
  };
};

export {
  gremlinQuery,
  defaultGremlinOpts,
  buildGremlinResponse,
  neptuneGet,
  neptunePost,
  buildBulkUploadBody,
};
