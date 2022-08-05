# Amazon Neptune 1G Playground

A command line tool implementing the [OneGraph](https://arxiv.org/abs/2110.13348) data model . Acts as an in-memory data store for both RDF data and labeled property graphs.

## Usage

The Amazon Neptune 1G Playground is a Java command line program used as an introductory tool for the "OneGraph" data model. The tool consists of both a 'client' and a 'server' component. To function, both components should be active simultaneously

### Building from source

The Amazon Neptune 1G Playground is a Java Maven project and requires JDK 8 and Maven 3 to build from source. 

First, change into the `1g-playground-cli` folder and run `mvn clean package`, this will generate an executable `.jar` in the directory `\target`. Next, change into the `1g-playground-server` folder and run `mvn clean package` again, an executable `.jar` will again be generated in the `\target` directory.

Now we have generated two `.jar` files, one for both client and server. Next, open two different command prompts. Run `java -jar <path_to_generated_jar>` in each of them, passing the path to the generated `.jar` files. The server process should now be running in the one prompt, and the CLI in the other.

### Using commands

The tool supports a number of commands, use the `help` command to obtain a general description of each command, and use the `help <command_name>` command to get a more detailed description.

The following commands can be issued:

**scenario**

    scenario  [-sc1]  [-sc2]  [-sc3]  

Runs a scenario to introduce OneGraph and the functionalities of the tool. The scenarios are self-explanatory and should ideally be taken when first using the tool. Do not pass multiple scenario flags simultaneously.

*Example*

    scenario -sc1
**load**

    load [-t] string  [-path1] string  [Optional, [-path2] string] 
Loads data into the tool, data is loaded additively. The -t flag takes the following inputs:

    nquads, ntriples, rdfxml, turtle, trig, graphson, graphml, og, neptunecsv

See [Gremlin Load Data Format](https://docs.aws.amazon.com/neptune/latest/userguide/bulk-load-tutorial-format-gremlin.html) for an explanation of the `neptunecsv` input format.

When using `neptunecsv`, the `-path1` argument should contain the path to the graph nodes, and `-path2` the path to the graph edges. Do not supply a `-path2` argument when the `-t` flag is not `neptunecsv`.

*Example*

    load -t neptunecsv path/to/nodes.csv path/to/edges.csv
    
or

    load -t ntriples path/to/ntriples.nt

**Export**

    export [-t] string  [-path1] string  [Optional, [-path2] string] 
Exports data from the tool to file, data is loaded additively. The -t flag takes the following inputs:

    nquads, ntriples, rdfxml, turtle, trig, graphson, graphml, og, neptunecsv, formal

Only supply the `-path2` argument when the `-t` argument is `neptunecsv`,

*Example*

    export -t neptunecsv path/to/nodes.csv path/to/edges.csv
    
or

    export ntriples path/to/ntriples.nt
    
**View**

    view -t <data format>

Print all data currently in the tool to the command line.  The -t flag takes the following inputs:

    nquads, ntriples, rdfxml, turtle, trig, graphson, graphml, og, neptunecsv, formal
    
*Example*

    view -t og
    
**query**

    query [-sparql]  [-gremlin]  [-f]  [-input] <query_or_path_to_query> 

Queries the data currenlty in the tool with either SPARQL or Gremlin. Do not supply both the `-sparql` and `-gremlin` flag simultaniously. Supply the -f flag to load a query from file.

*Example*
    
    query -sparql -f <path/to/sparql/query.rq>
    
or

    query -gremlin "SELECT ?s ?p ?o WHERE { ?s ?p ?o }"

**info**

    info

Shows info about the data currently loaded.

**clear-data**

    clear-data
    
Clears all data currently in the tool. Note: Does not clear the settings.

**settings**

    settings [Optional, [-c]]  [Optional, [-path-to-config] <path_to_mapping_configuration>]  [Optional, [-r] true/false]  [Optional, [-t] long]  [-show]  
    
Change the settings currently used in the playground. The flag `-c` takes one of the following values:

    custom, empty, default

If `custom` is passed for `-c`, pass a `-path-to-config` to point to the custom mapping configuration. Used the `-r` flag to indicate whether the current mapping configuration is reversible. Pass the `-t` flag to set the query timeout in milliseconds. Use `-show` to obtain the current settings.

A custom mapping configuration is a `.json` file that has the following structure:

    {
        "default_node_label" : "IRI",
        "node_label_mapping" : {
            "entry1" : ["IRI","IRI_or_STRING"],
            "entry2" : ["IRI","IRI_or_STRING"],
            ...
        },
        "edge_label_mapping" : {
            "entry1" : "IRI_or_STRING",
            "entry2" : "IRI_or_STRING",
            ...
        },
        "property_name_mapping" : {
        "entry1" : "IRI_or_STRING",
            "entry2" : "IRI_or_STRING",
            ...
        }
    }


*Example*

Setting a custom mapping configuration:

    settings -c custom <path_to_custom_configuration>

Setting a default mapping configuration:

    settings -c default

Setting an empty mapping configuration:

    settings -c empty

Setting the query timeout time:

    settings -t 15000
    
Setting the current mapping configuration to be non-reversible:

    settings -r false
    
Getting the current settings:

    settings -show
    


## License

Amazon Neptune 1G Playground is available under [Apache License, Version 2.0](https://aws.amazon.com/apache2.0).


----

Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
