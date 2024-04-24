import abc
from pyparsing import Opt, Suppress, Group, OneOrMore, ZeroOrMore, Word, Forward, Literal, alphanums

def enclosed(left_bracket, expression, right_bracket):
    return Suppress(left_bracket) + expression + Suppress(right_bracket)

class Translator(abc.ABC):

    def translateGraph(self, graph, is_strict, imports, elements, parent, **kwargs):
        if imports:
            imports.translate(graph, self, **kwargs)
        for e in elements:
            e.translate(graph, self, **kwargs)

    @abc.abstractmethod
    def translateImports(self, graphs, parent, **kwargs):
        ...

    @abc.abstractmethod
    def translateNamespacePrefix(self, prefix, namespace, parent, **kwargs):
        ...

    @abc.abstractmethod
    def translateProp(self, key, proptype, optional, parent, **kwargs):
        ...

    @abc.abstractmethod
    def translateNodeType(self, name, spec, parent, **kwargs):
        ...

    @abc.abstractmethod
    def translateEdgeType(self, name, edge_spec, source_spec, sink_spec, parent, **kwargs):
        ...

    def evaluateSimpleLabelSpec(self, label, parent, **kwargs):
        return [self.resolve(label)], [], False

    def evaluateConjunctiveLabelSpec(self, label1, label2, parent, **kwargs):
        classes1, props1, is_open1 = label1.evaluate(parent, self, **kwargs)
        classes2, props2, is_open2 = label2.evaluate(parent, self, **kwargs)
        return classes1 + classes2, props1 + props2, is_open1 and is_open2

    @abc.abstractmethod
    def evaluateDisjunctiveLabelSpec(self, label1, label2, parent, **kwargs):
        ...

    @abc.abstractmethod
    def evaluateOptionalLabelSpec(self, label, parent, **kwargs):
        ...

    def evaluateLabelPropertySpec(self, label_spec, prop_spec, is_open, parent, **kwargs):
        if label_spec:
            label, _, _ = label_spec.evaluate(parent, self, **kwargs)
        else:
            label = None
        if prop_spec:
            _, props, props_open = prop_spec.evaluate(parent, self, **kwargs)
        else:
            props, props_open = [], False
        return label, props, props_open or is_open

    @abc.abstractmethod
    def resolve(self, thing):
        ...

    @classmethod
    def parser(cls, property_type=None, key=None, label=None, type_name=None, colon=":"):
        property_type = property_type or Word(alphanums + ":")
        key = key or Word(alphanums + ":")
        label = label or Word(alphanums + ":")
        type_name = type_name or Word(alphanums + ":")
        # Property
        prop = Opt("OPTIONAL") + key + property_type
        prop.set_parse_action(Prop)
        # PropertySpec
        properties = Group(prop + ZeroOrMore(Suppress(",") + prop), aslist=True)
        property_spec = enclosed("{", Opt((properties + Opt(Suppress(",") + "OPEN")) ^ "OPEN"), "}")
        property_spec.set_parse_action(PropertySpec)
        # LabelSpec
        label_spec = Forward()
        non_comb_label_spec = (enclosed("(", label_spec, ")")
                               ^ enclosed("[", label_spec, "]")
                               ^ label
                               ^ type_name)
        non_comb_label_spec.set_parse_action(LabelSpec.make)
        label_spec <<= (non_comb_label_spec
                        ^ (non_comb_label_spec + ((Literal("|") ^ Literal("&")) + label_spec) ^ "?"))
        label_spec.set_parse_action(LabelSpec.make)
        # LabelPropertySpec
        label_property_spec = Opt(Suppress(colon) + label_spec) + Opt("OPEN") + Opt(property_spec)
        label_property_spec.set_parse_action(LabelPropertySpec)
        # EdgeType
        endpoint_type = enclosed("(", label_property_spec, ")")
        middle_type = enclosed("[", type_name + label_property_spec, "]")
        edge_type = endpoint_type + enclosed("-", middle_type, "->") + endpoint_type
        edge_type.set_parse_action(EdgeType)
        # NodeType
        node_type = enclosed("(", type_name + label_property_spec, ")")
        node_type.set_parse_action(NodeType)
        # GraphTypeImports
        graph_type_imports = (Suppress("IMPORTS")
                              + Group(type_name + ZeroOrMore(Suppress(",") + type_name), aslist=True))
        graph_type_imports.set_parse_action(Imports)
        # GraphType
        element_type = type_name ^ node_type ^ edge_type
        element_types = Group(element_type + ZeroOrMore(Suppress(",") + element_type), aslist=True)
        graph_type_elements = enclosed("{", Opt(element_types), "}")
        graph_type_mode = Literal("STRICT") ^ Literal("LOOSE")
        graph_type = type_name + graph_type_mode + Opt(graph_type_imports) + graph_type_elements
        graph_type.set_parse_action(Graph)
        # Top level forms
        create_graph_type = Suppress(Literal("CREATE") + "GRAPH" + "TYPE") + graph_type
        create_edge_type = Suppress(Literal("CREATE") + "EDGE" + "TYPE") + Opt("ABSTRACT") + edge_type
        create_node_type = Suppress(Literal("CREATE") + "NODE" + "TYPE") + Opt("ABSTRACT") + node_type
        create_type = create_node_type ^ create_edge_type ^ create_graph_type
        return OneOrMore(create_type + Suppress(Opt(";")))

