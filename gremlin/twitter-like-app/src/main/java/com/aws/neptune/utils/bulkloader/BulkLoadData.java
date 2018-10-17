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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.commons.io.IOUtils;
import java.io.InputStream;
import org.json.JSONObject;
import org.json.JSONArray;

public class BulkLoadData {

	
	public String sendLoadDataRequest(String neptuneEndpoint, String iamRoleArn, String s3BucketAndKey, String regionCode) throws Exception
	{
		String payload = 
				"{\"source\": \"s3://"+ s3BucketAndKey + "\", " +
                "\"format\": \"csv\", " +
                "\"iamRoleArn\": \""+ iamRoleArn +"\"," +
                "\"region\": \""+ regionCode + "\", " +
                "\"failOnError\": \"FALSE\" " +
                "}";
		//System.out.println("Payload => \n"+payload);
		String loadId = null;
		
        StringEntity entity = new StringEntity(payload,
        		ContentType.create("application/json"));

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("http://"+neptuneEndpoint+"/loader");
        request.setHeader("Content-Type", "application/json");
        request.setEntity(entity);

        //System.out.println("Request => \n"+request);
        
        HttpResponse response = httpClient.execute(request);
        //System.out.println(response.getStatusLine().getStatusCode());
        
        String jsonResponse = IOUtils.toString((InputStream)response.getEntity().getContent(), (String) null);
        
       if(response.getStatusLine().getStatusCode() == 200)
       {
        JSONObject robj= new JSONObject(jsonResponse);
        String jobStatus= robj.getString("status");
        JSONObject payload1 = robj.getJSONObject("payload");
        loadId= payload1.getString("loadId");
        }
       else
       {
    	   loadId="";
    	   throw new Exception(jsonResponse);  
       }
       
        return loadId;
        
	}
	
	public void pollForCompletion(String neptuneEndpoint, String loadId) throws Exception
	{ 
		 HttpClient httpClient = HttpClientBuilder.create().build();
		 HttpGet request = new HttpGet("http://"+neptuneEndpoint+"/loader?loadId="+loadId);
		 HttpResponse response =httpClient.execute(request);
		 String jsonResponse = IOUtils.toString((InputStream)response.getEntity().getContent(), (String) null);
		 JSONObject overallStatus =null;
		 JSONObject robj =null;
		 JSONObject payload =null;
		 if(response.getStatusLine().getStatusCode() == 200)
		 {
			 robj= new JSONObject(jsonResponse);
			 payload = robj.getJSONObject("payload");
			 overallStatus = payload.getJSONObject("overallStatus");
			 
			 while(!overallStatus.getString("status").equals("LOAD_COMPLETED"))
			 {
				 response =httpClient.execute(request);
				 jsonResponse = IOUtils.toString((InputStream)response.getEntity().getContent(), (String) null);
				 robj= new JSONObject(jsonResponse);
				 payload = robj.getJSONObject("payload");
				 overallStatus = payload.getJSONObject("overallStatus");
				 System.out.println(overallStatus.getString("status") + ". Loaded "+ overallStatus.get("totalRecords")+ " records..");
				 if(overallStatus.getString("status").equals("LOAD_FAILED"))
				 {
					 System.out.println(overallStatus.getString("status") + ". Run curl -G http://"+ neptuneEndpoint + "/loader/"+loadId+"?details=true for more details.");
					 break;
				 }
				 Thread.sleep(2000);
			 }
		 }
		 else
		 {
			 throw new Exception(jsonResponse);
		 }
		 
		 
	}
	
	 public static void main(String[] args) throws Exception {
	        if (null == args || args.length < 4) {
	            System.err.println("Usage: BulkLoadData <neptune-endpoint:port> <iam-s3-role-arn> <s3BucketAndKey> <regionCode>");
	            System.exit(1);
	        }
	        
	        String neptuneEndpoint = args[0];
	        String iamRole = args[1];
	        String s3BucketAndKey = args[2];
	        String regionCode = args[3];
	        
	        String loadId = new BulkLoadData().sendLoadDataRequest(neptuneEndpoint, iamRole, s3BucketAndKey, regionCode);
	        System.out.println("Successfully submitted the bulk load request with loadId: "+ loadId);
	        new BulkLoadData().pollForCompletion(neptuneEndpoint, loadId);
	        
	        
	 }
	 
}
