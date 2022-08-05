package software.amazon.neptune.onegraph.playground.server.servicespring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.neptune.onegraph.playground.server.service.QueryService;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;

/**
 * Spring Service that handles the query command.
 */
@Service
public class QueryServiceSpring extends QueryService {

    @Autowired
    public QueryServiceSpring(StateSpring state) {
        super(state);
    }
}
