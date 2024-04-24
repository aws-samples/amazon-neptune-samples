import sys
from rdfpgschema import RDFTranslator

if __name__ == "__main__":
    RDFTranslator.parse_and_translate('''
        PREFIX ex: <https://aws.amazon.com/schema/example#>
        CREATE GRAPH TYPE ex:Friends STRICT IMPORTS ex:Base {
            (ex:Thing OPEN { ex:name xsd:string }),
            (ex:Person : ex:Thing & ex:Something {OPTIONAL ex:gender xsd:string}),
            (: ex:Person)-[ex:hasFriend { OPTIONAL ex:since xsd:date }] -> (: ex:Person)
        }
    ''').serialize(sys.stdout.buffer)
