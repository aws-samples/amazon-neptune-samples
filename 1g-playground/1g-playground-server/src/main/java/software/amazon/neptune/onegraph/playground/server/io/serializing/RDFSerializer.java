package software.amazon.neptune.onegraph.playground.server.io.serializing;

import lombok.NonNull;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import java.io.OutputStream;

import static org.eclipse.rdf4j.rio.helpers.BasicParserSettings.PRESERVE_BNODE_IDS;

/**
 * Serializes RDF {@link Model}s to the following RDF Formats:
 * N3, N-Quads, Turtle, Trig and RDF/XML.
 */
public class RDFSerializer {

    /**
     * Serializes the given {@code model} to the given {@code format}.
     * @param out The output stream to write to.
     * @param model The RDF model to serialize.
     * @throws SerializerException thrown when an exception occurs during serializing.
     */
    public static void serializeToRDF(@NonNull OutputStream out,
                                      @NonNull Model model,
                                      @NonNull RDFFormat format) throws SerializerException {
        try {
            RDFWriter writer = Rio.createWriter(format, out).set(PRESERVE_BNODE_IDS, true);
            writer.startRDF();
            for (Statement st : model) {
                writer.handleStatement(st);
            }
            writer.endRDF();
        } catch (Exception e) {
            throw new SerializerException("Serializing to RDF failed", e);
        }
    }
}
