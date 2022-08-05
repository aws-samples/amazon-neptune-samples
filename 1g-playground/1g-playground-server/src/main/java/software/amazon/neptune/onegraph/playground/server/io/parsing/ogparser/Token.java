package software.amazon.neptune.onegraph.playground.server.io.parsing.ogparser;

/**
 * A {@link Token} is a single "word" in a serialized OG data set.
 * e.g "_:A :knows _:B __:SID1 in :graph1" contains 6 {@link Token}s.
 */
class Token {

    /**
     * The raw string content of the token.
     */
    public String content;

    /**
     * A token keeps track on which line it is, for error messaging.
     */
    public int lineNum = 0;

    /**
     * The type of the token. Note: A token being a certain type does not mean that that token is not malformed,
     * For example, the parser may assign a token the type IRI because an opening tag was seen (<), but this does
     * not mean that this is then a valid IRI.
     */
    public TokenType type;

    /**
     * Creates a new Token with underlying content and type
     * @param content The raw string of the token, this is not validated.
     * @param type The assumed type of the raw string.
     */
    public Token(String content, TokenType type) {
        this.content = content;
        this.type = type;
    }
}
