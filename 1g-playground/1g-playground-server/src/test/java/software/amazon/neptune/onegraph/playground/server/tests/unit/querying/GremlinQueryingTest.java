package software.amazon.neptune.onegraph.playground.server.tests.unit.querying;

import software.amazon.neptune.onegraph.playground.server.mockdata.io.IOTinkerPopTestData;
import software.amazon.neptune.onegraph.playground.server.querying.GremlinQuerying;
import software.amazon.neptune.onegraph.playground.server.querying.GremlinQueryingDelegate;
import software.amazon.neptune.onegraph.playground.server.querying.QueryException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class GremlinQueryingTest {

    @Test
    public void malformedGremlin() {
        GremlinQueryingDelegate d = new GremlinQueryingDelegate() {
            @Override
            public void gremlinQueryFinishedWithResult(List<?> result) {
                fail();
            }

            @Override
            public void gremlinQueryFailedWithException(QueryException exception) {
                assertTrue(exception.getMessage().contains("An exception occurred"));
            }
        };

        GremlinQuerying q = new GremlinQuerying(d);
        q.queryGraph(IOTinkerPopTestData.tinkerPopGraphAliceKnowsBobWithEmptyNode(), "g.V(0).valu", 15000);
    }

    @Test
    public void getVerticesTraversal() {
        GremlinQueryingDelegate d = new GremlinQueryingDelegate() {

            @Override
            public void gremlinQueryFinishedWithResult(List<?> result) {
                assertEquals(3, result.size());
            }

            @Override
            public void gremlinQueryFailedWithException(QueryException exception) {
                fail();
            }
        };

        String traversal = "g.V()";
        GremlinQuerying q = new GremlinQuerying(d);
        q.queryGraph(IOTinkerPopTestData.tinkerPopGraphAliceKnowsBobWithEmptyNode(), traversal, 15000);
    }

    @Test
    public void getValueMapTraversal() {
        GremlinQueryingDelegate d = new GremlinQueryingDelegate() {

            @Override
            public void gremlinQueryFinishedWithResult(List<?> result) {
                assertEquals(3, result.size());
            }

            @Override
            public void gremlinQueryFailedWithException(QueryException exception) {
                fail();
            }
        };

        String traversal = "g.V().valueMap(true)";
        GremlinQuerying q = new GremlinQuerying(d);
        q.queryGraph(IOTinkerPopTestData.tinkerPopGraphAliceKnowsBobWithEmptyNode(), traversal, 15000);
    }
}
