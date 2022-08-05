package software.amazon.neptune.onegraph.playground.server.io.serializing.lpgserializer;

import lombok.Getter;

/**
 * Part of {@link NeptuneCSVColumn}, holds the data type of the column
 */
enum NeptuneCSVDataType {

    BOOL("Bool"),

    BYTE("Byte"),

    SHORT("Short"),

    INT("Int"),

    LONG("Long"),

    FLOAT("Float"),

    DOUBLE("Double"),

    STRING("String"),

    DATE("Date"),

    EMPTY("");

    @Getter
    private final String type;

    NeptuneCSVDataType(String type) {
        this.type = type;
    }
}
