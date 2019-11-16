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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
@JsonInclude(Include.NON_EMPTY)
public class IndexBean{
    public String name = null;
    public List<String> propertyKeys;
    public boolean composite = false;
    public boolean unique = false;
    public String indexOnly = null;
    public String mixedIndex = null;
    /**
     * Construct an indexBean
     * @param name string name of the index
     * @param propertyKeys list property key(s) used for indexing
     * @param composite boolean composit index if true
     * @param unique boolean if key is unique
     * @param indexOnly string name of a label
     * @param mixedIndex string name of an external index backend
     */
    public IndexBean(
            String          name,
            List<String>    propertyKeys,
            boolean         composite,
            boolean         unique,
            String          indexOnly,
            String          mixedIndex){
        this.name = name;
        this.propertyKeys = propertyKeys;
        this.composite = composite;
        this.unique = unique;
        this.indexOnly = indexOnly;
        this.mixedIndex = mixedIndex;
    }


}
