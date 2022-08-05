package software.amazon.neptune.onegraph.playground.client.service;

import lombok.NonNull;
import software.amazon.neptune.onegraph.playground.client.scenarios.Scenario;
import software.amazon.neptune.onegraph.playground.client.scenarios.Scenario1;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.client.scenarios.Scenario2;
import software.amazon.neptune.onegraph.playground.client.scenarios.Scenario3;

/**
 * Service that handles the {@code scenario} command.
 */
@Service
public class ScenarioService {

    @Lazy
    @Autowired
    LineReader reader;

    /**
     * A reference to the data service is needed to make requests.
     */
    public final DataService dataService;

    /**
     * A reference to the info service is needed to make requests.
     */
    public final InfoService infoService;

    /**
     * A reference to the query service is needed to make requests.
     */
    public final QueryService queryService;

    @Autowired
    public ScenarioService(@NonNull InfoService infoService,
                           @NonNull DataService dataService,
                           @NonNull QueryService queryService) {
        this.infoService = infoService;
        this.dataService = dataService;
        this.queryService = queryService;
    }

    /**
     * Runs the correct scenario given a command line argument.
     * @param sc1 Set to {@code true} to run scenario 1, {@code false} otherwise.
     * @param sc2 Set to {@code true} to run scenario 2, {@code false} otherwise.
     * @param sc3 Set to {@code true} to run scenario 3, {@code false} otherwise.
     * @throws IllegalArgumentException When not exactly one of {@code sc1, sc2,} or {sc3} is {@code true}.
     */
    public void runScenario(boolean sc1,
                            boolean sc2,
                            boolean sc3) throws IllegalArgumentException {
        checkParams(new boolean[]{sc1, sc2, sc3});

        Scenario scenario;
        if (sc1) {
            scenario = new Scenario1(this.reader, this);
        } else if (sc2) {
            scenario = new Scenario2(this.reader, this);
        } else if (sc3) {
            scenario = new Scenario3(this.reader, this);
        } else {
            throw new IllegalArgumentException("Please specify a scenario flag to run.");
        }
        scenario.run();
    }

    private void checkParams(boolean[] input) throws IllegalArgumentException {
        int inputCount = 0;
        for (boolean s : input) {
            if (s) {
                inputCount++;
            }
        }
        if (inputCount > 1) {
            throw new IllegalArgumentException("Too many scenario flags, please specify exactly 1 flag.");
        }
    }
}
