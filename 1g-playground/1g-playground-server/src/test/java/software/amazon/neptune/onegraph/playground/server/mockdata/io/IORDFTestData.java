package software.amazon.neptune.onegraph.playground.server.mockdata.io;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Contains different test scenarios for RDF {@link Model}s
 */
public class IORDFTestData {

    public static final IRI ALICE = SimpleValueFactory.getInstance().createIRI("http://example.com/Alice");
    public static final IRI KNOWS = SimpleValueFactory.getInstance().createIRI("http://example.com/knows");
    public static final IRI PERSON = SimpleValueFactory.getInstance().createIRI("http://example.com/Person");
    public static final IRI TYPE = RDF.TYPE;
    public static final IRI BOB = SimpleValueFactory.getInstance().createIRI("http://example.com/Bob");
    public static final IRI AGE = SimpleValueFactory.getInstance().createIRI("http://example.com/age");
    public static final IRI NAME = SimpleValueFactory.getInstance().createIRI("http://example.com/name");
    public static final Value ALICE_NAME = SimpleValueFactory.getInstance().createLiteral("Alice");
    public static final Value BOB_NAME = SimpleValueFactory.getInstance().createLiteral("Bob");
    public static final Value ALICE_AGE = SimpleValueFactory.getInstance().createLiteral(45);
    public static final BNode B1 = SimpleValueFactory.getInstance().createBNode();
    public static final IRI ADDRESS = SimpleValueFactory.getInstance().createIRI("http://example.com/address");
    public static final IRI STREET = SimpleValueFactory.getInstance().createIRI("http://example.com/street");
    public static final Value ALICE_STREET = SimpleValueFactory.getInstance().createLiteral("123AB Street");

    public static final IRI IRI_GRAPH_NAME = SimpleValueFactory.getInstance().createIRI("http://www.example.com/graph2");
    public static final BNode BNODE_GRAPH_NAME = SimpleValueFactory.getInstance().createBNode();

    /**
     * Creates an RDF Model where alice knows bob, with statements all in the default graph.
     */
    public static Model modelAliceKnowsBobInDefaultGraph() {
        Model m  = new LinkedHashModel();

        m.add(ALICE, TYPE, PERSON);
        m.add(BOB, TYPE, PERSON);
        m.add(ALICE, KNOWS, BOB);
        m.add(ALICE, AGE, ALICE_AGE);
        m.add(BOB, NAME, BOB_NAME);
        m.add(ALICE, NAME, ALICE_NAME);
        m.add(ALICE, ADDRESS, B1);
        m.add(B1, STREET, ALICE_STREET);
        return m;
    }
    /**
     * Creates an RDF Model where alice knows bob, with statements in different named graphs.
     */
    public static Model modelAliceKnowsBobInNamedGraphs() {
        Model m  = new LinkedHashModel();

        m.add(ALICE, TYPE, PERSON, BNODE_GRAPH_NAME);
        m.add(BOB, TYPE, PERSON, BNODE_GRAPH_NAME);
        m.add(ALICE, KNOWS, BOB, BNODE_GRAPH_NAME);
        m.add(ALICE, AGE, ALICE_AGE, BNODE_GRAPH_NAME);
        m.add(BOB, NAME, BOB_NAME, IRI_GRAPH_NAME);
        m.add(ALICE, NAME, ALICE_NAME);
        m.add(ALICE, ADDRESS, B1, IRI_GRAPH_NAME);
        m.add(B1, STREET, ALICE_STREET, IRI_GRAPH_NAME);
        return m;
    }

    /**
     * Creates an RDF Model having statements with Literals in different data types.
     */
    public static Model modelManyDataTypes() {
        Model m  = new LinkedHashModel();

        Instant i = Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2022-05-16T14:37:56Z"));
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(Date.from(i));

        try {
            short s = 15;
            long l = 1500000000;
            byte b = -15;
            float f = 1.2345f;
            double d = 123e5;
            String str = "str";
            XMLGregorianCalendar date = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);

            Value stringLit = SimpleValueFactory.getInstance().createLiteral(str);
            Value shortLit = SimpleValueFactory.getInstance().createLiteral(s);
            Value longlit = SimpleValueFactory.getInstance().createLiteral(l);
            Value byteLit = SimpleValueFactory.getInstance().createLiteral(b);
            Value dateLit = SimpleValueFactory.getInstance().createLiteral(date);
            Value floatLit = SimpleValueFactory.getInstance().createLiteral(f);
            Value doubleLit = SimpleValueFactory.getInstance().createLiteral(d);

            IRI associated = SimpleValueFactory.getInstance().createIRI("http://www.example.com/associated");

            m.add(ALICE, associated, stringLit);
            m.add(ALICE, associated, shortLit);
            m.add(ALICE, associated, longlit);
            m.add(ALICE, associated, byteLit);
            m.add(ALICE, associated, dateLit);
            m.add(ALICE, associated, floatLit);
            m.add(ALICE, associated, doubleLit);

            return m;
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
