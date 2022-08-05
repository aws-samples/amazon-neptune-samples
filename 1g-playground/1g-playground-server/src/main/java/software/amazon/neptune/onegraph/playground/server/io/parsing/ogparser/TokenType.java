package software.amazon.neptune.onegraph.playground.server.io.parsing.ogparser;

/**
 * The different types a {@link Token} can be during parsing in {@link OGParser}.
 * The exact format of some of these tokens can be found in the
 * <a href="https://www.w3.org/TR/n-triples/">N-Triples specification</a>.
 */
enum TokenType {

    /**
     * IRI token type, see the "IRIs" section of the <a href="https://www.w3.org/TR/n-triples/#sec-iri">N-Triples specification</a>.
     */
    IRI,

    /**
     * Blank node token type, see the "RDF Blank Nodes" section of the <a href="https://www.w3.org/TR/n-triples/#BNodes">N-Triples specification</a>.
     */
    BNODE,

    /**
     * Literal token type, see the "RDF Literals" section of the <a href="https://www.w3.org/TR/n-triples/#sec-literals">N-Triples specification</a>.
     */
    LITERAL,

    /**
     * The word 'in'
     */
    IN,

    /**
     * Statement identifier, looks like:
     * {@literal __:statementID}
     */
    SID,

    /**
     * The character 'U'
     */
    FLAG_U,

    /**
     * The '#'
     */
    COMMENT
}
