package software.amazon.neptune.onegraph.playground.server.tests.unit.io.serializing;

import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;
import software.amazon.neptune.onegraph.playground.server.io.serializing.OGSerializer;
import software.amazon.neptune.onegraph.playground.server.mockdata.io.IOOGTestData;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OGSerializerTest {

    @Test
    public void dataWithUnassertedElements() {
        OGDataset ds = IOOGTestData.dataWithUnassertedElements();
        OGSerializer s = new OGSerializer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<OGTripleStatement, Integer> result = s.serializeToOG(outputStream, ds);
        assertEquals(6, result.values().size());

        String output = outputStream.toString();
        assertTrue(output.contains(" U "));
    }


    @Test
    public void dataWithManyNestings() {
        OGDataset ds = IOOGTestData.dataWithManyNestings();
        OGSerializer s = new OGSerializer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<OGTripleStatement, Integer> result = s.serializeToOG(outputStream, ds);
        assertEquals(6, result.values().size());

        String output = outputStream.toString();
        assertTrue(output.contains("<http://example.com/Person> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.com/Person>"));
        assertTrue(output.contains("<http://example.com/knows> __:SID"));
    }

    @Test
    public void dataInMultipleGraphs() {
        OGDataset ds = IOOGTestData.dataInMultipleGraphs();
        OGSerializer s = new OGSerializer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<OGTripleStatement, Integer> result = s.serializeToOG(outputStream, ds);
        assertEquals(6, result.values().size());

        String output = outputStream.toString();
        assertTrue(output.contains("<http://example.com/graph1>"));
        assertTrue(output.contains("<http://example.com/graph2>"));
        assertTrue(output.contains("_:graph3"));
    }

    @Test
    public void dataWithComplexLiterals() {
        OGDataset ds = IOOGTestData.dataWithComplexLiterals();
        OGSerializer s = new OGSerializer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<OGTripleStatement, Integer> result = s.serializeToOG(outputStream, ds);
        assertEquals(8, result.values().size());

        String output = outputStream.toString();
        assertTrue(output.contains("\\n\\n\\n\\n \\r \\r"));
        assertTrue(output.contains("<http://www.w3.org/2001/XMLSchema#byte>"));
        assertTrue(output.contains("\"1234\"^^<http://www.w3.org/2001/XMLSchema#long>"));
        assertTrue(output.contains("<http://www.w3.org/2001/XMLSchema#dateTime>"));
        assertTrue(output.contains("<http://www.w3.org/2001/XMLSchema#int>"));
        assertTrue(output.contains("<http://www.w3.org/2001/XMLSchema#short>"));
    }

    @Test
    public void lpgData() {
        OGDataset ds = IOOGTestData.lpgData();
        OGSerializer s = new OGSerializer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<OGTripleStatement, Integer> result = s.serializeToOG(outputStream, ds);
        assertEquals(5, result.values().size());

        String output = outputStream.toString();

        assertTrue(output.contains("<http://www.w3.org/2001/XMLSchema#dateTime>"));
        assertTrue(output.contains("<http://aws.amazon.com/neptune/ogplayground/data/edge/knows>"));
        assertTrue(output.contains("<http://aws.amazon.com/neptune/ogplayground/data/propertyName/age>"));
        assertTrue(output.contains("\"Person\""));
        assertTrue(output.contains("<http://aws.amazon.com/neptune/ogplayground/data/id/1>"));
        assertTrue(output.contains("<http://aws.amazon.com/neptune/ogplayground/data/id/theID>"));
        assertTrue(output.contains("<http://aws.amazon.com/neptune/ogplayground/data/id/3>"));
    }

    @Test
    public void empty() {
        OGDataset ds = new OGDataset();
        OGSerializer s = new OGSerializer();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        s.serializeToOG(outputStream, ds);
        assertEquals(0, outputStream.toString().length());
    }
}
