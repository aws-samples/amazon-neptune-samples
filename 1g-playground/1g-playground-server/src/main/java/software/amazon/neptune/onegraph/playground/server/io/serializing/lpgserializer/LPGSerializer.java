package software.amazon.neptune.onegraph.playground.server.io.serializing.lpgserializer;

import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONXModuleV3d0;
import org.apache.tinkerpop.gremlin.structure.io.graphson.TypeInfo;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.io.serializing.SerializerException;
import lombok.NonNull;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.graphml.GraphMLWriter;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONWriter;

import java.io.OutputStream;

/**
 * Serializes {@link LPGGraph}s to the following LPG Formats:
 * GraphSON, GraphML, NeptuneCSV
 */
public class LPGSerializer {

    /**
     * Serializes the given {@code graph} to the graphML format.
     * @param out The output stream to write to.
     * @param graph The TinkerPop graph to serialize.
     * @throws SerializerException thrown when an exception occurs during serializing.
     */
    public static void serializeToGraphML(@NonNull OutputStream out,
                                          @NonNull  Graph graph) throws SerializerException {
        try {
            GraphMLWriter writer = GraphMLWriter.build().normalize(true).create();

            writer.writeGraph(out, graph);
        } catch (Exception e) {
            throw new SerializerException("Serializing to graphML failed", e);
        }
    }

    /**
     * Serializes the given {@code graph} to the graphSON format.
     * @param out The output stream to write to.
     * @param graph The graph to serialize.
     * @throws SerializerException thrown when an exception occurs during serializing.
     */
    public static void serializeToGraphSON(@NonNull OutputStream out,
                                           @NonNull Graph graph) throws SerializerException {
        try {
            // Use version 3.0 of GraphSON, partial types argument is needed because to avoid type errors.
            GraphSONMapper mapper = GraphSONMapper.build().
                    typeInfo(TypeInfo.PARTIAL_TYPES).
                    addCustomModule(GraphSONXModuleV3d0.build().create(false)).
                    version(GraphSONVersion.V3_0).create();
            GraphSONWriter writer = GraphSONWriter.build().mapper(mapper).create();
            writer.writeGraph(out, graph);
        } catch (Exception e) {
            throw new SerializerException("Serializing to GraphSON failed", e);
        }
    }

    /**
     * Serializes the given {@code graph} to the NeptuneCSV format.
     * @param outNodes The output stream to write the nodes to.
     * @param outEdges The output stream to write the edges to.
     * @param graph The graph to serialize.
     * @throws SerializerException thrown when an exception occurs during serializing.
     */
    public static void serializeToNeptuneCSV(@NonNull OutputStream outNodes,
                                             @NonNull  OutputStream outEdges,
                                             @NonNull  LPGGraph graph) throws SerializerException {

        NeptuneCSVSerializer.serializeToNeptuneCSV(outNodes, outEdges, graph);
    }
}
