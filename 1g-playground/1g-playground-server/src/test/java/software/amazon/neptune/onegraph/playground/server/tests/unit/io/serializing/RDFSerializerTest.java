package software.amazon.neptune.onegraph.playground.server.tests.unit.io.serializing;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.io.serializing.RDFSerializer;
import software.amazon.neptune.onegraph.playground.server.io.serializing.SerializerException;
import software.amazon.neptune.onegraph.playground.server.mockdata.io.IORDFTestData;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RDFSerializerTest {

    @Test
    void serializeTrig() {
        Model m = IORDFTestData.modelAliceKnowsBobInDefaultGraph();
        OutputStream stream = new ByteArrayOutputStream();
        RDFSerializer.serializeToRDF(stream, m, RDFFormat.TRIG);

        assertTrue(stream.toString().contains("Alice"));
    }

    @Test
    void serializeEmptyTurtle() {
        Model m = new LinkedHashModel();
        OutputStream stream = new ByteArrayOutputStream();
        RDFSerializer.serializeToRDF(stream, m, RDFFormat.TURTLE);

        assertEquals(0,stream.toString().length());
    }

    @Test
    void unsupportedFormat() {
        Model m = IORDFTestData.modelAliceKnowsBobInDefaultGraph();
        OutputStream stream = new ByteArrayOutputStream();

        SerializerException exception = assertThrows(SerializerException.class, () -> RDFSerializer.serializeToRDF(stream, m, RDFFormat.TRIX));
        String expectedMessage = "failed";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
