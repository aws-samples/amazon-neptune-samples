package software.amazon.neptune.onegraph.playground.server.model.RDF;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import java.util.Date;

/**
 * Performs conversions between Objects and RDF {@link Literal}s.
 */
public class LiteralConverter {

    /**
     * Creates a literal for the given Object
     * @param o The Object to create a literal for.
     * @return the created literal.
     */
    public static Literal convertToLiteral(Object o) {
        if (o instanceof Literal) {
            return (Literal) o;
        } else if (o instanceof Byte) {
            return SimpleValueFactory.getInstance().createLiteral((Byte) o);
        } else if (o instanceof Short) {
            return SimpleValueFactory.getInstance().createLiteral((Short) o);
        } else if (o instanceof Integer) {
            return SimpleValueFactory.getInstance().createLiteral((Integer) o);
        } else if (o instanceof Long) {
            return SimpleValueFactory.getInstance().createLiteral((Long) o);
        } else if (o instanceof Float) {
            return SimpleValueFactory.getInstance().createLiteral((Float) o);
        } else if (o instanceof Double) {
            return SimpleValueFactory.getInstance().createLiteral((Double) o);
        } else if (o instanceof Date) {
            return SimpleValueFactory.getInstance().createLiteral((Date) o);
        } else {
            return SimpleValueFactory.getInstance().createLiteral(o.toString());
        }
    }

    /**
     * Converts the given literal to an Object.
     * @param l The literal to convert, language tag is ignored.
     * @return Object: (Byte, Short, Integer, Long, Float, Double, Date or String)
     */
    public static Object convertToObject(Literal l) {
        if (l.getDatatype().equals(XMLSchema.BYTE)) {
            return l.byteValue();
        } else if (l.getDatatype().equals(XMLSchema.SHORT)) {
            return l.shortValue();
        } else if (isIntDataType(l)) {
            return l.intValue();
        } else if (isIntegerDataType(l)) {
            return l.integerValue().intValue();
        } else if (l.getDatatype().equals(XMLSchema.DECIMAL)) {
            return l.decimalValue().intValue();
        } else if (l.getDatatype().equals(XMLSchema.LONG)) {
            return l.longValue();
        } else if (l.getDatatype().equals(XMLSchema.FLOAT)) {
            return l.floatValue();
        } else if (l.getDatatype().equals(XMLSchema.DOUBLE)) {
            return l.doubleValue();
        } else if (isDateDataType(l)) {
            try {
                return l.calendarValue().toGregorianCalendar().getTime();
            } catch (IllegalArgumentException e) {
                return l.stringValue();
            }
        } else if (l.getDatatype().equals(XMLSchema.STRING)) {
            return l.stringValue();
        } else {
            return l.toString();
        }
    }

    // True if the dataType of an int type.
    private static boolean isIntDataType(Literal l) {
        return (l.getDatatype().equals(XMLSchema.INT) || l.getDatatype().equals(XMLSchema.UNSIGNED_INT));
    }

    // True if the dataType of an integer type.
    private static boolean isIntegerDataType(Literal l) {
        return (l.getDatatype().equals(XMLSchema.INTEGER) ||
                l.getDatatype().equals(XMLSchema.NEGATIVE_INTEGER) ||
                l.getDatatype().equals(XMLSchema.POSITIVE_INTEGER) ||
                l.getDatatype().equals(XMLSchema.NON_NEGATIVE_INTEGER) ||
                l.getDatatype().equals(XMLSchema.NON_POSITIVE_INTEGER));
    }

    // True if the dataType of a date type.
    private static boolean isDateDataType(Literal l) {
        return (l.getDatatype().equals(XMLSchema.DATE) ||
                l.getDatatype().equals(XMLSchema.DATETIME) ||
                l.getDatatype().equals(XMLSchema.DATETIMESTAMP) ||
                l.getDatatype().equals(XMLSchema.TIME));
    }
}
