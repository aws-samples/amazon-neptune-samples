package software.amazon.neptune.onegraph.playground.server.io.parsing.ogparser;

import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;

import java.util.Optional;

import static software.amazon.neptune.onegraph.playground.server.io.parsing.ogparser.TokenType.COMMENT;

/**
 * Tokenizes lines of OG data files.
 */
class Tokenizer {

    /**
     * Gets the next token that can be found on the line, if the type of the token is not recognized,
     * produces an exception, see {@link TokenType}.
     * @param reader The reader with {@link CharacterReader#operatingString} set to the full line.
     * @return The next token found on the {@link CharacterReader#operatingString} or the empty optional if
     * no more tokens can be found on this line.
     * @throws ParserException If an error occurs during the tokenization.
     */
    public Optional<Token> nextToken(CharacterReader reader) throws ParserException {
        while(reader.canAdvance()) {
            char next = reader.peek();

            switch (next) {
                // Continue when reading the following characters.
                case '\\':
                case ',':
                case ' ':
                    reader.consume();
                    break;
                // Start read of IRI.
                case '<':
                    return Optional.of(processIRI(reader));
                // Start read of Blank node OR Statement ID.
                case '_':
                    if (reader.peek(1) == '_') {
                        return Optional.of(processSIDorBNode(reader, TokenType.SID));
                    }
                    return Optional.of(processSIDorBNode(reader, TokenType.BNODE));
                // Start read of Flag U
                case 'U':
                    return Optional.of(processU(reader));
                // Start read of a Literal
                case '"':
                    return Optional.of(processLiteral(reader));
                // Start read of EOL character (the .) or begin of comment (the #).
                case '.':
                    return Optional.empty();
                case '#':
                    // Returning the empty optional here marks that there are no more tokens on this line.
                    return Optional.of(new Token("#", COMMENT));
                    // Start read of 'in'
                case 'i':
                    if (reader.peek(1) == 'n') {
                        return Optional.of(processIN(reader));
                    }
                    // Deliberate fallthrough.
                default:
                    throw ParserException.unexpectedCharacter(reader.peek(), reader.operatingString);
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a token of type Literal.
     * @param reader The reader, the {@code index} should be set to the very beginning of the token.
     * @return The token
     * @throws ParserException If an exception occurred during the tokenization of this literal.
     */
    public Token processLiteral(CharacterReader reader) throws ParserException {
        int beginIndex = reader.index;
        reader.consume();

        char next = reader.peek();
        char quote = '"';
        boolean inQuotes = true;

        while (next != ' ' || inQuotes) {
            char c = reader.consume();
            next = reader.peek();
            if (next == quote && c != '\\') {
                inQuotes = false;
            }
        }
        return new Token(reader.operatingString.substring(beginIndex, reader.index), TokenType.LITERAL);
    }

    /**
     * Creates a token of type IRI.
     * @param reader The reader, the {@code index} should be set to the very beginning of the token.
     * @return The token
     * @throws ParserException If an exception occurred during the tokenization of this IRI.
     */
    public Token processIRI(CharacterReader reader) throws ParserException {
        int beginIndex = reader.index + 1;

        char c = reader.consume();
        while (c != '>') {
            c = reader.consume();
        }
        return new Token(reader.operatingString.substring(beginIndex, reader.index - 1), TokenType.IRI);
    }

    /**
     * Creates a token of type either SID or BNODE, depending on given token type.
     * @param reader The reader, the {@code index} should be set to the very beginning of the token.
     * @return The token
     * @throws ParserException If an exception occurred during the tokenization of this SID/BNode.
     */
    public Token processSIDorBNode(CharacterReader reader, TokenType type) {
        int beginIndex = reader.index;
        reader.consume();
        char next = reader.peek();

        while (next != ' ' && next != '<' && next != ',') {
            reader.consume();
            next = reader.peek();
        }
        return new Token(reader.operatingString.substring(beginIndex, reader.index), type);
    }

    /**
     * Creates a token of type IN.
     * @param reader The reader, the {@code index} should be set to the very beginning of the token.
     * @return The token
     * @throws ParserException If an exception occurred during the tokenization of this IN.
     */
    public Token processIN(CharacterReader reader) {
        reader.consume();
        reader.consume();
        return new Token("in", TokenType.IN);
    }

    /**
     * Creates a token of type FLAG_U.
     * @param reader The reader, the {@code index} should be set to the very beginning of the token.
     * @return The token
     * @throws ParserException If an exception occurred during the tokenization of this IN.
     */
    public Token processU(CharacterReader reader) {
        reader.consume();
        return new Token("U", TokenType.FLAG_U);
    }
}
