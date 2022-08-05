package software.amazon.neptune.onegraph.playground.server.servicespring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;
import software.amazon.neptune.onegraph.playground.server.service.ExportService;

/**
 * Spring Service that handles the export command.
 */
@Service
public class ExportServiceSpring extends ExportService {

    @Autowired
    public ExportServiceSpring(StateSpring state) {
        super(state);
    }
}
