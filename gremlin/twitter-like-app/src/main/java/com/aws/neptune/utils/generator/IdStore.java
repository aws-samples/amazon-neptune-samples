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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import com.aws.neptune.utils.generator.bean.CSVIdBean;
import com.aws.neptune.utils.generator.bean.RelationBean;

public class IdStore {
    private CSVIdBean idBean;
    private SimpleRelationStore simpleStore = null;
    private RelationStore generalstore = null;
    private RelationBean relation = null;
    private String multiplicity;
    /**Initialize an IdStore object for generating relations
     * @param idbean
     * @param relation
     * @param multiplicity
     */
    public IdStore(CSVIdBean idbean, RelationBean relation, String multiplicity) {
        this.idBean = idbean;
        this.relation = relation;
        this.multiplicity = multiplicity;
        if (multiplicity.equals("SIMPLE")) {
            this.simpleStore = new SimpleRelationStore(relation);
        }else if (multiplicity.equals("ONE2ONE")
                || multiplicity.equals("MANY2ONE")
                || multiplicity.equals("ONE2MANY")) {
                this.generalstore = new RelationStore(relation);
        }else if (multiplicity.equals("MULTI")){

        }else {
            throw new MultiplicityException(multiplicity);
        }
    }
    /**
     * Get a random integer except the one(s) in the the ex
     * @param vType vertex label
     * @param ex unsorted list of unique numbers to exclude
     * @return an integer not in the exclude list or -1 if not found
     */
    public int getRandomIdWithException(String vType, List<Integer> ex) {
        int start = idBean.getMinId(vType);
        int end = idBean.getMaxId(vType);
        boolean found = false;
        int rnd = -1;
        if (ex.size() > (end - start))
            return rnd;
        while (!found) {
            rnd = RandomUtils.nextInt(start, end + 1);
            if (false == ex.contains(rnd)) {
                found = true;
            }
        }
        return rnd;
    }
    /**
//   * Get a random integer except the one(s) in the the ex
//   * @param vType vertex label
//   * @param ex a sorted list of unique numbers to exclude
//   * @return an integer not in the exclude list or -1 if not found
//   */
//  public int anotherGetRandomIntWithoutExclude(String vType, List<Integer> ex) {
//      int start = getMinId(vType);
//      int end = getMaxId(vType);
//      int rnd = RandomUtils.nextInt(start, end + 1 - ex.size());
//      for (int e : ex) {
//          if (rnd < e) {
//              break;
//          }
//          rnd++;
//      }
//      return rnd>end?-1:rnd;
//  }
    /**Get random right id given a left id with respect to the multiplicity
     * This is used for generating supernodes.
     * @param leftId
     * @return IdBean object which consists a pair of IDs
     */
    public IdBean getRandomIdForRelation(int leftId) {
        IdBean ids = new IdBean();
        ids.left = leftId;
        if (multiplicity.equals("SIMPLE")) {
            ids.right = getRandomIdWithException(
                                            relation.right,
                                            simpleStore.getIds(ids.left));
            if ( -1 == ids.right) {
                throw new SupernodeException(relation,ids);
            }
            simpleStore.put(leftId, ids.right);
        }else if (multiplicity.equals("ONE2ONE")) {
            throw new RuntimeException(
                    String.format("Supernode is not applicable to %s relation", multiplicity));
        }else if (multiplicity.equals("MANY2ONE")) {
            throw new RuntimeException(
                    String.format("Supernode is not applicable to %s relation", multiplicity));
        }else if (multiplicity.equals("ONE2MANY")) {
            ids.right = getRandomIdWithException(relation.right, generalstore.getRightIds());
            if ( -1 == ids.right) {
                throw new SupernodeException(relation,ids);
            }
            if ( relation.left.equals(relation.right) && ids.left == ids.right && ! relation.selfRef) {
                List <Integer> excl = generalstore.getRightIds();
                excl.add(ids.right);
                ids.right = getRandomIdWithException(relation.right, excl);
                if ( -1 == ids.right) {
                    throw new SupernodeException(relation,ids);
                }
            }
            generalstore.putRight(ids.right);
        }else if ( multiplicity.equals("MULTI")) {
            List <Integer> excl = null;
            excl = new ArrayList<Integer>();
            if ( ! relation.selfRef) {
                excl.add(ids.left);
            }
            ids.right = getRandomIdWithException(relation.right, excl);
        }else {
            throw new MultiplicityException(multiplicity);
        }
        return ids;
    }
    /**Get random pair of IDs with respect to the multiplicity
     * @return IdBean object which consists a pair of IDs
     */
    public IdBean getRandomPairIdForRelation() {
        IdBean ids = new IdBean(idBean.getRandomIdForVertexType(relation.left),
                                idBean.getRandomIdForVertexType(relation.right));
        if (multiplicity.equals("SIMPLE")) {
            boolean added = simpleStore.put(ids.left, ids.right);
            while(!added) {
                //check left id already relates to every right id
                while (simpleStore.getIds(ids.left).size() == idBean.getIdPoolSize(relation.right)) {
                    int index = orderedIndex(simpleStore.excl_left_id, ids.left);
                    if ( -1 != index)
                        simpleStore.excl_left_id.add(index, ids.left);
                    ids.left = getRandomIdWithException(relation.left,
                                                        simpleStore.excl_left_id);
                    if ( -1 == ids.left) {
                        throw new TooManyEdgeExeption(relation, multiplicity);
                    }
                }
                ids.right = getRandomIdWithException(
                                            relation.right,
                                            simpleStore.getIds(ids.left));
                added = simpleStore.put(ids.left, ids.right);
               // System.out.println(String.join(" ","Replaced", Integer.toString(ids.left), Integer.toString(ids.right)));
            }
        }else if(multiplicity.equals("ONE2ONE")) {
            if( ! generalstore.putLeft(ids.left)){
                ids.left = getRandomIdWithException(relation.left, generalstore.getLeftIds());
                if ( ! generalstore.putLeft(ids.left) || -1 == ids.left) {
                    throw new TooManyEdgeExeption(relation, multiplicity);
                }
            }
            if ( relation.left.equals(relation.right) && ids.left == ids.right && ! relation.selfRef) {
                List <Integer> excl = generalstore.getRightIds();
                excl.add(ids.right);
                ids.right = getRandomIdWithException(relation.right, excl);
                if ( -1 == ids.right) {
                    throw new TooManyEdgeExeption(relation, multiplicity);
                }
                generalstore.putRight(ids.right);
            }else if( ! generalstore.putRight(ids.right)) {
                ids.right = getRandomIdWithException(relation.right, generalstore.getRightIds());
                if ( relation.left.equals(relation.right) && ids.left == ids.right && ! relation.selfRef) {
                    List <Integer> excl = generalstore.getRightIds();
                    excl.add(ids.right);
                    ids.right = getRandomIdWithException(relation.right, excl);
                    if ( -1 == ids.right) {
                        throw new TooManyEdgeExeption(relation, multiplicity);
                    }
                }
                generalstore.putRight(ids.right);
            }
        }else if (multiplicity.equals("MANY2ONE")) {
            if (! generalstore.putLeft(ids.left)) {
                ids.left = getRandomIdWithException(relation.left, generalstore.getLeftIds());
                if ( -1 == ids.left) {
                    throw new TooManyEdgeExeption(relation, multiplicity);
                }
                if ( relation.left.equals(relation.right) && ids.left == ids.right && ! relation.selfRef) {
                    List <Integer> excl = generalstore.getRightIds();
                    excl.add(ids.left);
                    ids.right = getRandomIdWithException(relation.right, excl);
                    if ( -1 == ids.right) {
                        throw new TooManyEdgeExeption(relation, multiplicity);
                    }
                }
            }
            generalstore.putLeft(ids.left);
        }else if (multiplicity.equals("ONE2MANY")) {
            if (! generalstore.putRight(ids.right)) {
                ids.right = getRandomIdWithException(relation.right, generalstore.getRightIds());
                if ( -1 == ids.right) {
                    throw new TooManyEdgeExeption(relation, multiplicity);
                }
                if ( relation.left.equals(relation.right) && ids.left == ids.right && ! relation.selfRef) {
                    List <Integer> excl = generalstore.getRightIds();
                    excl.add(ids.right);
                    ids.right = getRandomIdWithException(relation.right, excl);
                    if ( -1 == ids.right) {
                        throw new TooManyEdgeExeption(relation, multiplicity);
                    }
                }
            }
            generalstore.putRight(ids.right);
        }else if (multiplicity.equals("MULTI")) {
            List <Integer> excl = new ArrayList<Integer>();;
            if ( ! relation.selfRef) {
                excl.add(ids.left);
                ids.right = getRandomIdWithException(relation.right, excl);
            }
        }else {
            throw new MultiplicityException(multiplicity);
        }
        return ids;
    }
    /**This is used to create an sorted and unique integer array.
     * Given a list l, find out which index to insert i.
     * @param l
     * @param i
     * @return -1 if i already exists in l or return the insertion index.
     */
    public int orderedIndex(List<Integer> l, int i) {
        return lastIndexOf(l, i, 0, l.size() - 1);
    }
    /**binary search index of i
     * @param l
     * @param i
     * @param start
     * @param end
     * @return last index of i or -1 if i already exists
     */
    private int lastIndexOf(List<Integer> l, int i, int start, int end) {
        if(start <= end){
           int mid = (start + end)/2;
           if(l.get(mid).equals(i)){
               return -1;
           }
           else if (i < l.get(mid)){
               return lastIndexOf(l, i, start, mid - 1);
           }
           else{
               return lastIndexOf(l, i, mid + 1, end);
           }
        }
        return start;
    }
    public class IdBean{
        public int left = -1;
        public int right = -1;
        private ArrayList<Integer> list;
        /**Initialize POJO to store a pair of integer IDs
         */
        public IdBean() {};
        /**Initialize POJO to store a pair of integer IDs
         * @param left left id
         * @param right right id
         */
        public IdBean(int left, int right) {
            this.left = left;
            this.right = right;
        }
        /**Generate an array of IDs in the IdBean
         * @return array of IDs in the IdBean
         */
        public ArrayList<Integer>toArrayList(){
            this.list = new ArrayList<Integer>();
            this.list.add(left);
            this.list.add(right);
            return list;
        }
    }
    /**
     * An object to manipulate SIMPLE multiplicity relation
     */
    class SimpleRelationStore {
        private Map<Integer, HashSet<Integer>> store = new HashMap<Integer, HashSet<Integer>>();
        private RelationBean relation = null;
        private List<Integer> excl_left_id;
        public SimpleRelationStore(RelationBean relation) {
            this.relation = relation;
            this.excl_left_id = new ArrayList<Integer>();
        }
        public boolean put(int left, int right) {
            if (! store.containsKey(left)) {
                store.put(left, new HashSet<Integer>());
                if (!relation.selfRef) {
                    store.get(left).add(left);
                }
            }
            return store.get(left).add(right);
        }
        public List<Integer> getIds(int left) {
            if (! store.containsKey(left)) {
                store.put(left, new HashSet<Integer>());
                if (!relation.selfRef) {
                    store.get(left).add(left);
                }
            }
            List<Integer> a = new ArrayList<Integer>(store.get(left));
            Collections.sort(a);
            return a;
        }
    }
    /**
     * An object used to manipulate ONE2ONE,MANYTOONE, and ONE2MANY relations
     */
    class RelationStore{
        private HashSet<Integer> left;
        private HashSet<Integer> right;
        //private RelationBean relation;
        public RelationStore(RelationBean relation) {
            this.left = new HashSet<Integer>();
            this.right = new HashSet<Integer>();
            //this.relation = relation;
        }
        public boolean putLeft(int left) {
            return this.left.add(left);
        }
        public boolean putRight(int right) {
            return this.right.add(right);
        }
        public List<Integer> getLeftIds() {
            List<Integer> a = new ArrayList<Integer>(left);
            return a;
        }
        public List<Integer> getRightIds() {
            List<Integer> a = new ArrayList<Integer>(right);
            return a;
        }
    }
    class SupernodeException extends RuntimeException{
        private static final long serialVersionUID = 1L;
        public SupernodeException(RelationBean relation, IdBean ids) {
            super(String.format("Not enough IDs to generate supernode"
                        + " relations for node_id: %s. Inscrease"
                        + " number of rows for %s and %s or reduce edges for %s",
                        ids.left, relation.left,
                        relation.right, relation.supernode.toString()));
        }
    }
    class MultiplicityException extends RuntimeException{
        private static final long serialVersionUID = 1L;

        public MultiplicityException(String multiplicity) {
            super(String.format("Unrecogized multiplicity option: %s."
                    + "Check your CSV config json file.", multiplicity));
        }
    }
    class TooManyEdgeExeption extends RuntimeException{
        private static final long serialVersionUID = 1L;
        public TooManyEdgeExeption(RelationBean relation, String multiplicity) {
            super(String.format("Not enough %s to generate %d %s relations. "
                    + "Consider increasing the number of %s or %s OR "
                    + "decresing number of relations.",
                    relation.left, relation.row, multiplicity,
                    relation.left, relation.right));
        }
    }
}
