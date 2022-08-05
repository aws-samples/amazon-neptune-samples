package software.amazon.neptune.onegraph.playground.server.servicespring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.server.service.ViewService;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;

/**
 * Spring Service that handles the view command.
 */
@Service
public class ViewServiceSpring extends ViewService {

    @Autowired
    public ViewServiceSpring(StateSpring state, ExportServiceSpring es) {
        super(state, es);
    }
}
