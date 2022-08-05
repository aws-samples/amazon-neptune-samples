package software.amazon.neptune.onegraph.playground.server.io.serializing.lpgserializer;

import com.google.common.collect.ImmutableMap;

/**
 * Factory class used to instantiate a {@link NeptuneCSVDataType} from given a given {@link Object}.
 */
public class NeptuneCSVDataTypeFactory {

    private static final ImmutableMap<String, NeptuneCSVDataType> CLASS_NAME_TO_DATA_TYPE
            = ImmutableMap.<String, NeptuneCSVDataType>builder()
            .put("boolean", NeptuneCSVDataType.BOOL)
            .put("bool", NeptuneCSVDataType.BOOL)
            .put("string", NeptuneCSVDataType.STRING)
            .put("integer", NeptuneCSVDataType.INT)
            .put("int", NeptuneCSVDataType.INT)
            .put("double", NeptuneCSVDataType.DOUBLE)
            .put("short", NeptuneCSVDataType.SHORT)
            .put("byte", NeptuneCSVDataType.BYTE)
            .put("long", NeptuneCSVDataType.LONG)
            .put("date", NeptuneCSVDataType.DATE)
            .put("float", NeptuneCSVDataType.FLOAT)
            .build();

    /**
     * Infers NeptuneCSV data type from {@link Class} of given {@code value}.
     * @param value The value to obtain a data type from.
     * @return The data type closest to the {@link Class} of the given {@code value};
     */
    public static NeptuneCSVDataType dataTypeFromObject(Object value) {
        String className = value.getClass().getSimpleName().toLowerCase();
        if (!CLASS_NAME_TO_DATA_TYPE.containsKey(className)) {
            return NeptuneCSVDataType.STRING;
        }
        return CLASS_NAME_TO_DATA_TYPE.get(className);
    }
}
