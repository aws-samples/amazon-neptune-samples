package software.amazon.neptune.onegraph.playground.server.io.serializing.lpgserializer;

/**
 * Represents a column of a NeptuneCSV file.
 */
class NeptuneCSVColumn {
    /**
     * The name of the column
     */
    public String name;

    /**
     * The data type of the column
     */
    public NeptuneCSVDataType dataType;

    /**
     * The cardinality of the column
     */
    public NeptuneCSVCardinality cardinality;
}
