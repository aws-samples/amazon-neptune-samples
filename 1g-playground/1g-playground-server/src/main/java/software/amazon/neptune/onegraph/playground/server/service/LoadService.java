package software.amazon.neptune.onegraph.playground.server.service;

import software.amazon.neptune.onegraph.playground.server.mapping.ConverterException;
import software.amazon.neptune.onegraph.playground.server.mapping.TinkerPopConverter;
import software.amazon.neptune.onegraph.playground.server.io.parsing.LPGParser;
import software.amazon.neptune.onegraph.playground.server.io.parsing.ogparser.OGParser;
import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;
import software.amazon.neptune.onegraph.playground.server.io.parsing.RDFParser;
import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMapper;
import software.amazon.neptune.onegraph.playground.server.mapping.NeptuneCSVConverter;
import software.amazon.neptune.onegraph.playground.server.mapping.RDFMapper;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.state.State;
import lombok.NonNull;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.rdf4j.model.Model;

import java.io.InputStream;

/**
 * Service that handles the load command.
 */
public class LoadService extends Service {

    /**
     * All problems that occur during operations performed by view are thrown as
     * {@link LoadException} containing a message to help the user fix the issue.
     */
    public static class LoadException extends RuntimeException {

        /**
         * Creates a new load exception with the given message and cause.
         * @param message The exception message seen by the user.
         * @param cause The exception that caused this exception.
         */
        public LoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Initialize the load service with the given state.
     * @param state The state.
     */
    public LoadService(State state) {
        super(state);
    }

    /**
     * Loads the data in the given path(s) into the {@link State}.
     * @param dataFormat The data type the data is in.
     * @param stream1 The input stream to load from.
     * @param stream2 The second input stream to load from, (only supply a value for this if {@code dataFormat} is {@code NEPTUNECSV})
     * @param statusBuilder Appends info/warnings that are encountered during load to this builder.
     * @throws LoadException If an exception occurred during parsing or reading from file.
     * @throws IllegalArgumentException If path2 is null but dataFormat is {@code NEPTUNECSV}, and if path2 is not null but dataFormat,
     * is not {@code NEPTUNECSV}
     */
    public void loadDataAndUpdateState(@NonNull DataFormat dataFormat,
                                       @NonNull InputStream stream1,
                                       InputStream stream2,
                                       StringBuilder statusBuilder) throws LoadException, IllegalArgumentException {
        checkParameters(dataFormat, stream2);
        try {
            switch (dataFormat) {
                case GRAPHML:
                    handleGraphML(stream1, statusBuilder);
                    break;
                case GRAPHSON:
                    handleGraphSON(stream1, statusBuilder);
                    break;
                case NEPTUNECSV:
                    handleNeptuneCSV(stream1, stream2, statusBuilder);
                    break;
                case OG:
                    handleOG(stream1, statusBuilder);
                    break;
                default:
                    handleRDF(dataFormat, stream1, statusBuilder);
                    break;
            }
        } catch (Exception e) {
            throw new LoadException("An exception occurred during load", e);
        }
    }

    /**
     * Loads the data in the given path(s) into the {@link State}.
     * @param dataFormat The data type the data is in.
     * @param stream1 The input stream to load from.
     * @param stream2 The second input stream to load from, (only supply a value for this if {@code dataFormat} is {@code NEPTUNECSV})
     * @throws LoadException If an exception occurred during parsing or reading from file.
     * @throws IllegalArgumentException If path2 is null but dataFormat is {@code NEPTUNECSV}, and if path2 is not null but dataFormat,
     * is not {@code NEPTUNECSV}
     */
    public void loadDataAndUpdateState(@NonNull DataFormat dataFormat,
                                       @NonNull InputStream stream1,
                                       InputStream stream2) throws LoadException, IllegalArgumentException {
       this.loadDataAndUpdateState(dataFormat, stream1, stream2, new StringBuilder());
    }

    // Loads RDF data into the state.
    private void handleRDF(@NonNull DataFormat dataFormat, @NonNull InputStream stream, @NonNull StringBuilder statusBuilder) throws ParserException {
        Model rdf = RDFParser.parseRDF(stream, dataFormat);
        RDFMapper mapper = new RDFMapper(state.getOgDataset());
        mapper.addRDFModelToOG(rdf);
        state.reloadDerivativeData(statusBuilder);
    }

    // Loads OG data into the state.
    private void handleOG(@NonNull InputStream stream, @NonNull StringBuilder statusBuilder) throws ParserException {
        OGParser parser = new OGParser();
        parser.parseOGTriples(stream, state.getOgDataset());
        state.reloadDerivativeData(statusBuilder);
    }

    // Loads neptuneCSV data into the state.
    private void handleNeptuneCSV(@NonNull InputStream stream1,
                                  @NonNull InputStream stream2,
                                  @NonNull StringBuilder statusBuilder) throws ParserException, ConverterException {
        Model nodes = LPGParser.parseNeptuneCSV(stream1);
        Model edges = LPGParser.parseNeptuneCSV(stream2);

        LPGGraph graph = NeptuneCSVConverter.convertNeptuneCSVtoLPG(nodes, edges);
        addLPGToOGDatasetInState(graph, statusBuilder);
    }

    // Loads graphSON data into the state.
    private void handleGraphSON(@NonNull InputStream stream, @NonNull StringBuilder statusBuilder) throws Exception {
        try (Graph tinkerPopGraph = LPGParser.parseGraphSON(stream)) {
            LPGGraph graph = this.convertGraphToLPG(tinkerPopGraph, statusBuilder);
            addLPGToOGDatasetInState(graph, statusBuilder);
        }
    }

    // Loads graphML data into the state.
    private void handleGraphML(@NonNull InputStream stream, @NonNull StringBuilder statusBuilder) throws Exception {
        try (Graph tinkerPopGraph = LPGParser.parseGraphML(stream)) {
            LPGGraph graph = this.convertGraphToLPG(tinkerPopGraph, statusBuilder);
            addLPGToOGDatasetInState(graph, statusBuilder);
        }
    }

    private LPGGraph convertGraphToLPG(@NonNull Graph g, @NonNull StringBuilder statusBuilder) {
        TinkerPopConverter converter = new TinkerPopConverter(new TinkerPopConverter.TinkerPopConverterDelegate() {
            @Override
            public void unsupportedValueFound(String infoString) {
                statusBuilder.append(infoString).append("\n");
            }

            @Override
            public void metaPropertyFound(String infoString) {
                statusBuilder.append(infoString).append("\n");
            }

            @Override
            public void multiLabeledVertexFound(String infoString) {
                statusBuilder.append(infoString).append("\n");
            }
        });
        return converter.convertTinkerGraphToLPG(g);
    }
    // Loads the given LPGGraph into the state.
    private void addLPGToOGDatasetInState(@NonNull LPGGraph graph, @NonNull StringBuilder statusBuilder) {
        LPGMapper mapper = new LPGMapper(state.getOgDataset(), state.getLpgMappingConfiguration());
        mapper.addLPGToOGDataset(graph);
        state.reloadDerivativeData(statusBuilder);
    }

    // Check validity of parameters in loadDataAndUpdateState
    private void checkParameters(@NonNull DataFormat dataFormat,
                                 InputStream stream2) throws IllegalArgumentException {
        if (dataFormat == DataFormat.NEPTUNECSV && stream2 == null) {
            throw new IllegalArgumentException("NeptuneCSV specified but no second input stream given");
        }
    }
}
