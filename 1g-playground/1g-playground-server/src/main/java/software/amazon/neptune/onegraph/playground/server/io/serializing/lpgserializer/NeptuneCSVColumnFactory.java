package software.amazon.neptune.onegraph.playground.server.io.serializing.lpgserializer;

import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGElement;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Factory class used to instantiate a collection {@link NeptuneCSVColumn}s from given a collection {@link LPGElement}s.
 */
class NeptuneCSVColumnFactory {

    /**
     * Creates columns for the csv with proper name, datatype, and cardinality
     * @param elements The elements to create columns for, all properties of the element will be inspected to infer
     *                 column name, data type and cardinality.
     * @return A collection of columns.
     */
    public static Collection<NeptuneCSVColumn> createNeptuneColumnsForElements(Collection<? extends LPGElement> elements) {
        // Maps column names to columns
        Map<String, NeptuneCSVColumn> columns = new HashMap<>();
        for (LPGElement e : elements) {
            for (LPGProperty p : e.getProperties()) {
                // Figure out the cardinality and data type of this property
                NeptuneCSVCardinality cardinality = cardinalityForProperty(p);
                NeptuneCSVDataType dType = dataTypeForProperty(p);

                // Check if we already have a column with the same name
                if (columns.containsKey(p.name)) {
                    NeptuneCSVColumn column = columns.get(p.name);
                    // Adjust the current column if needed.
                    // If the datatype differs from the current one stored; make datatype string.
                    // Array cardinality always takes precedence as well.
                    if (column.dataType != dType) {
                        column.dataType = NeptuneCSVDataType.STRING;
                    }
                    if (cardinality == NeptuneCSVCardinality.ARRAY) {
                        column.cardinality = NeptuneCSVCardinality.ARRAY;
                    }
                } else {
                    NeptuneCSVColumn c = new NeptuneCSVColumn();
                    c.name = p.name;
                    c.cardinality = cardinality;
                    c.dataType = dType;
                    columns.put(c.name, c);
                }
            }
        }
        return columns.values();
    }

    private static NeptuneCSVCardinality cardinalityForProperty(LPGProperty p) {
        if (p.values.size() > 1) {
            return NeptuneCSVCardinality.ARRAY;
        }
        return NeptuneCSVCardinality.SINGLE;
    }

    // A property can hold multiple values, when those values differ in data type,
    // the data type in the NeptuneCSV for that property become "String"
    private static NeptuneCSVDataType dataTypeForProperty(LPGProperty property) {
        if (property.values.size() == 0) {
            return NeptuneCSVDataType.EMPTY;
        }
        Set<NeptuneCSVDataType> dataTypesFound = new HashSet<>();
        for (Object value : property.values) {
            NeptuneCSVDataType t = NeptuneCSVDataTypeFactory.dataTypeFromObject(value);
            dataTypesFound.add(t);
        }

        if (dataTypesFound.size() > 1) {
            // When there are multiple data types found for a property we make the datatype string.
            return NeptuneCSVDataType.STRING;
        }
        return dataTypesFound.iterator().next();
    }
}
