package software.amazon.neptune.onegraph.playground.server.servicespring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.server.service.LoadService;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;

/**
 * Spring Service that handles the load command.
 */
@Service
public class LoadServiceSpring extends LoadService {

    @Autowired
    public LoadServiceSpring(StateSpring state) {
        super(state);
    }
}
