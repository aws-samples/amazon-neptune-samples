package software.amazon.neptune.onegraph.playground.server.servicespring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.server.service.ClearService;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;

/**
 * Spring Service that handles the clear command.
 */
@Service
public class ClearServiceSpring extends ClearService {
    @Autowired
    public ClearServiceSpring(StateSpring state) {
        super(state);
    }
}