package software.amazon.neptune.onegraph.playground.server.servicespring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.server.service.SettingsService;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;

/**
 * Spring Service that handles the config command.
 */
@Service
public class SettingsServiceSpring extends SettingsService {

    @Autowired
    public SettingsServiceSpring(StateSpring state) {
        super(state);
    }
}
