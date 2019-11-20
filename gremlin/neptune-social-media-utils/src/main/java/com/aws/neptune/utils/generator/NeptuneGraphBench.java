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
package com.aws.neptune.utils.generator;

public class NeptuneGraphBench {

    public NeptuneGraphBench(String csvConfPath){

    }

    static void prepareCSV(String csvConfFile, String s3Bucket, String bucketFolder){
        CSVGenerator csv = new CSVGenerator(csvConfFile);
        System.out.println("Loaded csv config file: "+ csvConfFile);
        csv.writeAllCSVs(s3Bucket, bucketFolder );
    }

    public static void main(String[] args) {
        if (null == args || args.length < 2) {
            System.err.println("Usage: NeptuneGraphBench <csv-config-file> <s3-bucket> <optional-bucket-folder>");
            System.exit(1);
        }
        String arg2=null;
       try {
    	   arg2 = args.length ==2? null: args[2];
    	   
    	   String csvConfFile = args[0];
    	   prepareCSV(csvConfFile, args[1], arg2);
       
           //ejazs - commenting these lines as schema and datamapper are not required for Neptune
           //GSONUtil.writeToFile(args[1] + "/schema.json",GSONUtil.configToSchema(csvConfPath));
           //GSONUtil.writeToFile(args[1] + "/datamapper.json", GSONUtil.toDataMap(csvConfPath));
    
       } catch (Exception e) {
        throw e;
    	//System.err.println(e.getMessage());
        //System.exit(1);
    }

    }
}
