package software.amazon.neptune.onegraph.playground.server.api.controller;

import software.amazon.neptune.onegraph.playground.server.api.response.InfoResponse;
import software.amazon.neptune.onegraph.playground.server.servicespring.InfoServiceSpring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import software.amazon.neptune.onegraph.playground.server.state.State;

/**
 * REST controller for the info command.
 */
@RestController
@RequestMapping("/info")
public class InfoController {

    private final InfoServiceSpring infoService;

    @Autowired
    public InfoController(InfoServiceSpring is) {
        this.infoService = is;
    }

    /**
     * Callback for GET requests to the /info path, provides info about the current data in the
     * {@link State} to the client.
     */
    @GetMapping
    public InfoResponse info() {
        return this.infoService.getInfo();
    }
}
