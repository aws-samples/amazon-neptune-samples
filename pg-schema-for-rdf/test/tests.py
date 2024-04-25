import io
import unittest
from rdflib import Namespace, RDF, OWL, Graph
import pgschema
import rdfpgschema

ex1 = '''
    CREATE GRAPH TYPE fraudGraphType STRICT {
        ( personType : Person { name STRING , OPTIONAL birthday DATE }) ,
        ( customerType : Person & Customer { name STRING , OPTIONAL since DATE }) ,
        ( suspiciousType : Suspicious OPEN { reason STRING , OPEN }) ,
        (: personType | customerType )
            -[ friendType : Knows & Likes ] ->
        (: personType | customerType )
    }
'''
ex2 = '''
    CREATE GRAPH TYPE fraudGraphType STRICT {
        ( personType : Person { name STRING }) ,
        ( customerType : personType & Customer { id INT32}) ,
        ( creditCardType : CreditCard { num STRING }) ,
        ( transactionType : Transaction { num STRING }) ,
        ( accountType : Account { id INT32 }) ,
        (: customerType )
            -[ ownsType : owns ] ->
        (: accountType ) ,
        (: customerType )
            -[ usesType : uses ] ->
        (: creditCardType ) ,
        (: transactionType )
            -[ chargesType : charges { amount DOUBLE }] ->
        (: creditCardType ) ,
        (: transactionType )
            -[ activityType : deposits | withdraws ] ->
        (: accountType )
    }
'''

class PlainParserTests(unittest.TestCase):

    def testPlainParser1(self):
        ast = pgschema.Translator.parser().parse_string(ex1).as_list()
        self.assertTrue(len(ast) == 1)
        self.assertTrue(isinstance(ast[0], pgschema.Graph))

    def testPlainParser2(self):
        ast = pgschema.Translator.parser().parse_string(ex2).as_list()
        self.assertTrue(len(ast) == 1)
        self.assertTrue(isinstance(ast[0], pgschema.Graph))

class RDFTranslatorTests(unittest.TestCase):

    def setUp(self) -> None:
        self.ex = Namespace("http://o.ra/")

    def testMinimalNodeType(self):
        g = rdfpgschema.RDFTranslator.parse_and_translate('''
            PREFIX ex: <http://o.ra/>
            CREATE NODE TYPE (ex:Foo)
        ''')
        self.assertTrue(len(g) == 7)
        self.assertTrue(len(list(g.triples((self.ex.Foo, None, None)))) == 5)

    def testMinimalEdgeType(self):
        g = rdfpgschema.RDFTranslator.parse_and_translate('''
            PREFIX ex: <http://o.ra/>
            CREATE EDGE TYPE (: ex:Foo)-[ex:bar]->(: ex:Foo)
        ''')
        self.assertTrue(len(g) == 6)
        self.assertTrue(len(list(g.triples((self.ex.Foo, None, None)))) == 1)
        self.assertTrue(len(list(g.triples((self.ex.bar, None, None)))) == 3)

    def testMinimalGraphType(self):
        g = rdfpgschema.RDFTranslator.parse_and_translate('''
            PREFIX ex: <http://o.ra/>
            CREATE GRAPH TYPE ex:Foo STRICT {}
        ''')
        self.assertTrue(len(g) == 1)
        self.assertTrue(len(list(g.triples((self.ex.Foo, RDF.type, OWL.Ontology)))) == 1)

class ValidationTests(unittest.TestCase):

    SCHEMA = '''
        PREFIX ex: <http://o.ra/>
        CREATE GRAPH TYPE ex:Foo STRICT {
            (ex:Bar { ex:baz xsd:integer })
        }
    '''
    VALID_RDF = '''
        PREFIX ex: <http://o.ra/>
        ex:bar a ex:Bar ;
            ex:baz 1 .
    '''

    INVALID_RDF = '''
        PREFIX ex: <http://o.ra/>
        ex:bar a ex:Bar ;
            ex:baz "foo" .
    '''

    VALID_RDF_EXTRA_DATA = '''
        PREFIX ex: <http://o.ra/>
        ex:bar a ex:Bar ;
            ex:baz 1 ;
            ex:barbar "foo" .
    '''

    @staticmethod
    def runTest(data, schema):
        g = Graph()
        g.parse(data=data)
        s = rdfpgschema.RDFTranslator.parse_and_translate(schema)
        return rdfpgschema.RDFTranslator.validate(g, s)

    def testValidRDF(self):
        result = self.runTest(self.VALID_RDF, self.SCHEMA)
        self.assertTrue(result[0])

    def testInvalidRDF(self):
        result = self.runTest(self.INVALID_RDF, self.SCHEMA)
        self.assertFalse(result[0])
        # serialize(result[1])

    def testExtraRDF(self):
        result = self.runTest(self.VALID_RDF_EXTRA_DATA, self.SCHEMA)
        self.assertFalse(result[0])
        # serialize(result[1])

def serialize(graph, fmt="turtle"):
    # Helper to use with unittests in PyCharm (sys.stdout.buffer not readily usable)
    with io.BytesIO() as s:
        graph.serialize(s, format=fmt)
        print(s.getvalue().decode())
