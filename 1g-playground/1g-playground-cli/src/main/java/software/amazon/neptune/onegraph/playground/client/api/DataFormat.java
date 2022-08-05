package software.amazon.neptune.onegraph.playground.client.api;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Data format, passed as parameter during export, load and view requests to specify which data format to use.
 */
public enum DataFormat {

    /**
     * The graphSON labeled property graph data format.
     */
    GRAPHSON("GRAPHSON"),

    /**
     * The graphML labeled property graph data format.
     */
    GRAPHML("GRAPHML"),

    /**
     * The NeptuneCSV (see Gremlin Load Data Format - Amazon Neptune) data format.
     */
    NEPTUNECSV("NEPTUNECSV"),

    /**
     * Custom OneGraph data format.
     */
    OG("OG"),

    /**
     * The RDF/XML RDF data format.
     */
    RDFXML("RDFXML"),

    /**
     * The Turtle RDF data format.
     */
    TURTLE("TURTLE"),

    /**
     * The TriG RDF data format.
     */
    TRIG("TRIG"),

    /**
     * The N-Quads RDF data format.
     */
    NQUADS("NQUADS"),

    /**
     * The N-Triples RDF data format.
     */
    NTRIPLES("NTRIPLES"),

    /**
     * The formal data format specifies that the data should be represented as: <br />
     * - Relationship statements <br />
     * - Property statements <br />
     * - Membership statements <br />
     * - Simple nodes <br />
     * - Graphs <br />
     */
    FORMAL("FORMAL");

    private final String underlying;

    DataFormat(String dtString) {
        this.underlying = dtString;
    }

    @JsonValue
    public String getUnderlying() {
        return this.underlying;
    }

    @Override
    public String toString() {
        return underlying;
    }

    /**
     * Creats a new {@link ConfigType} by parsing the CLI {@code argument}.
     * @param argument Can take three values: custom, default or empty.
     * @return The config type inferred from the {@code argument}.
     * @throws IllegalArgumentException If the {@code argument} is not one of custom, default or empty.
     */
    public static DataFormat dataFormatFromCLIArgument(String argument) throws IllegalArgumentException {
        try {
            return DataFormat.valueOf(argument.toUpperCase());
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Data format should be one of: " + Arrays.toString(DataFormat.values()), e);
        }
    }
}
