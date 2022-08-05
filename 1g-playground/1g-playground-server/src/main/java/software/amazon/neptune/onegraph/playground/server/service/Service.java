package software.amazon.neptune.onegraph.playground.server.service;

import software.amazon.neptune.onegraph.playground.server.state.State;

/**
 * A service handles a particular command of the playground. <br />
 * Subclasses: <br />
 * {@link ClearService},
 * {@link SettingsService},
 * {@link ExportService},
 * {@link InfoService},
 * {@link LoadService},
 * {@link QueryService},
 * {@link ViewService}
 *
 */
public abstract class Service {

    /**
     * Each service needs to have a reference to the {@link State}.
     */
    protected final State state;

    protected Service(State state) {
        this.state = state;
    }
}
