package software.amazon.neptune.onegraph.playground.server.io.parsing;

import org.apache.commons.io.IOUtils;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONXModuleV3d0;
import org.apache.tinkerpop.gremlin.structure.io.graphson.TypeInfo;
import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;
import lombok.NonNull;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLReader;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONReader;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import software.amazon.neptune.csv2rdf.NeptuneCsvInputParser;
import software.amazon.neptune.csv2rdf.NeptunePropertyGraphElement;
import software.amazon.neptune.csv2rdf.PropertyGraph2RdfMapper;
import software.amazon.neptune.csv2rdf.PropertyGraph2RdfMapping;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;


/**
 * Parses LPG files in any of the following LPG Formats:
 * GraphSON, graphML, NeptuneCSV
 */
public class LPGParser {

    /** Used in the NeptuneCSV parser in predicate position to make statements about node and edge labels */
    public static final IRI NEPTUNECSV_PREDICATE_LABEL_IRI = RDF.TYPE;

    /** This is the default context/graph for the all models produced */
    public static final String NEPTUNECSV_DEFAULT_CTX = URLConstants.DEFAULT_GRAPH;

    /** This is the label used when an edge has no label */
    public static final String NEPTUNECSV_DEFAULT_PREDICATE = URLConstants.EDGE + "edge";

    /** This is the label used when a vertex has no label, getting the local namespace of this will produce empty string */
    public static final String NEPTUNECSV_DEFAULT_LABEL = URLConstants.DEFAULT_NODE_LABEL;

    /**
     * Parses a NeptuneCSV vertex or edge file.
     * @param stream The input stream of the CSV file.
     * @throws ParserException if an error occurs during the process
     * @return An RDF Model created from the CSV.
     */
    public static Model parseNeptuneCSV(@NonNull InputStream stream) throws ParserException {
        Model model = new LinkedHashModel();

        PropertyGraph2RdfMapper mapper = new PropertyGraph2RdfMapper();
        PropertyGraph2RdfMapping mapping = new PropertyGraph2RdfMapping();
        mapping.setDefaultNamedGraph(NEPTUNECSV_DEFAULT_CTX);
        mapping.setDefaultType(NEPTUNECSV_DEFAULT_LABEL);
        mapping.setDefaultPredicate(NEPTUNECSV_DEFAULT_PREDICATE);
        mapper.setMapping(mapping);

        try {
            // The NeptuneCSVInputParser needs a file input, create a temp file from the input stream.
            File tempFile = File.createTempFile("tempFile", ".tmp");
            OutputStream outputStream = Files.newOutputStream(tempFile.toPath());
            IOUtils.copy(stream, outputStream);
            outputStream.close();
            try (NeptuneCsvInputParser inputParser = new NeptuneCsvInputParser(tempFile)) {
                while (inputParser.hasNext()) {
                    NeptunePropertyGraphElement e = inputParser.next();

                    if (e instanceof NeptunePropertyGraphElement.NeptunePropertyGraphEdge) {
                        model.addAll(mapper.mapToStatements((NeptunePropertyGraphElement.NeptunePropertyGraphEdge) e));
                    }
                    if (e instanceof NeptunePropertyGraphElement.NeptunePropertyGraphVertex) {
                        model.addAll(mapper.mapToStatements((NeptunePropertyGraphElement.NeptunePropertyGraphVertex) e));
                    }
                }
            }
        } catch (Exception e) {
            throw new ParserException("Parsing NeptuneCSV file failed", e);
        }

        return model;
    }

    /**
     * Parses a GraphSON file, and creates a TinkerPop graph.
     * @param stream The input stream of the GraphSON file.
     * @throws ParserException if an error occurs during the process
     * @return A TinkerPop Graph.
     */
    public static Graph parseGraphSON(@NonNull InputStream stream) throws ParserException {
        try (TinkerGraph graph = TinkerGraph.open()) {
            // Use version 3.0 of GraphSON, partial types argument is needed because we must WRITE Partial types as well in
            // LPGSerializer to avoid type errors.
            GraphSONMapper mapper = GraphSONMapper.build().
                    typeInfo(TypeInfo.PARTIAL_TYPES).
                    addCustomModule(GraphSONXModuleV3d0.build().create(false)).
                    version(GraphSONVersion.V3_0).create();

            GraphSONReader reader = GraphSONReader.build().mapper(mapper).create();
            reader.readGraph(new BufferedInputStream(stream), graph);
            return graph;
        } catch (Exception e) {
            throw new ParserException("Parsing GraphSON failed", e);
        }
    }

    /**
     * Parses a GraphML file, and creates a TinkerPop graph.
     * @param stream The input stream of the GraphML file.
     * @throws ParserException if an error occurs during the process
     * @return A TinkerPop Graph.
     */
    public static Graph parseGraphML(@NonNull InputStream stream) throws ParserException {
        try (TinkerGraph graph = TinkerGraph.open()) {
            GraphMLReader reader = GraphMLReader.build().create();
            reader.readGraph(new BufferedInputStream(stream), graph);
            return graph;
        } catch (Exception e) {
            throw new ParserException("Parsing GraphML file failed", e);
        }
    }
}
