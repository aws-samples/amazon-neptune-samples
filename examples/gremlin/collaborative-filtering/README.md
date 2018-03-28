# This example shows how to load a collaborative filtering data set on Amazon Neptune

## Prerequisite

The cloudformation template will assume there is an already established Amazon Neptune Cluster. See the following links for how to create an Amazon Neptune Cluster for Gremlin:  

* https://docs.aws.amazon.com/neptune/latest/userguide/get-started-CreateInstance-Console.html
* https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load.html
* https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format-gremlin.html

Also assumes you have a client on EC2 that can reach the Amazon Neptune Cluster

## Use Case

Build a recommendation engine based a collaborative filtering technique. This includes leveraging similiar relationships across vertices to infer what a new relationship might be. In this 
example we'll traverse customer purchases and based on similiar purchase history, we'll recommend new products for customers. 

The purposes of this tutorial is to illustrate functionality not scale, so we'll leverage a small dataset.


![cloudformation](images/image1.jpg)


## Step 1 (Load Data Sample data)


**Game & Player Vertices** (~id,GamerAlias:String,ReleaseDate:Date,GameGenre:String,ESRBRating:String,Developer:String,Platform:String,GameTitle:String )

```

curl -X POST \
    -H 'Content-Type: application/json' \
    http://trainingdb.c4niqbvxvuf6.us-east-1-beta.rds.amazonaws.com:8182/loader -d '
    { 
      "source" : "s3://neptune-data-ml/recommendation/vertex.txt", 
      "accessKey" : "", 
      "secretKey" : "",
      "format" : "csv", 
      "region" : "us-east-1", 
      "failOnError" : "FALSE"
    }'

```

**Edges** (~id, ~from, ~to, ~label, weight:Double) 

```
curl -X POST \
    -H 'Content-Type: application/json' \
    http://trainingdb.c4niqbvxvuf6.us-east-1-beta.rds.amazonaws.com:8182/loader -d '
    { 
      "source" : "s3://neptune-data-ml/recommendation/edges.txt", 
      "accessKey" : "", 
      "secretKey" : "",
      "format" : "csv", 
      "region" : "us-east-1", 
      "failOnError" : "FALSE"
    }'

```


Alternatively, you could load all of the files by loading the entire directory

```
curl -X POST \
    -H 'Content-Type: application/json' \
    http://neptune-cluster:8182/loader -d '
    { 
      "source" : "s3://neptune-data-ml/recommendation/", 
      "accessKey" : "", 
      "secretKey" : "",
      "format" : "csv", 
      "region" : "us-east-1", 
      "failOnError" : "FALSE"
    }'
```

## Sample Queries


**query a particular vertex (gamer)**

```
gremlin> g.V().hasId('Luke').valueMap()
==>{GamerAlias=[skywalker123]}

gremlin> g.V().has("GamerAlias","skywalker123").valueMap()
==>{GamerAlias=[skywalker123]}

gremlin> g.V().has('GamerAlias','skywalker123')
==>v[Luke]

```

**What games does skywalker123 like?**
```
gremlin> g.V().has('GamerAlias','skywalker123').as('gamer').out('likes')
==>v[ARMS]
==>v[HorizonZeroDawn]
==>v[GranTurismoSport]
==>v[Ratchet&Clank]
==>v[Fifa18]
==>v[GravityRush]
==>v[SuperMarioOdyssey]
==>v[MarioKart8]

```

**Who else likes the same games? **
```
gremlin> g.V().has('GamerAlias','skywalker123').out('likes').in('likes').dedup().values('GamerAlias')
==>forchinet
==>skywalker123
==>bringit32
==>smiles007

```

**Who else likes these games (exclude yourself)**
```
gremlin> g.V().has('GamerAlias','skywalker123').as('TargetGamer').out('likes').in('likes').where(neq('TargetGamer')).dedup().values('GamerAlias')
==>forchinet
==>bringit32
==>smiles007

```

**What are other games similiar games like ? **
```
gremlin> g.V().has('GamerAlias','skywalker123').as('TargetGamer').out('likes').in('likes').where(neq('TargetGamer')).out('likes').dedup().values('GameTitle')
==>ARMs
==>HorizonZeroDawn
==>GranTurismoSport
==>Nioh
==>TombRaider
==>CallOfDutyBO4
==>SuperMarioOdyssey
==>MarioKart8
==>Ratchet&Clank
==>GravityRush
==>Knack

```

**Which games might make sense to recommend to a specific gamer that they don't already like? **
```
gremlin> g.V().has('GamerAlias','skywalker123').as('TargetGamer').out('likes').aggregate('self').in('likes').where(neq('TargetGamer')).out('likes').where(without('self')).dedup().values('GameTitle')
==>Nioh
==>TombRaider
==>CallOfDutyBO4
==>Knack

```

**Drop data**
```
g.V().drop().iterate()

```

## License

Copyright 2011-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

[http://aws.amazon.com/apache2.0/](http://aws.amazon.com/apache2.0/)

or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

