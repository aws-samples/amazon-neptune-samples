package software.amazon.neptune.onegraph.playground.server.tests.unit.querying;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.neptune.onegraph.playground.server.mockdata.io.IORDFTestData;
import software.amazon.neptune.onegraph.playground.server.querying.QueryException;
import software.amazon.neptune.onegraph.playground.server.querying.SPARQLQuerying;
import software.amazon.neptune.onegraph.playground.server.querying.SPARQLQueryingDelegate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SPARQLQueryingTest {

    private static final Repository repo = new SailRepository(new MemoryStore());

    @BeforeAll
    public static void loadData() {
        try (RepositoryConnection con = repo.getConnection()) {
            con.add(IORDFTestData.modelAliceKnowsBobInDefaultGraph());
        }
    }

    @Test
    public void ASKAliceHasAgeAlice_thenResultIsTrue() {
        SPARQLQueryingDelegate d = new SPARQLQueryingDelegate() {
            @Override
            public void SPARQLQueryFinishedWithResult(boolean result) {
                assertTrue(result);
            }
            @Override
            public void SPARQLQueryFinishedWithResult(Model result) { fail();}
            @Override
            public void SPARQLQueryFinishedWithResult(Map<String, List<Value>> result) {fail();}

            @Override
            public void SPARQLQueryFailedWithException(QueryException exception) {
                fail();
            }
        };
        SPARQLQuerying q = new SPARQLQuerying(d);
        String query = String.format("ASK { ?s <%s> %s . ?s <%s> %s}",
                IORDFTestData.NAME,
                IORDFTestData.ALICE_NAME.toString(),
                IORDFTestData.AGE,
                IORDFTestData.ALICE_AGE);
        q.queryRepository(repo, query, 15000);
    }

    @Test
    public void ASKBobIsAgeAlice_thenResultFalse() {
        SPARQLQueryingDelegate d = new SPARQLQueryingDelegate() {
            @Override
            public void SPARQLQueryFinishedWithResult(boolean result) {
                assertFalse(result);
            }
            @Override
            public void SPARQLQueryFinishedWithResult(Model result) { fail();}
            @Override
            public void SPARQLQueryFinishedWithResult(Map<String, List<Value>> result) {fail();}

            @Override
            public void SPARQLQueryFailedWithException(QueryException exception) {
                fail();
            }
        };
        SPARQLQuerying q = new SPARQLQuerying(d);
        String query = String.format("ASK { ?s <%s> %s . ?s <%s> %s}",
                IORDFTestData.NAME,
                IORDFTestData.BOB_NAME.toString(),
                IORDFTestData.AGE,
                IORDFTestData.ALICE_AGE);
        q.queryRepository(repo, query, 15000);
    }

    @Test
    public void malformedSPARQL() {
        SPARQLQueryingDelegate d = new SPARQLQueryingDelegate() {
            @Override
            public void SPARQLQueryFinishedWithResult(boolean result) {
                fail();
            }
            @Override
            public void SPARQLQueryFinishedWithResult(Model result) {fail();}
            @Override
            public void SPARQLQueryFinishedWithResult(Map<String, List<Value>> result) {fail();}

            @Override
            public void SPARQLQueryFailedWithException(QueryException exception) {
                assertTrue(exception.getMessage().contains("An exception occurred"));
            }
        };
        SPARQLQuerying q = new SPARQLQuerying(d);
        String query = "MALFORMED { ?s ?p ?o }";
        q.queryRepository(repo, query, 15000);
    }

    @Test
    public void CONSTRUCTGetAllTriples() {
        SPARQLQueryingDelegate d = new SPARQLQueryingDelegate() {
            @Override
            public void SPARQLQueryFinishedWithResult(boolean result) {fail();}
            @Override
            public void SPARQLQueryFinishedWithResult(Model result) {
                assertEquals(IORDFTestData.modelAliceKnowsBobInDefaultGraph(), result);
            }
            @Override
            public void SPARQLQueryFinishedWithResult(Map<String, List<Value>> result) {fail();}

            @Override
            public void SPARQLQueryFailedWithException(QueryException exception) {
                fail();
            }
        };

        SPARQLQuerying q = new SPARQLQuerying(d);
        String query = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
        q.queryRepository(repo, query, 15000);
    }

    @Test
    public void CONSTRUCTGetAllTypeTriples() {
        SPARQLQueryingDelegate d = new SPARQLQueryingDelegate() {
            @Override
            public void SPARQLQueryFinishedWithResult(boolean result) {fail();}
            @Override
            public void SPARQLQueryFinishedWithResult(Model result) {
                assertEquals(2, result.size());
                assertEquals(1, result.objects().size());
                assertTrue(result.objects().contains(IORDFTestData.PERSON));
            }
            @Override
            public void SPARQLQueryFinishedWithResult(Map<String, List<Value>> result) {fail();}

            @Override
            public void SPARQLQueryFailedWithException(QueryException exception) {
                fail();
            }
        };

        SPARQLQuerying q = new SPARQLQuerying(d);
        String query = String.format("CONSTRUCT { ?s <%s> ?o } WHERE { ?s <%s> ?o }", IORDFTestData.TYPE, IORDFTestData.TYPE);
        q.queryRepository(repo, query, 15000);
    }

    @Test
    public void SELECTGetAllTuples() {
        SPARQLQueryingDelegate d = new SPARQLQueryingDelegate() {
            @Override
            public void SPARQLQueryFinishedWithResult(boolean result) {fail();}
            @Override
            public void SPARQLQueryFinishedWithResult(Model result) {fail();}
            @Override
            public void SPARQLQueryFinishedWithResult(Map<String, List<Value>> result) {
                assertTrue(result.containsKey("s"));
                assertTrue(result.containsKey("p"));
                assertTrue(result.containsKey("o"));
                assertTrue(result.get("s").size() > 5);
                assertTrue(result.get("p").size() > 5);
                assertTrue(result.get("o").size() > 5);
            }

            @Override
            public void SPARQLQueryFailedWithException(QueryException exception) {
                fail();
            }
        };

        SPARQLQuerying q = new SPARQLQuerying(d);
        String query = "SELECT ?s ?p ?o WHERE { ?s ?p ?o }";
        q.queryRepository(repo, query, 15000);
    }
}
