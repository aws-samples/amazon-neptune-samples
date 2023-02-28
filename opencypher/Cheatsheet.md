# openCypher Syntax Cheatsheet
The below are some common examples of openCypher Syntax based upon the [air-routes dataset](https://github.com/krlawrence/graph/tree/master/sample-data) provided by Kelvin Lawrence.

## <a name='TableofContents'></a>Table of Contents
<!-- vscode-markdown-toc -->
* [Read Queries](#ReadQueries)
	* [Pattern Matching](#PatternMatching)
		* [Find Nodes with Label](#FindNodeswithLabel)
		* [Find Nodes by Label and Property](#FindNodesbyLabelandProperty)
		* [Find pattern](#Findpattern)
		* [Find path pattern](#Findpathpattern)
		* [Find optional pattern](#Findoptionalpattern)
		* [Find if something exists](#Findifsomethingexists)
	* [Looping](#Looping)
		* [Fixed number of loops](#Fixednumberofloops)
		* [Range of hops](#Rangeofhops)
		* [Range of hops (minimum boundary only)](#Rangeofhopsminimumboundaryonly)
		* [Range of hops (maximum boundary only)](#Rangeofhopsmaximumboundaryonly)
	* [Returning Values](#ReturningValues)
		* [Return everything](#Returneverything)
		* [Return property](#Returncolumn)
		* [Return property with alias](#Returncolumnwithalias)
		* [Return distinct values](#Returndistinctvalues)
		* [Order results ascending](#Orderresultsascending)
		* [Order results descending](#Orderresultsdescending)
		* [Return count of results](#Returncountofresults)
		* [Limit results](#Limitresults)
		* [Paginate results](#Paginateresults)
		* [Join distinct set from two queries](#Joindistinctsetfromtwoqueries)
		* [Join combined set from two queries](#Joincombinedsetfromtwoqueries)
* [Write Queries](#WriteQueries)
	* [Creating Data](#CreatingData)
		* [Create a node](#Createanode)
		* [Create a node with label and no properties](#Createanodewithlabelandnoproperties)
		* [Create a node with label and properties](#Createanodewithlabelandproperties)
		* [Create relationship](#Createrelationship)
		* [Create relationship with properties](#Createrelationshipwithproperties)
		* [Create a path](#Createapath)
		* [Create from a list](#Createfromalist)
	* [Updating Data](#UpdatingData)
		* [Set a property](#Setaproperty)
		* [Remove a property](#Removeaproperty)
		* [Merge a node](#Mergeanode)
	* [Deleting Data](#DeletingData)
		* [Delete a node](#Deleteanode)
		* [Delete a node and relationships](#Deleteanodeandrelationships)
		* [Delete relationships](#Deleterelationships)
* [Operators](#Operators)
* [Functions](#Functions)

<!-- vscode-markdown-toc-config
	numbering=false
	autoSave=true
	/vscode-markdown-toc-config -->
<!-- /vscode-markdown-toc -->

## <a name='ReadQueries'></a>Read Queries

### <a name='PatternMatching'></a>Pattern Matching
Pattern matching is the most basic actions one can do in openCypher and is the basis for all read queries.

#### <a name='FindNodeswithLabel'></a>Find Nodes with Label
```
MATCH (a:airport) RETURN a
```

#### <a name='FindNodesbyLabelandProperty'></a>Find Nodes by Label and Property
```
MATCH (a:airport {code: 'SEA'}) RETURN a
```
```
MATCH (a:airport) WHERE a.code='SEA' RETURN a
```

#### <a name='Findpattern'></a>Find pattern
```
MATCH (a:airport {code: 'SEA'})-[:route]->(d) RETURN d
```
```
MATCH (a:airport {code: 'SEA'})<-[:route]-(d) RETURN d
```
#### <a name='Findpathpattern'></a>Find path pattern
```
MATCH p=(a:airport {code: 'SEA'})-[:route]->(d) RETURN p
```
#### <a name='Findoptionalpattern'></a>Find optional pattern
```
OPTIONAL MATCH (a:airport {code: 'SEA'})-[:route]->(d) RETURN d
```

#### <a name='Findifsomethingexists'></a>Find if something exists
```
MATCH (a:airport {code: 'SEA'})-[:route]->(d) RETURN d
```

### <a name='Looping'></a>Looping
Looping through connections is a common pattern in property graphs and can be accomplished using either fixed or variable length paths.

#### <a name='Fixednumberofloops'></a>Fixed number of loops
```
MATCH (a:airport {code: 'SEA'}*2)-[:route]->(d) RETURN d
```
#### <a name='Rangeofhops'></a>Range of hops
```
MATCH (a:airport {code: 'SEA'})-[:route*1..3]->(d) RETURN d
```
#### <a name='Rangeofhopsminimumboundaryonly'></a>Range of hops (minimum boundary only)
```
MATCH (a:airport {code: 'SEA'})-[:route*1..]->(d) RETURN d
```
#### <a name='Rangeofhopsmaximumboundaryonly'></a>Range of hops (maximum boundary only)
```
MATCH (a:airport {code: 'SEA'})-[:route*..3]->(d) RETURN d
```

### <a name='ReturningValues'></a>Returning Values
Each read query must specify how to return values from the query.

#### <a name='Returneverything'></a>Return everything
```
MATCH (a) RETURN *
```

#### <a name='Returncolumn'></a>Return property
```
MATCH (a:airport) RETURN a.city
```
#### <a name='Returncolumnwithalias'></a>Return property with alias
```
MATCH (a:airport) RETURN a.city AS dest
```
#### <a name='Returndistinctvalues'></a>Return distinct values
```
MATCH (a:airport) RETURN DISTINCT a.region
```
#### <a name='Orderresultsascending'></a>Order results ascending
```
MATCH (a:airport) RETURN a ORDER BY a.elev
```
#### <a name='Orderresultsdescending'></a>Order results descending
```
MATCH (a:airport) RETURN a ORDER BY a.elev DESC
```

#### <a name='Returncountofresults'></a>Return count of results
```
MATCH (a:airport) RETURN count(a)
```

#### <a name='Limitresults'></a>Limit results
```
MATCH (a:airport) RETURN a LIMIT 5
```

#### <a name='Paginateresults'></a>Paginate results
```
MATCH (a:airport) RETURN a SKIP 5 LIMIT 5
```

#### <a name='Joindistinctsetfromtwoqueries'></a>Join distinct set from two queries
```
MATCH (a:airport {code:'SEA'}) 
RETURN a.city
UNION 
MATCH (a:airport {code:'ANC'}) 
RETURN a.elev
```
#### <a name='Joincombinedsetfromtwoqueries'></a>Join combined set from two queries
```
MATCH (a:airport {code:'SEA'})-->(d) 
RETURN d.city
UNION ALL
MATCH (a:airport {code:'ANC'})-->(d) 
RETURN d.city
```

## <a name='WriteQueries'></a>Write Queries

### <a name='CreatingData'></a>Creating Data

#### <a name='Createanode'></a>Create a node
```
CREATE (a)
```
#### <a name='Createanodewithlabelandnoproperties'></a>Create a node with label and no properties
```
CREATE (a:airport)
```
#### <a name='Createanodewithlabelandproperties'></a>Create a node with label and properties
```
CREATE (a:airport {code: 'FOO'})
```
#### <a name='Createrelationship'></a>Create relationship
```
MATCH (a:airport {code:'SEA'}), (b:airport {code:'ANC'})
CREATE (a)-[r:route]->(b)
RETURN r
```
#### <a name='Createrelationshipwithproperties'></a>Create relationship with properties
```
MATCH (a:airport {code:'SEA'}), (b:airport {code:'ANC'})
CREATE (a)-[r:route {dist: 1000}]->(b)
RETURN r
```
#### <a name='Createapath'></a>Create a path
```
CREATE p = (a:airport {code:'Foo'})-[:route]->(a:airport {code:'Bar'})
RETURN p
```
#### <a name='Createfromalist'></a>Create from a list
```
UNWIND [{code: 'foo'}, {code: 'bar'}] AS properties
CREATE (a:airport) SET a = properties
```

### <a name='UpdatingData'></a>Updating Data

#### <a name='Setaproperty'></a>Set a property
```
MATCH (a:airport {code: 'SEA'})
SET a.foo='bar'
```
#### <a name='Removeaproperty'></a>Remove a property
```
MATCH (a:airport {code: 'SEA'})
SET a.foo=null
```
```
MATCH (a:airport {code: 'SEA'})
REMOVE a.foo
```
#### <a name='Mergeanode'></a>Merge a node
```
MERGE (a:airport {code: 'SEA'})
  ON CREATE SET n.created = timestamp()
  ON MATCH SET
    n.counter = coalesce(n.counter, 0) + 1,
    n.accessTime = timestamp()
```
### <a name='DeletingData'></a>Deleting Data

#### <a name='Deleteanode'></a>Delete a node
```
DELETE (a:airport {code: 'SEA'})
```
#### <a name='Deleteanodeandrelationships'></a>Delete a node and relationships
```
MATCH (a:airport {code: 'SEA'})
DETACH DELETE a
```
#### <a name='Deleterelationships'></a>Delete relationships
```
MATCH (a:airport {code:'SEA'})-[r]->(d) 
DELETE r
```

## <a name='Operators'></a>Operators

Type | Operators
------------ | -------------
General | DISTINCT, x.y (property access)
Math | +, -, *, /, %, ^
Comparison | =, >, <, <>, <=, >=, IS NULL, IS NOT NULL
Boolean | AND, OR, NOT, XOR
String | STARTS WITH, ENDS WITH, CONTAINS, +
LIST | +, IN, []

## <a name='Functions'></a>Functions

Type | Functions
------------ | -------------
Predicate | exists()
Scalar | coalesce(), endNode(), head(), id(0, last(), length(), properties(), size(), startNode(), timestamp(), toBoolean(), toFloat(), toInteger(), type()
Aggregating | avg(), collect(), count(), max(), min(), percentileCont(), percentileDisc(), stDev(), stDevP(), sum()
List | keys(), labels(), nodes(), range(), relationships(), reverse(), tail()
Math - numeric | abs(), ceil(), floor(), rand(), round(), sign()
Math - logarithmic | e(), exp(), log(), log10(), sqrt()
Math - trigonometric | acos(), asin(), atan(), atan2(), cos(), cot(), degree(), pi(), radians(), sin(), tan()
String | left(), lTrim(), replace(), reverse(), right(), rTrim(), split(), substring(), toLower(), toString(), toUpper(), trim()
