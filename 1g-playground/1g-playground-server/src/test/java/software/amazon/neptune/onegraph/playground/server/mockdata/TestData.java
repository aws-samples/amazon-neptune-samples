package software.amazon.neptune.onegraph.playground.server.mockdata;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Contains ready-to-use sample mock data for test cases.
 */
public abstract class TestData {

    protected static final String baseIRIString = "http://example.com/";
    protected static final String bossString   = "Boss";
    protected static final String personString = "Person";
    protected static final String knowsString = "knows";
    protected static final String nameString = "name";
    protected static final String sinceString = "since";
    protected static final String ageString = "age";
    protected static final String heightString = "height";
    protected static final String friendsOfString = "friendsOf";
    protected static final String birthDateString = "birthDate";
    protected static final String cashBalanceString   = "cashBalance";
    protected static final String addressString   = "address";
    protected static final String vertexString = "vertex";
    protected final LPGVertex aliceVertex = vertexWithID("A");
    protected final LPGVertex bobVertex = vertexWithID("B");
    protected final LPGVertex charlieVertex   = vertexWithID("C");
    protected static final String aliceString = "Alice";
    protected static final String bobString = "Bob";
    protected static final Integer int44 = 44;
    protected static final Float float1314 = 13.14f;
    protected static final Long long1234 = 1234L;
    protected static final Byte byte80 = 80;
    protected static final Short short16 = 16;
    protected static final Date date1985 = get1985();
    protected static final Double double172 = 1.72;
    protected static final BNode bNodeA = SimpleValueFactory.getInstance().createBNode("A");
    protected static final BNode bNodeB = SimpleValueFactory.getInstance().createBNode("B");
    protected static final BNode bNodeC = SimpleValueFactory.getInstance().createBNode("C");
    protected static final BNode bNodeVertexA = SimpleValueFactory.getInstance().createBNode("vertexA");
    protected static final BNode bNodeVertexB = SimpleValueFactory.getInstance().createBNode("vertexB");
    protected static final BNode bNodeVertexC = SimpleValueFactory.getInstance().createBNode("vertexC");
    protected static final Literal literalPersonString = SimpleValueFactory.getInstance().createLiteral(personString);
    protected static final Literal literalFriendsOfString = SimpleValueFactory.getInstance().createLiteral(friendsOfString);
    protected static final Literal literalAliceString = SimpleValueFactory.getInstance().createLiteral(aliceString);
    protected static final Literal literalBobString = SimpleValueFactory.getInstance().createLiteral(bobString);
    protected static final Literal literalInt44 = SimpleValueFactory.getInstance().createLiteral(int44);
    protected static final Literal literalDouble172 = SimpleValueFactory.getInstance().createLiteral(double172);
    protected static final Literal literalDate1985 = SimpleValueFactory.getInstance().createLiteral(date1985);
    protected static final Literal literalFloat1314 = SimpleValueFactory.getInstance().createLiteral(float1314);
    protected static final Literal literalLong1234 = SimpleValueFactory.getInstance().createLiteral(long1234);
    protected static final Literal literalByte80 = SimpleValueFactory.getInstance().createLiteral(byte80);
    protected static final Literal literalShort16 = SimpleValueFactory.getInstance().createLiteral(short16);
    protected static final Literal literalVertex = SimpleValueFactory.getInstance().createLiteral(vertexString);
    protected static final IRI graph1 = SimpleValueFactory.getInstance().createIRI(baseIRIString + "graph1");
    protected static final IRI graph2 = SimpleValueFactory.getInstance().createIRI(baseIRIString + "graph2");
    protected static final BNode graph3 = SimpleValueFactory.getInstance().createBNode("graph3");
    protected static final IRI DEFAULT_GRAPH_IRI = SimpleValueFactory.getInstance().createIRI(URLConstants.DEFAULT_GRAPH);


    /**
     * Creates new LPG vertex with given id.
     * @param id The id for the vertex.
     * @return The vertex with id.
     */
    private static LPGVertex vertexWithID(String id) {
        LPGVertex v = new LPGVertex();
        v.setId(id);
        return v;
    }

    /**
     * The 6th of June 1985 00:00:00
     * @return 6th of June 1985 01:01:01.
     */
    private static Date get1985() {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(486910861000L);
        return calendar.getTime();
    }

    protected static IRI getKnowsIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + knowsString);
    }

    protected static IRI getSinceIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + sinceString);
    }

    protected static IRI getAgeIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + ageString);
    }

    protected static IRI getNameIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + nameString);
    }

    protected static IRI getHeightIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + heightString);
    }

    protected static IRI getBossIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + bossString);
    }

    protected static IRI getPersonIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + personString);
    }

    public static IRI getFriendsOfIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + friendsOfString);
    }


    public static IRI getAddressIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + addressString);
    }

    public static IRI getTypeIRI() {
        return RDF.TYPE;
    }

    public static IRI getID_A_IRI() {
        return SimpleValueFactory.getInstance().createIRI(URLConstants.ID + "A");
    }

    public static IRI getID_B_IRI() {
        return SimpleValueFactory.getInstance().createIRI(URLConstants.ID + "B");
    }

    public static IRI getID_C_IRI() {
        return SimpleValueFactory.getInstance().createIRI(URLConstants.ID + "C");
    }

    public static IRI getVertexIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + vertexString);
    }

    public static IRI getCashBalanceIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + cashBalanceString);
    }

    public static IRI getBirthDateIRI(String base) {
        return SimpleValueFactory.getInstance().createIRI(base + birthDateString);
    }
}
