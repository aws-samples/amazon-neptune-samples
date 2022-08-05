package software.amazon.neptune.onegraph.playground.client.api;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Enum that specifies which LPG mapping configuration the user wants to use.
 */
public enum ConfigType {

    /**
     * An empty mapping configuration means that no properties or labels get translated when loading an LPG.
     * The predicate of a label-statement gets set to RDF.TYPE.
     */
    EMPTY("EMPTY"),

    /**
     * A default mapping configuration means that all properties and labels get translated to IRIs when loading an LPG.
     * The predicate of a label-statement gets set to RDF.TYPE.
     */
    DEFAULT("DEFAULT"),

    /**
     * A custom configuration means that the users specify themselves which property names or labels gets translated.
     */
    CUSTOM("CUSTOM");

    private final String underlying;

    ConfigType(String dtString) {
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
    public static ConfigType configTypeFromCLIArgument(String argument) throws IllegalArgumentException {
        try {
            return ConfigType.valueOf(argument.toUpperCase());
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Mapping configuration type should be one of: " + Arrays.toString(DataFormat.values()), e);
        }
    }
}

