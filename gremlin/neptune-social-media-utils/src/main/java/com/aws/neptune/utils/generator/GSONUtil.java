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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.aws.neptune.utils.generator.bean.BatchImporterDataMap;
import com.aws.neptune.utils.generator.bean.CSVConfig;
import com.aws.neptune.utils.generator.bean.ColumnBean;
import com.aws.neptune.utils.generator.bean.EdgeLabelBean;
import com.aws.neptune.utils.generator.bean.EdgeMapBean;
import com.aws.neptune.utils.generator.bean.EdgeTypeBean;
import com.aws.neptune.utils.generator.bean.GSONSchema;
import com.aws.neptune.utils.generator.bean.IndexBean;
import com.aws.neptune.utils.generator.bean.PropertyKeyBean;
import com.aws.neptune.utils.generator.bean.RelationBean;
import com.aws.neptune.utils.generator.bean.VertexLabelBean;
import com.aws.neptune.utils.generator.bean.VertexMapBean;
import com.aws.neptune.utils.generator.bean.VertexTypeBean;
public class GSONUtil {

    public static GSONSchema loadSchema(String gsonSchemaFile){
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(gsonSchemaFile), GSONSchema.class);
        } catch (Exception e) {
            throw new RuntimeException("Fail to parse, read, or evaluate the GSON schema. " + e.toString());
        }
    }

    public static void writeToFile(String jsonOutputFile,Object gson){
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            mapper.writeValue(new File(jsonOutputFile), gson);
            System.out.println("Generated: "+ jsonOutputFile);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e.toString());
        }

    }
    public static BatchImporterDataMap toDataMap(String csvConfigFile){
        BatchImporterDataMap bmDataMap = new BatchImporterDataMap();
        CSVConfig csvConf = CSVGenerator.loadConfig(csvConfigFile);
        for(VertexTypeBean type: csvConf.VertexTypes){
            String vertexFileName = type.name + ".csv";
            VertexMapBean vertex = new VertexMapBean(type.name);
            for (String key: type.columns.keySet()){
                vertex.maps.put(key, key);
            }
            bmDataMap.vertexMap.put(vertexFileName,vertex.maps);
        }

        for(EdgeTypeBean type: csvConf.EdgeTypes){
            for (RelationBean relation: type.relations) {
                /*Ex: <left-label>_<edgeType>_<right-label>_edges.csv    */
                String edgeFileName = String.join("_",
                                                  relation.left,
                                                  type.name,
                                                  relation.right,
                                                  "edges.csv");
                EdgeMapBean vertex = new EdgeMapBean(type.name);
                Map<String, String> subMap = new HashMap<>();
                Map<String, String> subMap2 = new HashMap<>();
                subMap.put("Left", relation.left + ".node_id");
                vertex.maps.put("[edge_left]", subMap);
                subMap2.put("Right", relation.right + ".node_id");
                vertex.maps.put("[edge_right]", subMap2);
                if (type.columns != null) {
                    for (String key : type.columns.keySet()) {
                        vertex.maps.put(key, key);
                    }
                }
            bmDataMap.edgeMap.put(edgeFileName,vertex.maps);
            }
        }
        return bmDataMap;
    }
    public static GSONSchema configToSchema(String csvConfPath){
        GSONSchema gsonschema = new GSONSchema();
        CSVConfig csvConf = CSVGenerator.loadConfig(csvConfPath);

        //manually add node_id as a unique propertyKey and index
        PropertyKeyBean nodeIdKey = new PropertyKeyBean("node_id", "Integer");
        gsonschema.propertyKeys.add(nodeIdKey);
        IndexBean nodeIdIndex = new IndexBean(
                                    "node_id_comp",
                                    Arrays.asList("node_id"),
                                    true,
                                    true,
                                    null,
                                    null);
        gsonschema.vertexIndexes.add(nodeIdIndex);

        //Extract columns under VertexTypes from csv config and add to propertyKeys
        for (VertexTypeBean type : csvConf.VertexTypes){
            //add vertexLabels
            VertexLabelBean vertexLabel = new VertexLabelBean(type.name);
            gsonschema.vertexLabels.add(vertexLabel);

            //add propertyKeys
            for (Entry<String,ColumnBean> col : type.columns.entrySet()){
                String propertyKeyName = col.getKey();
                String propertyKeyType = col.getValue().dataType;
                boolean compositIndex = col.getValue().composit;
                String indexOnly = col.getValue().indexOnly;
                String mixedIndex = col.getValue().mixedIndex;

                //test key does not exist before adding
                if (null == gsonschema.getPropertyKey(propertyKeyName)) {
                    gsonschema.propertyKeys.add(
                            new PropertyKeyBean(propertyKeyName,propertyKeyType));
                }

                //add composit vertex indexes if any
                if(compositIndex == true) {
                    IndexBean index = new IndexBean(
                                        String.join("_", propertyKeyName, "comp"),
                                        Arrays.asList(propertyKeyName),
                                        compositIndex,
                                        false,
                                        indexOnly,
                                        null);
                    if (null == gsonschema.getVertexIndex(index.name)) {
                        gsonschema.vertexIndexes.add(index);
                    }
                }
                //add mixed vertex index if any
                if(mixedIndex != null) {
                    IndexBean index = new IndexBean(
                                        String.join("_", propertyKeyName, "mixed"),
                                        Arrays.asList(propertyKeyName),
                                        false,
                                        false,
                                        indexOnly,
                                        mixedIndex);
                    if (null == gsonschema.getVertexIndex(index.name)) {
                        gsonschema.vertexIndexes.add(index);
                    }
                }
            }
        }

        //extract edge properties from csv config and add to propertyKeys
        for (EdgeTypeBean type : csvConf.EdgeTypes){
            EdgeLabelBean edgeLabel = new EdgeLabelBean(type.name, type.multiplicity);
            gsonschema.edgeLabels.add(edgeLabel);
            if (type.columns != null) {
                //extract columns and add to propertKeys
                for (Entry<String, ColumnBean> col : type.columns.entrySet()) {
                    String propertyKeyName = col.getKey();
                    String propertyKeyType = col.getValue().dataType;
                    boolean compositIndex = col.getValue().composit;
                    String indexOnly = col.getValue().indexOnly;
                    String mixedIndex = col.getValue().mixedIndex;
                    gsonschema.propertyKeys.add(new PropertyKeyBean(
                                                        propertyKeyName,
                                                        propertyKeyType));

                    //add composit edgeIndex if any
                    if (compositIndex == true) {
                        IndexBean index = new IndexBean(
                                              String.join("_", propertyKeyName, "comp"),
                                              Arrays.asList(propertyKeyName),
                                              compositIndex,
                                              false,
                                              indexOnly,
                                              mixedIndex);
                        if (null == gsonschema.getEdgeIndex(index.name)) {
                            gsonschema.edgeIndexes.add(index);
                        }
                    }
                    //add mixed edgeIndex if any
                    if(mixedIndex != null) {
                        IndexBean index = new IndexBean(
                                            String.join("_", propertyKeyName, "mixed"),
                                            Arrays.asList(propertyKeyName),
                                            false,
                                            false,
                                            indexOnly,
                                            mixedIndex);
                        if (null == gsonschema.getEdgeIndex(index.name)) {
                            gsonschema.edgeIndexes.add(index);
                        }
                    }
                }
            }
        }
        return gsonschema;
    }
}
