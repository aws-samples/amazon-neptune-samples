package software.amazon.neptune.onegraph.playground.server.service;

import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.io.serializing.SerializerException;
import software.amazon.neptune.onegraph.playground.server.io.serializing.lpgserializer.LPGSerializer;
import software.amazon.neptune.onegraph.playground.server.io.serializing.OGSerializer;
import software.amazon.neptune.onegraph.playground.server.io.serializing.RDFSerializer;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGMembershipStatement;
import lombok.NonNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Service that handles the export command.
 */
public class ExportService extends Service {

    // Delimiters used when exporting to FORMAL
    private final String RELATIONSHIP_DELIMITER = "---- Relationship Statements ----";
    private final String MEMBERSHIP_DELIMITER = "---- Membership Statements ----";
    private final String PROPERTY_DELIMITER = "---- Property Statements ----";
    private final String SIMPLE_NODE_DELIMITER = "---- Simple Nodes ----";
    private final String GRAPH_DELIMITER = "---- Graphs ----";

    /**
     * All problems that occur during operations performed by export are thrown as
     * {@link ExportException} containing a message to help the user fix the issue.
     */
    public static class ExportException extends RuntimeException {

        /**
         * Creates a new export exception with the given message and cause.
         * @param message The exception message seen by the user.
         * @param cause The exception that caused this exception.
         */
        public ExportException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Initialize the export service with the given state.
     * @param state The state.
     */
    public ExportService(State state) {
        super(state);
    }

    /**
     * Exports the current OG data set in the {@link State} in the given {@code dataFormat} format to the given path(s).
     * @param dataFormat The data type format to export in.
     * @param path1 The first file name.
     * @param path2 The second file name, (only supply a value for this if {@code dataFormat} is {@code NEPTUNECSV})
     * @throws ExportException If an exception occurred during serializing or writing to file.
     * @throws IllegalArgumentException If {@code fileName2} is null but dataFormat is {@code NEPTUNECSV}, and if {@code fileName2} is not null but dataFormat,
     * is not {@code NEPTUNECSV}
     */
    public void exportData(@NonNull DataFormat dataFormat,
                                       @NonNull Path path1,
                           Path path2) throws ExportException, IllegalArgumentException {
        checkParameters(dataFormat, path2);

        try (OutputStream out1 = Files.newOutputStream(path1, WRITE, CREATE, TRUNCATE_EXISTING);
             BufferedOutputStream bufferedOut = new BufferedOutputStream(out1) ) {

            if (dataFormat != DataFormat.NEPTUNECSV) {
                exportData(dataFormat, bufferedOut, null);
            } else {
                try (OutputStream out2 = Files.newOutputStream(path2, WRITE, CREATE, TRUNCATE_EXISTING);
                     BufferedOutputStream bufferedOut2 = new BufferedOutputStream(out2) ) {
                    exportData(dataFormat, bufferedOut, bufferedOut2);
                }
            }
        } catch (Exception e) {
            throw new ExportException("An exception occurred during export", e);
        }
    }
    public void exportData(@NonNull DataFormat dataFormat,
                           @NonNull OutputStream out1,
                           OutputStream out2) throws Exception {
        switch (dataFormat) {
            case GRAPHML:
                handleGraphML(out1);
                break;
            case GRAPHSON:
                handleGraphSON(out1);
                break;
            case NEPTUNECSV:
                handleNeptuneCSV(out1, out2);
                break;
            case OG:
                handleOG(out1);
                break;
            case FORMAL:
                handleFormal(out1);
                break;
            default:
                handleRDF(dataFormat, out1);
                break;
        }
    }

    /**
     * View the current OG data set in the {@link State} in formal notation.
     * @return A string containing the data set in formal notation.
     */
    private String handleFormal(@NonNull OutputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(RELATIONSHIP_DELIMITER).append("\n");
        for (OGRelationshipStatement statement : this.state.getOgDataset().getRelationshipStatements()) {
            builder.append(statement).append("\n");
        }
        builder.append("\n").append(PROPERTY_DELIMITER).append("\n");
        for (OGPropertyStatement statement : this.state.getOgDataset().getPropertyStatements()) {
            builder.append(statement).append("\n");
        }
        builder.append("\n").append(MEMBERSHIP_DELIMITER).append("\n");
        for (OGMembershipStatement statement : this.state.getOgDataset().getMembershipStatements()) {
            builder.append(statement).append("\n");
        }
        builder.append("\n").append(SIMPLE_NODE_DELIMITER).append("\n");
        for (OGSimpleNode<?> sn : this.state.getOgDataset().getSimpleNodes()) {
            builder.append(sn).append("\n");
        }
        builder.append("\n").append(GRAPH_DELIMITER).append("\n");
        for (OGGraph<?> g : this.state.getOgDataset().getGraphs()) {
            builder.append(g).append("\n");
        }
        stream.write(builder.toString().getBytes(StandardCharsets.UTF_8));
        return builder.toString();
    }
    
    // Exports data in the State to given Data type to the given path.
    private void handleRDF(@NonNull DataFormat type, @NonNull OutputStream stream) throws SerializerException {
        RDFSerializer.serializeToRDF(stream, state.getRdfModel(), type.toRDFFormat());
    }

    // Exports data in the State to OG to the given path.
    private void handleOG(@NonNull OutputStream stream) {
        OGSerializer s = new OGSerializer();
        s.serializeToOG(stream, this.state.getOgDataset());
    }

    // Exports data in the State to neptuneCSV to the given path.
    private void handleNeptuneCSV(@NonNull OutputStream stream1,
                                  @NonNull OutputStream stream2) throws SerializerException {
        LPGSerializer.serializeToNeptuneCSV(stream1, stream2, this.state.getLpgGraph());
    }

    // Exports data in the State to graphSON to the given path.
    private void handleGraphSON(@NonNull OutputStream stream) throws SerializerException {
        LPGSerializer.serializeToGraphSON(stream, this.state.getTinkerPopGraph());
    }

    // Exports data in the State to graphML to the given path.
    private void handleGraphML(@NonNull OutputStream stream) throws SerializerException {
        LPGSerializer.serializeToGraphML(stream, this.state.getTinkerPopGraph());
    }

    // Check validity of parameters in exportData
    private void checkParameters(@NonNull DataFormat dataFormat,
                                 Path p) throws IllegalArgumentException {
        if (dataFormat == DataFormat.NEPTUNECSV && p == null) {
            throw new IllegalArgumentException("NeptuneCSV specified but no second path given");
        }
    }
}
