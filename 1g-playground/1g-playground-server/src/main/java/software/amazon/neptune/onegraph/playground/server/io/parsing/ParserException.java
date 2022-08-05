package software.amazon.neptune.onegraph.playground.server.io.parsing;

import lombok.NonNull;

/**
 * All problems that occur during the parsing are thrown as
 * {@link ParserException} containing a message to help the user fix the issue.
 */
public class ParserException extends RuntimeException {

    /**
     * Creates a new parser exception with the given message.
     * @param message The exception message seen by the user.
     */
    public ParserException(@NonNull String message) {
        super(message);
    }

    /**
     * Creates a new parser exception with the given message and cause.
     * @param message The exception message seen by the user.
     * @param cause The exception that caused this exception.
     */
    public ParserException(@NonNull String message, @NonNull  Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new parser exception with error string indicating that an invalid IRI was encountered.
     * @param badEntry The invalid IRI.
     * @param lineNumber The line on which the invalid IRI was encountered.
     * @param cause The reason this is an invalid IRI.
     * @return The parser exception.
     */
    public static ParserException invalidIRI(@NonNull String badEntry, int lineNumber, @NonNull Throwable cause) {
        return new ParserException(String.format("The entry: %s on line %d is not a valid IRI.", badEntry, lineNumber), cause);
    }

    /**
     * Creates a new parser exception with error string indicating that an invalid BNode was encountered.
     * @param badEntry The invalid BNode.
     * @param lineNumber The line on which the invalid BNode was encountered.
     * @param cause The reason this is an invalid BNode.
     * @return The parser exception.
     */
    public static ParserException invalidBNode(String badEntry, int lineNumber, Throwable cause) {
        return new ParserException(String.format("The entry: %s on line %d is not a valid Blank node.", badEntry, lineNumber), cause);
    }

    /**
     * Creates a new parser exception with error string indicating that the given entry is not a BNode or IRI.
     * @param badEntry The invalid IRI or BNode.
     * @param lineNumber The line on which the invalid IRI or BNode was encountered.
     * @return The parser exception.
     */
    public static ParserException invalidIRIorBNode(@NonNull String badEntry, int lineNumber) {
        return new ParserException(String.format("The entry: %s on line %d is not a valid Blank node or IRI.", badEntry, lineNumber));
    }

    /**
     * Creates a new parser exception with error string indicating that there is a missing SID in the file
     * or if there is a reference cycle.
     * @param badEntry The referenced SID that is missing or part of a reference cycle.
     * @param lineNumber The line on which the SID was encountered.
     * @return The parser exception.
     */
    public static ParserException missingSID(@NonNull String badEntry, int lineNumber) {
        return new ParserException(String.format("The statement with statement ID: %s on line %d is not present in the file.",
                badEntry, lineNumber));
    }

    /**
     * Creates a new parser exception with error string indicating that the given entry is not a Literal.
     * @param badEntry The invalid Literal.
     * @param lineNumber The line on which the Literal was encountered.
     * @return The parser exception.
     */
    public static ParserException invalidLiteral(@NonNull String badEntry, int lineNumber) {
        String error = String.format("The entry: %s on line %d " +
                " is not a supported Literal, literals can only span 1 line and only the following forms are supported:\n" +
                "1. Lexical form only: \"lexical-form-example\"\n" +
                "2. Lexical form + data type: \"lexical-form-example\"^^<http://example.com>\n" +
                "3. Lexical form + language tag: \"lexical-form-example\"@fr\n", badEntry, lineNumber);

        return new ParserException(error);
    }

    /**
     * Creates a new parser exception with error string indicating that the given entry is not a:
     * BNode, SID, IRI or Literal, and thus does not belong in object position.
     * @param badEntry The invalid entry in object position.
     * @param lineNumber The line on which the entry was encountered.
     * @return The parser exception.
     */
    public static ParserException unexpectedEntry(String badEntry, int lineNumber) {
        return new ParserException(String.format("The entry: %s on line %d is not allowed in that position.",
                badEntry, lineNumber));
    }

    /**
     * Creates a new parser exception with error string indicating that an unexpected character was encountered.
     * @param character The unexpected character
     * @param line The line on which this character resides.
     * @return The parser exception.
     */
    public static ParserException unexpectedCharacter(char character, @NonNull  String line) {
        return new ParserException(String.format("Unexpected character \"%c\" encountered on line %s", character, line));
    }

    public static ParserException referenceCycle() {
        return new ParserException("There is a reference cycle in the data");
    }

    /**
     * Creates a new parser exception with error string indicating that there was an unexpected end of line.
     * @param line The line which ended unexpectedly.
     * @return The parser exception.
     */
    public static ParserException unexpectedEndOfLine(@NonNull String line) {
        return new ParserException(String.format("Unexpected end of line in %s", line));
    }
}
