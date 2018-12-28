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
**/

var gremlin = require('gremlin');
var http = require('http');
var url = require('url');

exports.outnodes = [];

exports.handler = function(event, context, callback) {

    var DriverRemoteConnection = gremlin.driver.DriverRemoteConnection;
    var Graph = gremlin.structure.Graph;
    //Use wss:// for secure connections. See https://docs.aws.amazon.com/neptune/latest/userguide/access-graph-ssl.html 
    dc = new DriverRemoteConnection('wss://'+process.env.NEPTUNE_CLUSTER_ENDPOINT+':'+process.env.NEPTUNE_PORT+'/gremlin');
    var graph = new Graph();
    var g = graph.traversal().withRemote(dc);

    const headers = {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'OPTIONS, POST, GET',
        'Access-Control-Max-Age': 2592000, // 30 days
        /** add other headers as per requirement */
        'Access-Control-Allow-Headers' : '*',
        "Content-Type": "application/json"
    };

    console.log("Path Parameters => "+ event.pathParameters);
    console.log("event.pathParameters.proxy => "+ event.pathParameters.proxy);
    console.log(event.pathParameters.proxy.match(/proxy/ig));

    // this code is only for populating the search LoV
    if (event.pathParameters.proxy.match(/initialize/ig)) {
        //using another technique as opposed to creating a new callback function

        g.V().hasLabel('User').limit(1000).valueMap(true).toList().then(
            data => {
            console.log("Response from Neptune for initialize .." + JSON.stringify(data));
        var nodes=[];
        for(var i = 0;    i < data.length;    i++)
        {
            nodes.push({name: data[i].name.toString()});
        }
        var response = {
            statusCode: 200,
            headers: headers,
            body: JSON.stringify(nodes)
        };
        console.log("Initialize call response: " + JSON.stringify(data));
        callback(null, response);
        context.done();
        dc.close(); // look at this carefully!!!
    }).
        catch(error => {
            console.log('ERROR', error);
        dc.close();
    });
    }


    if (event.pathParameters.proxy.match(/search/ig)) {
        g.V().has('name', gremlin.process.P.between(event.queryStringParameters.username, event.queryStringParameters.touser)).limit(20).valueMap(true).toList().then(
            data => {
            console.log(JSON.stringify(data));
            var response = {
            statusCode: 200,
            headers: headers,
            body: JSON.stringify(data)
        };
        console.log("Search call response: " + JSON.stringify(data));
        callback(null, response);
        context.done();
        dc.close(); // look at this carefully!!!
    }).
        catch(error => {
            console.log('ERROR', error);
        dc.close();
    });
    }


    if (event.pathParameters.proxy.match(/neighbours/ig)) {
        g.V().has('User','~id',event.queryStringParameters.id).in_('Follows').valueMap(true).limit(10).toList().then(
            data => {
            console.log(JSON.stringify(data));
        var response = {
            statusCode: 200,
            headers: headers,
            body: JSON.stringify(data)
        };
        console.log("getNeighbours response: " + JSON.stringify(data));
        callback(null, response);
        context.done();
        dc.close();
    }).
        catch(error => {
            console.log('ERROR', error);
        dc.close();
    });

    }


    if (event.pathParameters.proxy.match(/getusertweets/ig)) {
        g.V().has('User', '~id', event.queryStringParameters.userid).out('Tweets').limit(3).valueMap(true).toList().then(
            data => {
        console.log("getusertweets data" + JSON.stringify(data));
        var response = {
            statusCode: 200,
            headers: headers,
            body: JSON.stringify(data)
        };
        console.log("getusertweets response: " + JSON.stringify(data));
        callback(null, response);
        context.done();
        dc.close(); // look at this carefully!!!
        }).
        catch(error => {
            console.log('ERROR', error);
        dc.close();
        });
    }


    if (event.pathParameters.proxy.match(/whichusersliketweet/ig)) {
        g.V().has('Tweet','~id',event.queryStringParameters.tweetid).in_('Likes').hasLabel('User').limit(5).valueMap(true).toList().then(
            data => {
            console.log(JSON.stringify(data));
        var response = {
            statusCode: 200,
            headers: headers,
            body: JSON.stringify(data)
        };
        console.log("getusertweets response: " + JSON.stringify(data));
        callback(null, response);
        context.done();
        dc.close(); // look at this carefully!!!
    }).
        catch(error => {
            console.log('ERROR', error);
        dc.close();
    });
    }


}

