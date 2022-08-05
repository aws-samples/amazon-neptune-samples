package software.amazon.neptune.onegraph.playground.server.io.parsing.ogparser;

import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Converts {@link Token}s into RDF {@link Literal}s.
 * This is a rudimentary best-effort approach, it does not handle edge cases well and might throw exceptions for them.
 * Only the following forms are supported:
 * 1. "xyz"
 * 2. "xyz"^^{@literal <http://iri.com>}
 * 3. "xyz"@en
 */
class LiteralProcessing {

    /**
     * Creates a literal from the content of the given token.
     * @param token The token to create the literal from
     * @return The Literal
     * @throws ParserException if the content of the token do not form a valid Literal.
     */
    public static Literal fromToken(Token token) throws ParserException {
        CharacterReader reader = new CharacterReader(token.content);
        String valuePart = obtainLexicalForm(token, reader);

        // Distinguish between the 3 literal forms specified in class description.
        if (token.content.length() == reader.index) {
            //  Literal is form 1.
            return SimpleValueFactory.getInstance().createLiteral(valuePart);
        } else if (reader.peek() == '^') {
            // Literal is form 2.
            IRI datatype = obtainDataType(token, reader);
            return SimpleValueFactory.getInstance().createLiteral(valuePart, datatype);
        } else if (reader.peek() == '@') {
            // Literal is form 3.
            String languageTag = obtainLanguageTag(reader);
            return SimpleValueFactory.getInstance().createLiteral(valuePart, languageTag);
        } else {
            throw ParserException.invalidLiteral(token.content, token.lineNum);
        }
    }

    // Gets the language tag from the literal string.
    private static String obtainLanguageTag(CharacterReader reader) throws ParserException {
        reader.consume();
        return reader.operatingString.substring(reader.index);
    }

    // Gets the data type from the literal string.
    private static IRI obtainDataType(Token token, CharacterReader reader) throws ParserException {

        reader.consume();
        reader.consume();
        reader.consume();

        int beginIdx = reader.index;
        char next = reader.peek();
        while (next != '>') {
            reader.consume();
            next = reader.peek();
        }
        try {
            IRI i = SimpleValueFactory.getInstance().createIRI(reader.operatingString.substring(beginIdx, reader.index));
            reader.consume();

            if (reader.canAdvance()) {
                // Done reading the data type IRI but there are still characters left in the literal.
                throw ParserException.invalidLiteral(token.content, token.lineNum);
            }
            return i;
        } catch (IllegalArgumentException e) {
            throw ParserException.invalidIRI(token.content, token.lineNum, e);
        }
    }

    // Obtains the value part (between quotes) of the literal string.
    private static String obtainLexicalForm(Token token, CharacterReader reader) throws ParserException {
        char quote = '"';

        if (reader.consume() != quote) {
            throw ParserException.invalidLiteral(token.content, token.lineNum);
        }
        char next = reader.peek();
        boolean inQuotes = true;

        while (next != '"' || inQuotes) {
            char c = reader.consume();
            next = reader.peek();
            if (next == quote && c != '\\') {
                inQuotes = false;
            }
        }
        String value = reader.operatingString.substring(1, reader.index);
        reader.consume();
        return StringEscapeUtils.unescapeJava(value);
    }
}
