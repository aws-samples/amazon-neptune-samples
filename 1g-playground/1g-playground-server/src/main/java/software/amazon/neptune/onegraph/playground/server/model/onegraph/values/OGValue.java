package software.amazon.neptune.onegraph.playground.server.model.onegraph.values;

import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGObject;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.RDF.LiteralConverter;
import lombok.NonNull;
import org.eclipse.rdf4j.model.Literal;

/**
 * An {@link OGValue} appears in the object position of
 * an {@link OGPropertyStatement}.
 * It has an associated {@code value} which can be any Object.
 * @param <T> Any Object
 */
public class OGValue<T> implements OGObject {

    /**
     * The associated value of this OGValue
     */
    public final T value;

    /**
     * Creates a new OGValue with given value
     * @param value The value can be any object.
     */
    public OGValue(@NonNull T value) {
        this.value = value;
    }

    /**
     * Returns the lpg value representation of the {@code value} associated with this {@link OGValue}.
     * @return An Object obtained by converting the associated {@code value}.
     */
    public Object lpgValue() {

        if (this.value instanceof Literal) {
            return LiteralConverter.convertToObject((Literal) this.value);
        } else {
            return this.value;
        }
    }

    /**
     * Returns the {@link Literal} representation of the {@code value} associated with this {@link OGValue}.
     * @return A {@link Literal} obtained by converting the associated {@code value}.
     */
    public Literal literalValue() {
        if (this.value instanceof Literal) {
            return (Literal) this.value;
        } else {
            return LiteralConverter.convertToLiteral(this.value);
        }
    }

    @Override
    public boolean canBeInNodeOfEdge() {
        return false;
    }

    @Override
    public String toString() {
        return "\"" + this.value + "\"";
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OGValue) {
            return ((OGValue<?>) obj).value.equals(this.value);
        }
        return false;
    }
}
