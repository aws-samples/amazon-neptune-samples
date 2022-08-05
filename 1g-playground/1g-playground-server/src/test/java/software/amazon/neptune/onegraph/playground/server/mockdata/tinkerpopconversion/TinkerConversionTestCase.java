package software.amazon.neptune.onegraph.playground.server.mockdata.tinkerpopconversion;

import org.apache.tinkerpop.gremlin.structure.Graph;
import software.amazon.neptune.onegraph.playground.server.mapping.TinkerPopConverter;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Test scenarios for the conversion between LPG {@link LPGGraph}s and TinkerPop {@link Graph}s.
 */
public abstract class TinkerConversionTestCase {

    /**
     * Resulting TinkerPop graph when using the {@link TinkerPopConverter}
     * on the graph given by {@link #lpgGraph()}.
     */
    public abstract Graph tinkerPopGraph();

    /**
     * Resulting LPG when using the {@link TinkerPopConverter}
     * on the graph given by {@link #tinkerPopGraph()}.
     */
    public abstract  LPGGraph lpgGraph();

    /**
     * The 6th of June 1985 00:00:00
     * @return 6th of June 1985 00:00:00.
     */
    protected Date get1985() {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(486910861000L);
        return calendar.getTime();
    }
}
