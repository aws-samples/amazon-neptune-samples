/*
 * MIT License

* Copyright (c) 2018.  Amazon Web Services, Inc. All Rights Reserved.

* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:

* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.

* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
* */

const gremlin = require('gremlin');

const formatResponse = payload => {
  return {
    statusCode: 200,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'OPTIONS, POST, GET',
      'Access-Control-Max-Age': 2592000, // 30 days
      'Access-Control-Allow-Headers': '*',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  };
}

exports.handler = async event => {
  const {DriverRemoteConnection} = gremlin.driver;
  const {Graph} = gremlin.structure;
  // Use wss:// for secure connections. See https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-ssl.html
  const dc = new DriverRemoteConnection(
    `wss://${process.env.NEPTUNE_CLUSTER_ENDPOINT}:${process.env.NEPTUNE_PORT}/gremlin`,
    {mimeType: 'application/vnd.gremlin-v2.0+json'}
  );
  const graph = new Graph();
  const g = graph.traversal().withRemote(dc);
  const withTokens = '~tinkerpop.valueMap.tokens';

  try {
    let data = [];
    const {
      id, username, touser, userid, tweetid
    } = event.queryStringParameters || {};

    if (event.pathParameters.proxy.match(/initialize/ig)) {
      const nodes = await g.V()
        .hasLabel('User')
        .limit(1000)
        .valueMap()
        .with_(withTokens)
        .toList();
      data = nodes.map(row => ({name: row.name.toString()}));
    } else if (event.pathParameters.proxy.match(/search/ig)) {
      data = await g.V().has('name', gremlin.process.P.between(username, touser))
        .limit(20)
        .valueMap()
        .with_(withTokens)
        .toList();
    } else if (event.pathParameters.proxy.match(/neighbours/ig)) {
      data = await g.V().has('User', '~id', id)
        .in_('Follows')
        .valueMap()
        .with_(withTokens)
        .limit(10)
        .toList();
    } else if (event.pathParameters.proxy.match(/getusertweets/ig)) {
      data = await g.V().has('User', '~id', userid)
        .out('Tweets')
        .limit(3)
        .valueMap()
        .with_(withTokens)
        .toList();
    } else if (event.pathParameters.proxy.match(/whichusersliketweet/ig)) {
      data = await g.V().has('Tweet', '~id', tweetid)
        .in_('Likes')
        .hasLabel('User')
        .limit(5)
        .valueMap()
        .with_(withTokens)
        .toList();
    }

    dc.close();
    return formatResponse(data);
  } catch (error) {
    console.log('ERROR', error);
    dc.close();
  }
};
