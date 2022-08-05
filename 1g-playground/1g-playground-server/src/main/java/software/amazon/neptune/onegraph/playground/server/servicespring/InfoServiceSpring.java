package software.amazon.neptune.onegraph.playground.server.servicespring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.server.service.InfoService;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;

/**
 * Spring Service that handles the info command.
 */
@Service
public class InfoServiceSpring extends InfoService {

    @Autowired
    public InfoServiceSpring(StateSpring state) {
        super(state);
    }
}
