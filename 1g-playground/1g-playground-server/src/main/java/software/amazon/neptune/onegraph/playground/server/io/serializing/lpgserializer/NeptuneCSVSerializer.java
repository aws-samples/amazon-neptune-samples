package software.amazon.neptune.onegraph.playground.server.io.serializing.lpgserializer;

import software.amazon.neptune.onegraph.playground.server.io.serializing.SerializerException;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import lombok.NonNull;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Serializes {@link LPGGraph}s to file, in the NeptuneCSV file format, used by
 * {@link LPGSerializer}.
 */
class NeptuneCSVSerializer {

    /**
     * Serializes the given {@code graph} to the NeptuneCSV format.
     * @param outNodes The output stream to write the nodes to.
     * @param outEdges The output stream to write the edges to.
     * @param graph The graph to serialize.
     * @throws SerializerException thrown when an exception occurs during serializing.
     */
    public static void serializeToNeptuneCSV(@NonNull OutputStream outNodes,
                                             @NonNull  OutputStream outEdges,
                                             @NonNull LPGGraph graph) throws SerializerException {

        StringBuffer nodesBuffer = new StringBuffer();
        StringBuffer edgesBuffer = new StringBuffer();

        // Perform the serializing.
        neptuneCSVWriteVerticesToBuffer(nodesBuffer, graph);
        neptuneCSVWriteEdgesToBuffer(edgesBuffer, graph);

        // Write to output streams
        try {
            outNodes.write(nodesBuffer.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SerializerException("Serializing to NeptuneCSV failed, reason: Failed to write nodes to stream.", e);
        }
        try {
            outEdges.write(edgesBuffer.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SerializerException("Serializing to NeptuneCSV failed, reason: Failed to write edges to stream.", e);
        }
    }

    // Write out the node headers to the CSV.
    private static void printNodeHeadersToCSV(CSVPrinter p, Collection<NeptuneCSVColumn> vColumns) throws IOException {
        ArrayList<String> headers = new ArrayList<>();
        headers.add("~id");
        headers.add("~label");
        for (NeptuneCSVColumn col : vColumns) {
            // Spec says colons in headers should be escaped
            String escapedName = col.name.replace(":","\\:");
            headers.add(escapedName + ":" + col.dataType.getType() + col.cardinality.getCardinality());
        }
        p.printRecord(headers);
    }

    // Write out the edge headers to the CSV.
    private static void printEdgeHeadersToCSV(CSVPrinter p, Collection<NeptuneCSVColumn> eColumns) throws IOException {
        ArrayList<String> headers = new ArrayList<>();
        headers.add("~id");
        headers.add("~from");
        headers.add("~to");
        headers.add("~label");
        for (NeptuneCSVColumn col : eColumns) {
            // Spec says colons in headers should be escaped
            String escapedName = col.name.replace(":","\\:");
            headers.add(escapedName + ":" + col.dataType.getType() + col.cardinality.getCardinality());
        }
        p.printRecord(headers);
    }

    // Converts a collection of Objects to an array string as per the NeptuneCSV spec.
    // The following rules are applied:
    // 1. if " , \n \r found in string then the string must be surrounded in ""
    // 2. if " found in string replace with "" and surround string in "" if needed
    // 3. if ; is found it must be escaped to \;
    // 4. Dates must be formatted as an ISO date, use yyyy-MM-ddTHH:mm:ssZ as it is most precise
    private static String collectionToNeptuneArrayString(Collection<?> coll, NeptuneCSVColumn column) {
        StringBuilder resultBuilder = new StringBuilder();
        for (Object entry : coll) {
            String toAppend;
            if (column.dataType == NeptuneCSVDataType.STRING && column.cardinality == NeptuneCSVCardinality.ARRAY) {
                // Replace semicolon (;) with escaped semicolon (\;)
                toAppend = entry.toString().replace(";","\\;");
            } else if (column.dataType == NeptuneCSVDataType.DATE && entry instanceof Date) {
                Date date = (Date) entry;
                // Format dates to yyyy-MM-dd'T'HH:mm:ss.SSSZ
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                toAppend = format.format(date);
            } else {
                toAppend = entry.toString();
            }
            resultBuilder.append(toAppend);
            resultBuilder.append(";");
        }
        // Delete the last ;
        if (resultBuilder.length() > 1) {
            resultBuilder.deleteCharAt(resultBuilder.length() - 1);
        }
        return resultBuilder.toString();
    }

    // Writes the edges of the given graph to the given buffer in NeptuneCSV format
    private static void neptuneCSVWriteEdgesToBuffer(StringBuffer buffer, LPGGraph graph) throws SerializerException {
        Collection<NeptuneCSVColumn> eColumns = NeptuneCSVColumnFactory.createNeptuneColumnsForElements(graph.edges);

        try (CSVPrinter p = new CSVPrinter(buffer, CSVFormat.RFC4180)) {
            // Print the headers
            printEdgeHeadersToCSV(p, eColumns);
            // Print the edges.
            for (LPGEdge e : graph.edges) {
                // A csv row
                List<Object> row = new ArrayList<>();
                // 1. Add the edge ID
                row.add(e.getId().toString());
                // 2. Add the vertex out and in ids
                row.add(e.outVertex.getId());
                row.add(e.inVertex.getId());
                // 3. Add the edge label
                row.add(e.label);

                // 4. Add the edge properties.
                for (NeptuneCSVColumn c : eColumns) {

                    // Try to get the property from the edge
                    if (e.propertyForName(c.name).isPresent()) {
                        LPGProperty prop = e.propertyForName(c.name).get();
                        String propString = collectionToNeptuneArrayString(prop.values, c);
                        row.add(propString);
                    } else {
                        // There is no property of that name on this edge then add empty string.
                        row.add("");
                    }
                }
                p.printRecord(row);
            }
            p.flush();
        } catch (Exception e) {
            throw new SerializerException("Serializing to NeptuneCSV failed, reason: Failed to write edges to buffer.", e);
        }
    }

    // Writes the vertices of the given graph to the given buffer in NeptuneCSV format
    // returns a mapping of vertices to their ids
    private static void neptuneCSVWriteVerticesToBuffer(StringBuffer buffer, LPGGraph graph) throws SerializerException {
        // Create column objects from the given elements
        Collection<NeptuneCSVColumn> vColumns = NeptuneCSVColumnFactory.createNeptuneColumnsForElements(graph.vertices);

        try (CSVPrinter p = new CSVPrinter(buffer, CSVFormat.RFC4180)) {
            // Print the headers
            printNodeHeadersToCSV(p, vColumns);
            // Print the vertices.
            for (LPGVertex v : graph.vertices) {
                // A csv row
                List<Object> row = new ArrayList<>();
                // 1. Add the vertex ID
                row.add(v.getId().toString());
                // 2. Add the vertex labels, create a neptune column with the properties of the label column
                NeptuneCSVColumn labelColumn = new NeptuneCSVColumn();
                labelColumn.dataType = NeptuneCSVDataType.STRING;
                labelColumn.cardinality = NeptuneCSVCardinality.ARRAY;
                String labelString = collectionToNeptuneArrayString(v.getLabels(), labelColumn);
                row.add(labelString);

                // 3. Add the vertex properties.
                for (NeptuneCSVColumn c : vColumns) {
                    // Try to get the property from the vertex
                    if (v.propertyForName(c.name).isPresent()) {
                        LPGProperty prop = v.propertyForName(c.name).get();
                        String propString = collectionToNeptuneArrayString(prop.values, c);
                        row.add(propString);
                    } else {
                        // There is no property of that name on this vertex, add empty string.
                        row.add("");
                    }
                }
                p.printRecord(row);
            }
            p.flush();
        } catch (Exception e) {
            throw new SerializerException("Serializing to NeptuneCSV failed, reason: Failed to write nodes to buffer.", e);
        }
    }
}
