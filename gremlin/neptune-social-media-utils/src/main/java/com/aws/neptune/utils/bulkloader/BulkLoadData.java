/*******************************************************************************
 *   Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/

package com.aws.neptune.utils.bulkloader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

public class BulkLoadData {

	
	public HttpResponse sendLoadDataRequest(String neptuneEndpoint, String port, String iamRoleArn, String bucketFolder, String region) throws Exception
	{
		String payload = 
				"{\"source\": \"s3://" + bucketFolder + "/\", " +
                "\"format\": \"csv\", " +
                "\"iamRoleArn\": \""+ iamRoleArn + "\"," +
                "\"region\": \"" + region + "\", " +
                "\"failOnError\": \"FALSE\" " +
                "}";
		System.out.println("Payload => \n"+payload);
		
        StringEntity entity = new StringEntity(payload,
        		ContentType.create("application/json"));

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("https://"+neptuneEndpoint+":"+port+"/loader");
        request.setHeader("Content-Type", "application/json");
        request.setEntity(entity);

        System.out.println("Request => \n"+request);
        
        HttpResponse response = httpClient.execute(request);
        System.out.println(response.getStatusLine().getStatusCode());
        return response;
        
	}
	
	 public static void main(String[] args) throws Exception {
	        if (null == args || args.length < 2) {
	            System.err.println("Usage: BulkLoadData <neptune-endpoint> <port> <iam-s3-role-arn> <s3-bucket-name>/<folder-name> <aws-region-code>");
	            System.exit(1);
	        }
	        
	        //System.out.println(args.length);
	        //System.out.println("Arguments: "+ args[0] + " " + args[1]);
	        HttpResponse response = new BulkLoadData().sendLoadDataRequest(args[0],  args[1], args[2], args[3], args[4]);
	        
	        System.out.println(response);
	        
	 }
	 
}
