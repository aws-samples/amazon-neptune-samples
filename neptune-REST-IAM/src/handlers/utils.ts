import gremlin from 'gremlin';
import { SignatureV4 } from '@smithy/signature-v4';
import { Hash } from '@smithy/hash-node';
import { fromNodeProviderChain } from '@aws-sdk/credential-providers';
import { HttpRequest } from '@smithy/protocol-http';

const { Graph } = gremlin.structure;
const { DriverRemoteConnection } = gremlin.driver;
const { traversal } = gremlin.process;

type GraphTraversalSource = InstanceType<typeof gremlin.process.GraphTraversalSource>;

async function getNeptuneSigV4Headers(
  endpoint: string,
  port: string,
  region: string
): Promise<Record<string, string>> {
  const credentials = fromNodeProviderChain({ clientConfig: { region } });
  const signer = new SignatureV4({
    service: 'neptune-db',
    region,
    sha256: Hash.bind(null, 'sha256'),
    credentials,
  });

  const request = new HttpRequest({
    method: 'GET',
    protocol: 'https:',
    hostname: endpoint,
    port: Number(port),
    path: '/gremlin',
    headers: {
      host: `${endpoint}:${port}`,
    },
  });

  const signed = await signer.sign(request);
  return signed.headers as Record<string, string>;
}

const gremlinQuery = async <T>(
  neptuneEndpoint: string,
  neptunePort: string,
  runInContext: (g: GraphTraversalSource) => Promise<any>,
  transform?: (result: any) => T
): Promise<T> => {
  const region = process.env.AWS_REGION || process.env.AWS_DEFAULT_REGION || 'us-east-1';
  const headers = await getNeptuneSigV4Headers(neptuneEndpoint, neptunePort, region);
  const url = `wss://${neptuneEndpoint}:${neptunePort}/gremlin`;

  const connection = new DriverRemoteConnection(url, {
    headers,
    rejectUnauthorized: true,
  });

  try {
    await connection.open();
    const g = traversal().with_(connection);
    const result = await runInContext(g);
    console.log(`Query result: ${JSON.stringify(result, null, 2)}`);
    const transformedResult = transform ? transform(result) : result;
    return transformedResult as T;
  } finally {
    await connection.close();
  }
};

const buildGremlinResponse = (queryResult: any): Promise<any> => {
  return queryResult.next().then((result: any) => result.value);
};

async function signRequest(
  method: string,
  endpoint: string,
  port: string,
  region: string,
  path: string,
  body?: string
): Promise<Record<string, string>> {
  const credentials = fromNodeProviderChain({ clientConfig: { region } });
  const signer = new SignatureV4({
    service: 'neptune-db',
    region,
    sha256: Hash.bind(null, 'sha256'),
    credentials,
  });

  const request = new HttpRequest({
    method,
    protocol: 'https:',
    hostname: endpoint,
    port: Number(port),
    path,
    headers: {
      host: `${endpoint}:${port}`,
      'content-type': 'application/json',
    },
    body,
  });

  const signed = await signer.sign(request);
  return signed.headers as Record<string, string>;
}

const neptuneGet = async (
  neptuneEndpoint: string,
  neptunePort: string,
  neptuneRegion: string,
  path: string
) => {
  const headers = await signRequest('GET', neptuneEndpoint, neptunePort, neptuneRegion, path);
  const response = await fetch(
    `https://${neptuneEndpoint}:${neptunePort}${path}`,
    { method: 'GET', headers }
  );
  const data = await response.json();
  return {
    data,
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
  const bodyStr = JSON.stringify(body);
  const headers = await signRequest('POST', neptuneEndpoint, neptunePort, neptuneRegion, path, bodyStr);
  const response = await fetch(
    `https://${neptuneEndpoint}:${neptunePort}${path}`,
    { method: 'POST', headers, body: bodyStr }
  );
  const data = await response.json();
  return {
    data,
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
  buildGremlinResponse,
  neptuneGet,
  neptunePost,
  buildBulkUploadBody,
};
export type { GraphTraversalSource };
