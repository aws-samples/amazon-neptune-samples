package software.amazon.neptune.onegraph.playground.server.mockdata.io;

import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGProperty;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Contains different test scenarios for {@link LPGGraph}s
 */
public class IOLPGTestData {

    public static LPGGraph aliceKnowsBobSince2005() {
        LPGGraph g = new LPGGraph();

        LPGVertex alice = new LPGVertex("Person", "Boss");
        LPGVertex bob = new LPGVertex("Person");

        LPGEdge knows = new LPGEdge(alice, bob, "knows");
        LPGProperty since = new LPGProperty("since", "2k15");
        LPGProperty aliceNam = new LPGProperty("name", "Alice");
        LPGProperty aliceAge = new LPGProperty("age", 44);
        LPGProperty bobNam = new LPGProperty("name", "Bob");

        alice.addProperty(aliceAge);
        alice.addProperty(aliceNam);
        bob.addProperty(bobNam);
        knows.addProperty(since);

        g.addVertex(alice);
        g.addVertex(bob);
        g.addEdge(knows);

        return g;
    }

    public static LPGGraph aliceKnowsBobTwiceWithEdgeProperties() {
        LPGGraph g = new LPGGraph();

        LPGVertex alice = new LPGVertex("Person", "Boss");
        LPGVertex bob = new LPGVertex("Person");

        LPGEdge knows = new LPGEdge(alice, bob, "knows");
        LPGEdge knows2 = new LPGEdge(alice, bob, "knows");
        LPGProperty aliceNam = new LPGProperty("name", "Alice");
        LPGProperty aliceAge = new LPGProperty("age", 44);
        LPGProperty bobNam = new LPGProperty("name", "Bob");

        LPGProperty nyTimes = new LPGProperty("statedBy", "NYTimes");
        LPGProperty guard = new LPGProperty("statedBy", "TheGuardian");
        LPGProperty since2021 = new LPGProperty("since", 2021);
        LPGProperty since2020 = new LPGProperty("since", 2020);

        alice.addProperty(aliceAge);
        alice.addProperty(aliceNam);
        bob.addProperty(bobNam);

        knows.addProperty(nyTimes);
        knows.addProperty(since2021);
        knows2.addProperty(guard);
        knows2.addProperty(since2020);

        g.addVertex(alice);
        g.addVertex(bob);
        g.addEdge(knows);
        g.addEdge(knows2);

        return g;
    }

    public static LPGGraph aliceKnowsBobTwice() {
        LPGGraph g = new LPGGraph();

        LPGVertex alice = new LPGVertex("Person", "Boss");
        LPGVertex bob = new LPGVertex("Person");

        LPGEdge knows = new LPGEdge(alice, bob, "knows");
        LPGEdge knows2 = new LPGEdge(alice, bob, "knows");
        LPGProperty aliceNam = new LPGProperty("name", "Alice");
        LPGProperty aliceAge = new LPGProperty("age", 44);
        LPGProperty bobNam = new LPGProperty("name", "Bob");

        alice.addProperty(aliceAge);
        alice.addProperty(aliceNam);
        bob.addProperty(bobNam);

        g.addVertex(alice);
        g.addVertex(bob);
        g.addEdge(knows);
        g.addEdge(knows2);

        return g;
    }

    public static LPGGraph unlabeledVertex() {
        LPGGraph g = new LPGGraph();
        g.addVertex(new LPGVertex());

        return g;
    }

    public static LPGGraph empty() {
        return new LPGGraph();
    }

    public static LPGGraph selfLoop() {
        LPGGraph g = new LPGGraph();
        LPGVertex v = new LPGVertex();
        LPGEdge e = new LPGEdge(v, v, "knows");

        g.addVertex(v);
        g.addEdge(e);

        return g;
    }

    public static LPGGraph multiValuedProperty() {
        LPGGraph g = new LPGGraph();
        LPGVertex v = new LPGVertex("Person");

        LPGProperty ages = new LPGProperty("age", 21, 22, "23", 24.0f, 25.0d, get1985());

        v.addProperty(ages);
        g.addVertex(v);

        return g;
    }

    public static LPGGraph complexProperties() {
        LPGGraph g = new LPGGraph();
        LPGVertex v1 = new LPGVertex("Person");
        LPGVertex v2 = new LPGVertex("Person");
        LPGVertex v3 = new LPGVertex("Person");
        LPGEdge e1 = new LPGEdge(v1, v2,"edge1");
        LPGEdge e2 = new LPGEdge(v2, v3,"edge2");

        LPGProperty empty = new LPGProperty("emptyProp");
        LPGProperty age1 = new LPGProperty("age",25);
        LPGProperty age2 = new LPGProperty("age",25, 26, 27);
        LPGProperty height1 = new LPGProperty("height", 0x000);
        LPGProperty height2 = new LPGProperty("height", "a height");

        LPGProperty unsupported = new LPGProperty("aChar", 'a');
        LPGProperty edgeProp1 = new LPGProperty("edgeProp1", 2005);
        LPGProperty edgeProp2 = new LPGProperty("edgeProp2", 1955);

        LPGProperty dateProp = new LPGProperty("date", get1985());

        v1.addProperty(empty);
        v1.addProperty(age1);
        v1.addProperty(height1);
        v3.addProperty(unsupported);

        v2.addProperty(age2);
        v2.addProperty(height2);
        v2.addProperty(unsupported);

        e1.addProperty(edgeProp1);
        e2.addProperty(edgeProp2);
        e2.addProperty(dateProp);

        g.addVertices(v1, v2, v3);
        g.addEdges(e1, e2);

        return g;
    }

    public static LPGGraph arrayProperty() {
        LPGGraph g = new LPGGraph();
        LPGVertex v = new LPGVertex("Person");

        List<Integer> l = Arrays.asList(21, 22, 23, 24);
        LPGProperty ages = new LPGProperty("age", l);

        v.addProperty(ages);
        g.addVertex(v);

        return g;
    }

    public static LPGGraph complexLabels() {
        LPGGraph g = new LPGGraph();
        LPGVertex v1 = new LPGVertex("Person, Friend");
        LPGVertex v2 = new LPGVertex("Person","Boss","%afe'");
        LPGVertex v3 = new LPGVertex("Person","Boss");
        LPGVertex v4 = new LPGVertex("[]]Person");
        LPGEdge e1 = new LPGEdge(v1, v2,"knows;thinks \" wants");
        LPGEdge e2 = new LPGEdge(v3, v4,"\"quoted\"");
        g.addVertices(v1, v2, v3, v4);
        g.addEdges(e1, e2);
        return g;
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
}
