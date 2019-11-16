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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class GSONSchema{
    public List<PropertyKeyBean> propertyKeys = new ArrayList<PropertyKeyBean>();
    public List<VertexLabelBean> vertexLabels = new ArrayList<VertexLabelBean>();
    public List<EdgeLabelBean> edgeLabels= new ArrayList<EdgeLabelBean>();
    public List<IndexBean> vertexIndexes= new ArrayList<IndexBean>();
    public List<IndexBean> edgeIndexes= new ArrayList<IndexBean>();
    public List<VertexCentricIndexBean> vertexCentricIndexes= new ArrayList<VertexCentricIndexBean>();

    /**
     * Search property by name
     * @param name property name
     * @return the corresponding PropertyKeyBean or null
     */
    public PropertyKeyBean getPropertyKey(String name) {
        for (Iterator<PropertyKeyBean> iterator = propertyKeys.iterator(); iterator.hasNext();) {
            PropertyKeyBean propertyKeyBean = iterator.next();
            if (propertyKeyBean.name.equals(name)) {
                return propertyKeyBean;
            }
        }
        return null;
    }

    /**
     * Search VertexIndex by index name
     * @param indexName the index name
     * @return the corresponding IndexBean or null
     */
    public IndexBean getVertexIndex(String indexName) {
        for (Iterator<IndexBean> iterator = vertexIndexes.iterator(); iterator.hasNext();) {
            IndexBean indexBean = iterator.next();
            if (indexBean.name.equals(indexName)) {
                return indexBean;
            }
        }
        return null;
    }

    /**
     * Search EdgeIndex by index name
     * @param indexName the index name
     * @return the corresponding IndexBean or null
     */
    public IndexBean getEdgeIndex(String indexName) {
        for (Iterator<IndexBean> iterator = edgeIndexes.iterator(); iterator.hasNext();) {
            IndexBean indexBean = iterator.next();
            if (indexBean.name.equals(indexName)) {
                return indexBean;
            }
        }
        return null;
    }
}
