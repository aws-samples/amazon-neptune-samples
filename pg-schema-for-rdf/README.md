# PG-Schema for RDF

This is an experiment that adds an "RDF-friendly" syntax to the [PG-Schema](https://arxiv.org/abs/2211.10962) proposal to explore possibilities for better aligment between RDF and Labeled Property Graphs (LPGs). The new PG-Schema expressions are translated to a combination of RDF Schema and [SHACL](https://www.w3.org/TR/shacl/), effectively allowing validation of RDF graphs. This experimentation provides better understanding of what RDF/LPG interoperability means, and thus supports Amazon Neptune's [OneGraph project](https://www.semantic-web-journal.net/content/onegraph-vision-challenges-breaking-graph-model-lock-0).

Once presented, material from [Ora Lassila](https://www.lassila.org/)'s talk at [KGC 2024](https://www.knowledgegraph.tech/) (titled "Schema language for both RDF and LPGs") will be included here as well.

## Installation

To play with _PG-Schema for RDF_, you need Python 3.10 (or newer), clone or fork the [amazon-neptune-samples](https://github.com/aws-samples/amazon-neptune-samples) repository, and then:
```shell
cd pg-schema-for-rdf
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

See the file [test/demo.py](./test/demo.py) for an example of how the system translates PG-Schema expressions to SHACL.

## License

See [LICENSE](../LICENSE) for license and copyright information.
