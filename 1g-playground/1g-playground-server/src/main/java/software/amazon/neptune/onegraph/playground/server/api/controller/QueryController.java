package software.amazon.neptune.onegraph.playground.server.api.controller;

import org.apache.commons.lang.exception.ExceptionUtils;
import software.amazon.neptune.onegraph.playground.server.querying.QueryException;
import software.amazon.neptune.onegraph.playground.server.servicespring.QueryServiceSpring;
import software.amazon.neptune.onegraph.playground.server.api.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.neptune.onegraph.playground.server.state.State;
import software.amazon.neptune.onegraph.playground.server.state.StateSpring;

import java.sql.Time;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * REST controller for the query command.
 */
@RestController
@RequestMapping("/query")
public class QueryController {

    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    private final QueryServiceSpring queryService;

    private final StateSpring state;

    @Autowired
    public QueryController(QueryServiceSpring qs, StateSpring state) {
        this.queryService = qs;
        this.state = state;
    }

    /**
     * Callback for GET requests to the /query/gremlin path, allows the client to query the data currently in
     * the {@link State} with Gremlin.
     * @param query The gremlin query.
     * @return The results of the query, the {@link QueryResponse#traversalResult} will be set.
     * @throws InterruptedException When the query was interrupted.
     * @throws TimeoutException When the query was timed out.
     * @throws ExecutionException When there was an exception during the execution of the query.
     */
    @GetMapping("/gremlin")
    @ResponseBody
    public QueryResponse performGremlinQuery(@RequestParam(name="query") String query) throws InterruptedException,
            ExecutionException, TimeoutException {
        Future<QueryResponse> future = this.queryService.performGremlinQuery(query);

        return future.get(this.state.getQueryTimeoutMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Callback for GET requests to the /query/gremlin path, allows the client to query the data currently in
     * the {@link State} with SPARQL.
     * @param query The SPARQL query.
     * @return The results of the query, the {@link QueryResponse#traversalResult} will be set.
     * @throws InterruptedException When the query was interrupted.
     * @throws TimeoutException When the query was timed out.
     * @throws ExecutionException When there was an exception during the execution of the query.
     */
    @GetMapping("/sparql")
    @ResponseBody
    public QueryResponse performSPARQLQuery(@RequestParam(name="query") String query) throws InterruptedException,
            ExecutionException, QueryException, TimeoutException {

        Future<QueryResponse> future = this.queryService.performSPARQLQuery(query);
        return future.get(this.state.getQueryTimeoutMillis(), TimeUnit.MILLISECONDS);
    }

    @ExceptionHandler
    public ResponseEntity<String> handleException(Exception exception) {
        logger.warn("Query exception: " + exception.getCause().getMessage());

        Throwable cause = exception.getCause();
        Throwable rootCause = ExceptionUtils.getRootCause(exception);

        String errorToReturn = cause.getMessage() + " ROOT CAUSE: " + rootCause.getMessage();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorToReturn);
    }
}
