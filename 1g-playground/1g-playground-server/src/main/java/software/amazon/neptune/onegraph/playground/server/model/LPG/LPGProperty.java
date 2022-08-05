package software.amazon.neptune.onegraph.playground.server.model.LPG;

import lombok.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the formal definition of a property graph property.
 * A property has a property name and zero or more property values.
 */
public class LPGProperty {

    /**
     * The name of this property
     */
    public final String name;

    /**
     * The values of this property
     */
    public Set<Object> values = new HashSet<>();

    /**
     * Creates a new property with given name
     * @param name The name
     */
    public LPGProperty(@NonNull String name) {
        this.name = name;
    }

    /**
     * Creates a new property with given name and values
     * @param name The name
     * @param values The values
     */
    public LPGProperty(@NonNull String name, Object... values) {
        this.name = name;
        for (Object value : values) {
            this.addValue(value);
        }
    }

    /**
     * Creates a new property with given name and values
     * @param name The name
     * @param values The values
     */
    public LPGProperty(@NonNull String name, Collection<?> values) {
        this.name = name;
        for (Object value : values) {
            this.addValue(value);
        }
    }

    /**
     * Adds a value to this property
     * @param value The value
     */
    public void addValue(@NonNull Object value) {
        this.values.add(value);
    }

    @Override
    public String toString() {
        return name + " : " + values;
    }
}
