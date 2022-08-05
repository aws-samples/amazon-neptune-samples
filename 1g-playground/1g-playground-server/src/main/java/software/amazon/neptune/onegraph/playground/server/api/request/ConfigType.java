package software.amazon.neptune.onegraph.playground.server.api.request;

import com.fasterxml.jackson.annotation.JsonValue;
import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;

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
     * see {@link URLConstants} for the base-IRIs for the namespace, the local name will be the property name or label.
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
}
