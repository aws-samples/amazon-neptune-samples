package software.amazon.neptune.onegraph.playground.server.io.parsing.ogparser;

import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;

/**
 * Simplifies tokenizing a string.
 */
class CharacterReader {

    /**
     * The string this reader is currently operating on.
     */
    public String operatingString;

    /**
     * The index the reader is currently at.
     */
    public int index = 0;

    /**
     * Creates a character reader with index set to 0 and no operatingString.
     */
    public CharacterReader() {}

    /**
     * Creates a character reader with index set to 0 and the given operating string.
     * @param opStr The operating string.
     */
    public CharacterReader(String opStr) {
        this.operatingString = opStr;
        this.index = 0;
    }

    /**
     * Loads a new operating string in the reader, sets index to 0.
     * @param opStr The operating string.
     */
    public void load(String opStr) {
        this.operatingString = opStr;
        this.index = 0;
    }

    /**
     * Returns the character in the {@code operatingString} at {@code index} + {@code ahead},
     * does not advance the {@code index}.
     * @param ahead How many steps to look ahead from the {@code index}.
     * @return The character found.
     */
    public char peek(int ahead) throws ParserException {
        try {
            return operatingString.charAt(index + ahead);
        } catch (Exception e) {
            throw ParserException.unexpectedEndOfLine(this.operatingString);
        }
    }

    /**
     * Returns the character in the {@code operatingString} at {@code index}, does not advance the {@code index}.
     * @return The character found.
     * @throws ParserException If the reader was already at the end of the {@code operatingString}.
     */
    public char peek() throws ParserException {
        try {
            return operatingString.charAt(index);
        } catch (Exception e) {
            throw ParserException.unexpectedEndOfLine(this.operatingString);
        }
    }

    /**
     * Returns the character in the {@code operatingString} at {@code index}, advances the {@code index} by 1.
     * @return The character found.
     * @throws ParserException If the reader was already at the end of the {@code operatingString}.
     */
    public char consume() throws ParserException {
        try {
            char c = operatingString.charAt(index);
            index++;
            return c;
        } catch (Exception e) {
            throw ParserException.unexpectedEndOfLine(this.operatingString);
        }
    }

    /**
     * True if the reader can advance {@link #peek()} or {@link #consume()}, false otherwise.
     * @return True if the reader can advance, false otherwise.
     */
    public boolean canAdvance() {
        return index < operatingString.length();
    }
}
