package software.amazon.neptune.onegraph.playground.server.model.LPG;

import lombok.NonNull;
import lombok.Setter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Superclass of {@link LPGEdge} and {@link LPGVertex}
 */
public abstract class LPGElement {

    /**
     * Elements have identifiers just like in TinkerPop.
     */
    @Setter
    private Object id;

    // Maps property names to property objects
    private final Map<String,LPGProperty> propNameToProp = new HashMap<>();

    /**
     * Either creates a new property in the element with given value, or adds
     * the value to an already existing property.
     * @param propName The name of the property
     * @param propValue The value of the property
     */
    public void addPropertyValue(@NonNull String propName, @NonNull Object propValue) {
        if (this.propNameToProp.containsKey(propName)) {
            this.propNameToProp.get(propName).values.add(propValue);
        } else {
            this.propNameToProp.put(propName, new LPGProperty(propName, propValue));
        }
    }
    /**
     * Adds a property to this element
     * @param prop The property to add.
     */
    public void addProperty(@NonNull LPGProperty prop) {
        for (Object value : prop.values) {
            this.addPropertyValue(prop.name, value);
        }
    }

    /**
     * Returns the property with given name or empty optional if not prop of that name present
     * @param name The name of the property
     * @return The property or empty optional if not found.
     */
    public Optional<LPGProperty> propertyForName(@NonNull String name) {
        return Optional.ofNullable(propNameToProp.get(name));
    }

    /**
     * Gets the id of this element.
     * @return The id, if no id is set prior the id will be the hashCode of this element.
     */
    public Object getId() {
        if (id == null) {
            return this.hashCode();
        }
        return id;
    }

    /**
     * Gets all properties tied to this element
     * @return The properties of this element.
     */
    public Collection<LPGProperty> getProperties() {
        return propNameToProp.values();
    }
}