class TranslatedSchemaElement(abc.ABC):

    # Delegates translation of the element to the translator
    @abc.abstractmethod
    def translate(self, parent, translator, **kwargs):
        ...

class EvaluatedSchemaElement(abc.ABC):

    # Evaluates the element, returns three values: "resolved" label (translator-dependent value),
    #  properties (list of Prop instances), and whether the definition is "open" (boolean)
    @abc.abstractmethod
    def evaluate(self, parent, translator, **kwargs):
        ...

class Graph(TranslatedSchemaElement):
    def __init__(self, tokens):
        self.name = tokens[0]
        self.isStrict = tokens[1] == "STRICT"
        if len(tokens) > 2:
            if isinstance(tokens[2], Imports):
                self.imports, self.elements = tokens[2], tokens[3]
            else:
                self.imports, self.elements = None, tokens[2]
        else:
            self.imports, self.elements = None, []

    def translate(self, parent, translator, **kwargs):
        translator.translateGraph(self, self.isStrict, self.imports, self.elements,
                                  parent, **kwargs)

class Imports(TranslatedSchemaElement):
    def __init__(self, tokens):
        self.graphs = tokens[0]

    def translate(self, parent, translator, **kwargs):
        translator.translateImports(self.graphs, parent, **kwargs)

class Prop(TranslatedSchemaElement):
    def __init__(self, tokens):
        if tokens[0] == "OPTIONAL":
            self.optional, self.key, self.type = True, tokens[1], tokens[2]
        else:
            self.optional, self.key, self.type = False, tokens[0], tokens[1]

    def translate(self, parent, translator, **kwargs):
        translator.translateProp(self.key, self.type, self.optional, parent, **kwargs)

class NodeType(TranslatedSchemaElement):
    def __init__(self, tokens):
        self.name, self.spec = tokens

    def translate(self, parent, translator, **kwargs):
        translator.translateNodeType(self.name, self.spec, parent, **kwargs)

class EdgeType(TranslatedSchemaElement):
    def __init__(self, tokens):
        self.source_spec, self.name, self.edge_spec, self.sink_spec = tokens

    def translate(self, parent, translator, **kwargs):
        translator.translateEdgeType(self.name, self.edge_spec, self.source_spec, self.sink_spec,
                                     parent, **kwargs)

class LabelSpec(EvaluatedSchemaElement, abc.ABC):
    @classmethod
    def make(cls, tokens):
        if len(tokens) == 1:
            label = tokens[0]
            return label if isinstance(label, LabelSpec) else SimpleLabelSpec(label)
        else:
            match tokens[1]:
                case "&": return ConjunctiveLabelSpec(tokens[0], tokens[2])
                case "|": return DisjunctiveLabelSpec(tokens[0], tokens[2])
                case "?": return OptionalLabelSpec(tokens[0])
                case _: raise ValueError(tokens)

    def evaluate(self, parent, translator, **kwargs):
        raise NotImplementedError()

class SimpleLabelSpec(LabelSpec):
    def __init__(self, label):
        self.label = label

    def evaluate(self, parent, translator, **kwargs):
        return translator.evaluateSimpleLabelSpec(self.label, parent, **kwargs)

class ConjunctiveLabelSpec(LabelSpec):
    def __init__(self, label1, label2):
        self.label1 = label1
        self.label2 = label2

    def evaluate(self, parent, translator, **kwargs):
        return translator.evaluateConjunctiveLabelSpec(self.label1, self.label2, parent, **kwargs)

class DisjunctiveLabelSpec(LabelSpec):
    def __init__(self, label1, label2):
        self.label1 = label1
        self.label2 = label2

    def evaluate(self, parent, translator, **kwargs):
        return translator.evaluateDisjunctiveLabelSpec(self.label1, self.label2, parent, **kwargs)

class OptionalLabelSpec(LabelSpec):
    def __init__(self, label):
        self.label = label

    def evaluate(self, parent, translator, **kwargs):
        return translator.evaluateOptionalLabelSpec(self.label, parent, **kwargs)

class LabelPropertySpec(EvaluatedSchemaElement):
    def __init__(self, tokens):
        if tokens:
            if isinstance(tokens[0], LabelSpec):
                self.label_spec = tokens[0]
                tokens = tokens[1:]
            else:
                self.label_spec = None
            if tokens and tokens[0] == "OPEN":
                self.isOpen = True
                tokens = tokens[1:]
            else:
                self.isOpen = False
            self.prop_spec = tokens[0] if tokens else None
        else:
            self.label_spec, self.prop_spec, self.isOpen = None, None, False

    def evaluate(self, parent, translator, **kwargs):
        return translator.evaluateLabelPropertySpec(self.label_spec, self.prop_spec, self.isOpen,
                                                    parent, **kwargs)

class PropertySpec(EvaluatedSchemaElement):
    def __init__(self, tokens):
        self.properties = None
        self.isOpen = False
        if tokens:
            if tokens[0] == "OPEN":
                self.isOpen = True
            else:
                self.properties = tokens[0]
                if len(tokens) > 1 and tokens[1] == "OPEN":
                    self.isOpen = True

    def evaluate(self, parent, translator, **kwargs):
        return None, self.properties, self.isOpen
