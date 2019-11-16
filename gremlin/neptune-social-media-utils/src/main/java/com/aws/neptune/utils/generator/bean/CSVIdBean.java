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
package com.aws.neptune.utils.generator.bean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomUtils;

/**
 * Stores vertex ID boundaries
 *
 */
public class CSVIdBean {
    private Map<String, IdRange> idMap = null;
    /**Calculates and stores ID boundaries for each vertex type
     * @param vertexTypes
     */
    public CSVIdBean(List<VertexTypeBean> vertexTypes){
        this.idMap = new HashMap<String, IdRange>();
        int bot = 0;
        int top = 0;
        for (VertexTypeBean vertexType: vertexTypes){
            bot = top + 1;
            top = bot + vertexType.row - 1;
            this.idMap.put(vertexType.name, new IdRange(bot, top));
        }
    }
    /**Return a random id of a vertex type
     * @param name vertex label name
     * @return a random integer id of the given vertex
     */
    public int getRandomIdForVertexType(String name){
        return RandomUtils.nextInt(idMap.get(name).minId, idMap.get(name).maxId);
    }
    /**Get Minimum Id boundary for a vertex
     * @param name vertex label name
     * @return Minimum Id boundary for a vertex
     */
    public int getMinId(String name){
        return idMap.get(name).minId;
    }
    /**Get Maximum Id boundary for a vertex
     * @param name vertex label name
     * @return Maximum Id boundary for a vertex
     */
    public int getMaxId(String name){
        return idMap.get(name).maxId;
    }
    /**Calculate number of vertices for a vertex label
     * @param v name vertex label name
     * @return number of vertices for a vertex label
     */
    public int getIdPoolSize(String v) {
        int size = getMaxId(v) - getMinId(v) + 1;
        return size;
    }
    class IdRange{
        int minId = 0;
        int maxId = 0;
        public IdRange(int minId, int maxId){
            this.minId = minId;
            this.maxId = maxId;
        }
    }
}