package software.amazon.neptune.onegraph.playground.server.io.serializing.lpgserializer;

import lombok.Getter;

/**
 * Part of {@link NeptuneCSVColumn}, holds the cardinality of the column
 */
enum NeptuneCSVCardinality {

    SINGLE(""),

    ARRAY("[]");

    @Getter
    private final String cardinality;

    NeptuneCSVCardinality(String cardinality) {
        this.cardinality = cardinality;
    }
}
