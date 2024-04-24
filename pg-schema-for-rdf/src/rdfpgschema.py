import logging
from pyparsing import Suppress, ZeroOrMore, Word, alphanums, Combine
from pgschema import Translator, TranslatedSchemaElement, NodeType, enclosed
from rdflib import URIRef, Graph, RDF, OWL, Literal
import pyshacl
from rdfhelpers import Constructor, expandQName

class QName:
    def __init__(self, tokens):
        self.qname = tokens[0]
        self.uri = None

    def resolve(self, graph):
        if self.uri is None:
            prefix, name = self.qname.split(":")
            if prefix == "":
                raise ValueError("Use of the default namespace is not permitted")
            self.uri = URIRef(expandQName(prefix, name, graph.namespace_manager))
        return self.uri

class NamespacePrefix(TranslatedSchemaElement):
    def __init__(self, tokens):
        self.prefix, self.namespace = tokens

    def translate(self, parent, translator, **kwargs):
        translator.translateNamespacePrefix(self.prefix, self.namespace, parent, **kwargs)

class RDFTranslator(Translator):
    def __init__(self, graph=None, translate_edge_props=True):
        self.graph = graph or Graph()
        self.graph.bind("sh", URIRef("http://www.w3.org/ns/shacl#"))
        self.translate_edge_props = translate_edge_props

    def translateGraph(self, graph, is_strict, imports, elements, parent, **kwargs):
        self.graph.add((self.resolve(graph.name), RDF.type, OWL.Ontology))
        super().translateGraph(graph, is_strict, imports, elements, parent, **kwargs)

    def translateImports(self, graphs, parent, **kwargs):
        self.graph += Constructor('''
            ?graph owl:imports ?imported_graph
        ''').expand(graph=self.resolve(parent.name),
                    imported_graph=[self.resolve(g) for g in graphs])

    def translateNamespacePrefix(self, prefix, namespace, parent, **kwargs):
        self.graph.bind(prefix, namespace)

    def translateProp(self, key, proptype, optional, parent, **kwargs):
        self.graph += Constructor('''
            ?prop a owl:DatatypeProperty ;
                rdfs:domain ?domain ;
                rdfs:range ?range .
            ?domain sh:property [
                sh:path ?prop ;
                sh:datatype ?range ;
                sh:minCount ?mincount ;
                sh:maxCount 1
            ]
        ''').expand(prop=key.resolve(self.graph), domain=parent, range=proptype.resolve(self.graph),
                    mincount=Literal(0 if optional else 1))

    def translateNodeType(self, name, spec, parent, **kwargs):
        superclass, props, is_open = spec.evaluate(parent, self, **kwargs)
        node = self.resolve(name)
        self.graph += Constructor('''
            ?node a rdfs:Class, sh:NodeShape ;
                rdfs:subClassOf ?superclass ;
                sh:targetClass ?node ;
                sh:closed ?closed
        ''').expand(node=node, superclass=superclass, closed=Literal(not is_open))
        if kwargs.get("reification", False):
            self.graph += Constructor('''
                ?node sh:ignoredProperties ( rdf:type rdf:subject rdf:predicate rdf:object )
            ''').expand(node=node)
        else:
            self.graph += Constructor('''
                ?node sh:ignoredProperties ( rdf:type )
            ''').expand(node=node)
        for p in props:
            p.translate(node, self)

    def translateEdgeType(self, name, edge_spec, source_spec, sink_spec, parent, **kwargs):
        edge_class, edge_props, _ = edge_spec.evaluate(parent, self, **kwargs)
        domain_class, domain_props, _ = source_spec.evaluate(parent, self, **kwargs)
        range_class, range_props, _ = sink_spec.evaluate(parent, self, **kwargs)
        if domain_props or range_props:
            logging.warning("We ignore endpoint property declarations in edge definitions, for now")
        self.graph += Constructor('''
            ?edge a owl:ObjectProperty ;
                rdfs:subPropertyOf ?superproperty ;
                rdfs:domain ?domain ;
                rdfs:range ?range .
            ?domain sh:property [
                sh:path ?edge ;
                sh:class ?range
            ]
        ''').expand(edge=name.resolve(self.graph),
                    superproperty=edge_class, domain=domain_class, range=range_class)
        if edge_props:
            if self.translate_edge_props:
                stmt = NodeType([URIRef(str(name.resolve(self.graph)) + "__Statement"), edge_spec])
                kwargs.pop("reification", None)
                stmt.translate(name, self, reification=True, **kwargs)
                self.graph += Constructor('''
                    ?edge_stmt rdfs:subClassOf rdf:Statement
                ''').expand(edge_stmt=stmt.name)
                # TODO: if there are mandatory edge props, must ensure that the original statement has been reified
            else:
                logging.warning("We ignore edge property declarations")

    def evaluateDisjunctiveLabelSpec(self, label1, label2, parent, **kwargs):
        raise NotImplementedError()

    def evaluateOptionalLabelSpec(self, label, parent, **kwargs):
        raise NotImplementedError()

    def resolve(self, thing):
        if isinstance(thing, URIRef):
            return thing
        elif isinstance(thing, QName):
            return thing.resolve(self.graph)
        else:
            raise ValueError("Cannot resolve " + str(thing))

    @classmethod
    def parser(cls, **kwargs):
        qname = Combine(Word(alphanums) + ":" + Word(alphanums))
        qname.set_parse_action(QName)
        uri = enclosed("<", ..., ">")
        uri.set_parse_action(lambda x: URIRef(x[0]))
        r = qname ^ uri
        schema = super().parser(property_type=r, key=r, label=r, type_name=r)
        sparql_prefix = Suppress("PREFIX") + Combine(Word(alphanums) + Suppress(":")) + uri
        sparql_prefix.set_parse_action(NamespacePrefix)
        return ZeroOrMore(sparql_prefix) + schema

    @classmethod
    def parse_and_translate(cls, source_string, **kwargs):
        translator = cls(**kwargs)
        for element in translator.parser().parse_string(source_string).as_list():
            element.translate(None, translator, **kwargs)
        return translator.graph

    @classmethod
    def validate(cls, data, schema, **kwargs):
        if isinstance(schema, str):
            schema = cls.parse_and_translate(schema, **kwargs)
        return pyshacl.validate(data, shacl_graph=schema, **kwargs)
