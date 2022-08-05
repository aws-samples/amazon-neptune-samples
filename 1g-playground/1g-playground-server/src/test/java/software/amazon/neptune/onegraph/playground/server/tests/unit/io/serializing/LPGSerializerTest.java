package software.amazon.neptune.onegraph.playground.server.tests.unit.io.serializing;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.io.serializing.lpgserializer.LPGSerializer;
import software.amazon.neptune.onegraph.playground.server.io.serializing.SerializerException;
import software.amazon.neptune.onegraph.playground.server.mockdata.io.IOTinkerPopTestData;
import software.amazon.neptune.onegraph.playground.server.mockdata.io.IOLPGTestData;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class LPGSerializerTest {

    @Test
    void serializeToGraphML() {
        Graph aliceBob = IOTinkerPopTestData.tinkerPopGraphAliceKnowsBobWithEmptyNode();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LPGSerializer.serializeToGraphML(out, aliceBob);
        assertEquals("<?xml version=\"1.0\" ?>\n" +
                "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.1/graphml.xsd\">\n" +
                "    <key id=\"labelV\" for=\"node\" attr.name=\"labelV\" attr.type=\"string\"></key>\n" +
                "    <key id=\"name\" for=\"node\" attr.name=\"name\" attr.type=\"string\"></key>\n" +
                "    <key id=\"labelE\" for=\"edge\" attr.name=\"labelE\" attr.type=\"string\"></key>\n" +
                "    <key id=\"since\" for=\"edge\" attr.name=\"since\" attr.type=\"int\"></key>\n" +
                "    <graph id=\"G\" edgedefault=\"directed\">\n" +
                "        <node id=\"0\">\n" +
                "            <data key=\"labelV\">Person</data>\n" +
                "            <data key=\"name\">Alice</data>\n" +
                "        </node>\n" +
                "        <node id=\"1\">\n" +
                "            <data key=\"labelV\">Person</data>\n" +
                "            <data key=\"name\">Bob</data>\n" +
                "        </node>\n" +
                "        <node id=\"2\">\n" +
                "            <data key=\"labelV\">vertex</data>\n" +
                "        </node>\n" +
                "        <edge id=\"5\" source=\"0\" target=\"1\">\n" +
                "            <data key=\"labelE\">knows</data>\n" +
                "            <data key=\"since\">1955</data>\n" +
                "        </edge>\n" +
                "    </graph>\n" +
                "</graphml>", out.toString());
    }

    @Test
    void serializeToGraphSON() {
        Graph aliceBob = IOTinkerPopTestData.tinkerPopGraphAliceKnowsBobWithEmptyNode();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LPGSerializer.serializeToGraphSON(out, aliceBob);
        assertTrue(out.toString().contains("{\"id\":{\"@type\":\"g:Int64\",\"@value\":0},\"label\":\"Person\",\"outE\":{\"knows\":[{\"id\":{\"@type\":\"g:Int64\",\"@value\":5},\"inV\":{\"@type\":\"g:Int64\",\"@value\":1},\"properties\":{\"since\":{\"@type\":\"g:Int32\",\"@value\":1955}}}]},\"properties\":{\"name\":[{\"id\":{\"@type\":\"g:Int64\",\"@value\":3},\"value\":\"Alice\"}]}}"));
        assertTrue(out.toString().contains("{\"id\":{\"@type\":\"g:Int64\",\"@value\":1},\"label\":\"Person\",\"inE\":{\"knows\":[{\"id\":{\"@type\":\"g:Int64\",\"@value\":5},\"outV\":{\"@type\":\"g:Int64\",\"@value\":0},\"properties\":{\"since\":{\"@type\":\"g:Int32\",\"@value\":1955}}}]},\"properties\":{\"name\":[{\"id\":{\"@type\":\"g:Int64\",\"@value\":4},\"value\":\"Bob\"}]}}"));
        assertTrue(out.toString().contains("{\"id\":{\"@type\":\"g:Int64\",\"@value\":2},\"label\":\"vertex\"}"));
    }

    @Test
    void serializeToGraphMLConflictingKey() {

        Graph aliceBob = IOTinkerPopTestData.tinkerPopGraphConflictingKeyForGraphML();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SerializerException exception = assertThrows(SerializerException.class, () -> LPGSerializer.serializeToGraphML(out, aliceBob));
        String expectedMessage = "failed";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    void serializeAliceKnowsBobSince2005ToNeptuneCSV() {
        LPGGraph graph = IOLPGTestData.aliceKnowsBobSince2005();

        ByteArrayOutputStream outN = new ByteArrayOutputStream();
        ByteArrayOutputStream outE = new ByteArrayOutputStream();

        LPGSerializer.serializeToNeptuneCSV(outN, outE, graph);

        assertTrue(outN.toString().contains("~id,~label,name:String,age:Int"));
        assertTrue(outN.toString().contains(",Person,Bob,"));
        assertTrue(outN.toString().contains(",Person;Boss,Alice,44"));

        assertTrue(outE.toString().contains("~id,~from,~to,~label,since:String"));
        assertTrue(outE.toString().contains(",knows,2k15"));
    }

    @Test
    void serializeAliceKnowsBobTwiceWithEdgePropertiesToNeptuneCSV() {
        LPGGraph graph = IOLPGTestData.aliceKnowsBobTwiceWithEdgeProperties();

        ByteArrayOutputStream outN = new ByteArrayOutputStream();
        ByteArrayOutputStream outE = new ByteArrayOutputStream();

        LPGSerializer.serializeToNeptuneCSV(outN, outE, graph);

        assertTrue(outN.toString().contains(",Person,Bob,"));
        assertTrue(outN.toString().contains(",Person;Boss,Alice,44"));

        assertTrue(outE.toString().contains("~id,~from,~to,~label,statedBy:String,since:Int"));
        assertTrue(outE.toString().contains(",knows,NYTimes,2021"));
        assertTrue(outE.toString().contains(",knows,TheGuardian,2020"));
    }

    @Test
    void serializeMultiValuedPropertyToNeptuneCSV() {
        LPGGraph graph = IOLPGTestData.multiValuedProperty();

        ByteArrayOutputStream outN = new ByteArrayOutputStream();
        ByteArrayOutputStream outE = new ByteArrayOutputStream();

        LPGSerializer.serializeToNeptuneCSV(outN, outE, graph);

        assertTrue(outN.toString().contains("~id,~label,age:String[]"));
        assertTrue(outN.toString().contains("24.0"));
        assertTrue(outN.toString().contains("23"));
        assertTrue(outN.toString().contains("22"));
        assertTrue(outN.toString().contains("25.0"));
    }

    @Test
    void serializeUnlabeledVertexToNeptuneCSV() {
        LPGGraph graph = IOLPGTestData.unlabeledVertex();

        ByteArrayOutputStream outN = new ByteArrayOutputStream();
        ByteArrayOutputStream outE = new ByteArrayOutputStream();

        LPGSerializer.serializeToNeptuneCSV(outN, outE, graph);

        assertTrue(outN.toString().contains("~id,~label"));
        assertTrue(outN.toString().contains(",vertex"));

        assertTrue(outE.toString().contains("~id,~from,~to,~label"));
    }

    @Test
    void serializeArrayPropertyToNeptuneCSV() {
        LPGGraph graph = IOLPGTestData.arrayProperty();

        ByteArrayOutputStream outN = new ByteArrayOutputStream();
        ByteArrayOutputStream outE = new ByteArrayOutputStream();

        LPGSerializer.serializeToNeptuneCSV(outN, outE, graph);

        assertTrue(outN.toString().contains("~id,~label,age:Int[]"));
        assertTrue(outN.toString().contains("Person,21;22;23;24"));
    }

    @Test
    void serializeEmptyNeptuneCSV() {
        LPGGraph graph = new LPGGraph();

        ByteArrayOutputStream outN = new ByteArrayOutputStream();
        ByteArrayOutputStream outE = new ByteArrayOutputStream();

        LPGSerializer.serializeToNeptuneCSV(outN, outE, graph);

        assertEquals("~id,~label", outN.toString().trim());
        assertEquals("~id,~from,~to,~label", outE.toString().trim());
    }
    @Test
    void serializeVertexWithComplexLabelsToNeptuneCSV() {

        LPGGraph graph = IOLPGTestData.complexLabels();

        ByteArrayOutputStream outN = new ByteArrayOutputStream();
        ByteArrayOutputStream outE = new ByteArrayOutputStream();

        LPGSerializer.serializeToNeptuneCSV(outN, outE, graph);

        assertTrue(outN.toString().contains(",Person;Boss"));
        assertTrue(outN.toString().contains(",[]]Person"));
        assertTrue(outN.toString().contains(",Person;Boss;%afe'"));
        assertTrue(outN.toString().contains(",\"Person, Friend\""));

        assertTrue(outE.toString().contains(",\"knows;thinks \"\" wants\""));
        assertTrue(outE.toString().contains(",\"\"\"quoted\"\"\""));

    }

    @Test
    void serializeVertexWithComplexPropertiesToNeptuneCSV() {

        LPGGraph graph = IOLPGTestData.complexProperties();

        ByteArrayOutputStream outN = new ByteArrayOutputStream();
        ByteArrayOutputStream outE = new ByteArrayOutputStream();

        LPGSerializer.serializeToNeptuneCSV(outN, outE, graph);

        assertTrue(outN.toString().contains("~id,~label,aChar:String,age:Int[],height:String"));
        assertTrue(outN.toString().contains(",Person,a,,"));
        assertTrue(outN.toString().contains(",Person,a,25;26;27,a height"));
        assertTrue(outN.toString().contains(",Person,,25,0"));

        assertTrue(outE.toString().contains("~id,~from,~to,~label,date:Date,edgeProp1:Int,edgeProp2:Int"));
        assertTrue(outE.toString().contains(",edge1,,2005,"));
    }
}
