package software.amazon.neptune.onegraph.playground.server.tests.unit.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.api.request.DataFormat;
import software.amazon.neptune.onegraph.playground.server.service.ExportService;
import software.amazon.neptune.onegraph.playground.server.service.ViewService;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.mockdata.service.GeneralServiceTestData;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ViewServiceTest {

    public static State state = new State();

    @BeforeAll
    public static void addData() {
        GeneralServiceTestData.addToDataSet(state.getOgDataset());
        state.reloadDerivativeData();
    }

    @Test
    public void viewOGFormal() {
        ViewService service = new ViewService(state, new ExportService(state));
        String result = service.viewOGDatasetInDataFormat(DataFormat.FORMAL);
        assertTrue(result.contains("---- Simple Nodes ----"));
        assertTrue(result.contains("---- Graphs ----"));
        assertTrue(result.contains("---- Relationship Statements ----"));
        assertTrue(result.contains("---- Membership Statements ----"));
        assertTrue(result.contains("---- Property Statements ----"));
        assertTrue(result.contains("IRI: http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
        assertTrue(result.contains("IRI: http://example.com/knows"));
        assertTrue(result.contains("http://example.com/name"));
        assertTrue(result.contains("http://example.com/birthDate"));
        assertTrue(result.contains("IRI: http://example.com/age]"));
        assertTrue(result.contains("http://aws.amazon.com/neptune/vocab/v01/DefaultNamedGraph"));
        assertTrue(result.contains("Blank node: _:A"));
        assertTrue(result.contains("Blank node: _:B"));
        assertTrue(result.length() > 500);
    }

    @Test
    public void viewRDF() {
        ViewService service = new ViewService(state, new ExportService(state));
        String result = service.viewOGDatasetInDataFormat(DataFormat.NTRIPLES);
        assertTrue(result.contains("_:A <http://example.com/name> \"Alice\" ."));
        assertTrue(result.contains("_:A <http://example.com/name> \"Alice\""));
        assertTrue(result.contains("_:B <http://example.com/name> \"Bob\" ."));
        assertTrue(result.contains("_:A <http://example.com/knows> _:B ."));
        assertTrue(result.contains("_:A <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.com/Person> ."));
        assertTrue(result.length() > 500);
    }

    @Test
    public void viewNeptuneCSV() {
        ViewService service = new ViewService(state, new ExportService(state));
        String result = service.viewOGDatasetInDataFormat(DataFormat.NEPTUNECSV);

        assertTrue(result.contains("---- NODES ----"));
        assertTrue(result.contains("---- EDGES ----"));
        assertTrue(result.contains("http://example.com/knows"));
        assertTrue(result.contains("http\\://example.com/address:String"));
        assertTrue(result.contains("http\\://example.com/birthDate:Date"));
        assertTrue(result.contains("http\\://example.com/age:Int"));
        assertTrue(result.contains("http\\://example.com/name:String"));
        assertTrue(result.contains("http://example.com/Person"));
        assertTrue(result.contains("1985-06-06"));
        assertTrue(result.contains("1.72"));
        assertTrue(result.contains("Alice"));
        assertTrue(result.contains("Bob"));
        assertTrue(result.contains("~id,~from,~to,~label"));
        assertTrue(result.contains("http://example.com/knows"));
        assertTrue(result.length() > 150);
    }

    @Test
    public void viewGraphML() {
        ViewService service = new ViewService(state, new ExportService(state));
        String result = service.viewOGDatasetInDataFormat(DataFormat.GRAPHML);
        assertTrue(result.contains("<key id=\"http://example.com/address\" for=\"node\" attr.name=\"http://example.com/address\" attr.type=\"string\"></key>"));
        assertTrue(result.contains("<key id=\"http://example.com/age\" for=\"node\" attr.name=\"http://example.com/age\" attr.type=\"int\"></key>"));
        assertTrue(result.contains("<key id=\"http://example.com/birthDate\" for=\"node\" attr.name=\"http://example.com/birthDate\" attr.type=\"string\"></key>"));
        assertTrue(result.contains("<key id=\"http://example.com/cashBalance\" for=\"node\" attr.name=\"http://example.com/cashBalance\" attr.type=\"long\"></key>"));
        assertTrue(result.contains("<key id=\"labelE\" for=\"edge\" attr.name=\"labelE\" attr.type=\"string\"></key>"));
        assertTrue(result.contains("<data key=\"http://example.com/address\">80</data>"));
        assertTrue(result.contains("<data key=\"http://example.com/age\">44</data>"));
        assertTrue(result.contains("<key id=\"http://example.com/age\" for=\"node\" attr.name=\"http://example.com/age\" attr.type=\"int\"></key>"));
        assertTrue(result.contains("<data key=\"http://example.com/cashBalance\">1234</data>"));
        assertTrue(result.length() > 300);
    }

    @Test
    public void viewGraphSON() {
        ViewService service = new ViewService(state, new ExportService(state));
        String result = service.viewOGDatasetInDataFormat(DataFormat.GRAPHSON);
        assertTrue(result.contains("\"label\":\"http://example.com/Person\""));
        assertTrue(result.contains("\"label\":\"http://example.com/Person\""));
        assertTrue(result.contains("\"@type\":\"g:Int64\",\"@value\":1234"));
        assertTrue(result.contains("\"@type\":\"g:Date\",\"@value\":486910861000"));
        assertTrue(result.contains("\"@type\":\"gx:Byte\",\"@value\":80"));
        assertTrue(result.contains("\"@type\":\"g:Int32\",\"@value\":44"));
        assertTrue(result.contains("\"http://example.com/age\""));
        assertTrue(result.contains("\"http://example.com/name\""));
        assertTrue(result.contains("\"http://example.com/address\""));
        assertTrue(result.contains("\"http://example.com/height\""));

        assertTrue(result.length() > 250);
    }

    @Test
    public void viewOG() {
        ViewService service = new ViewService(state, new ExportService(state));
        String result = service.viewOGDatasetInDataFormat(DataFormat.OG);
        assertTrue(result.contains("_:A <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.com/Person>"));
        assertTrue(result.contains("_:B <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.com/Person>"));
        assertTrue(result.contains("_:A <http://example.com/knows> _:B"));
        assertTrue(result.contains("_:A <http://example.com/name> \"Alice\""));
        assertTrue(result.contains("_:A <http://example.com/age> \"44\"^^<http://www.w3.org/2001/XMLSchema#int>"));
        assertTrue(result.contains("_:A <http://example.com/address> \"80\"^^<http://www.w3.org/2001/XMLSchema#byte>"));
        assertTrue(result.contains("_:B <http://example.com/address> \"16\"^^<http://www.w3.org/2001/XMLSchema#short>"));
        assertTrue(result.contains("_:A <http://example.com/knows> _:B"));
        assertTrue(result.length() > 250);
    }

    @Test
    public void viewForceGraphMLError() {
        State state = new State();

        GeneralServiceTestData.addGraphMLIncompatibleData(state.getOgDataset());
        state.reloadDerivativeData();

        ViewService service = new ViewService(state, new ExportService(state));
        ViewService.ViewException exception = assertThrows(ViewService.ViewException.class,
                () -> service.viewOGDatasetInDataFormat(DataFormat.GRAPHML));

        String expectedMessage = "An exception occurred during view";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
