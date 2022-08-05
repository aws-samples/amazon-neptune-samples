package software.amazon.neptune.onegraph.playground.client.response;

import java.util.HashSet;
import java.util.Set;

/**
 * Response format for GET requests to /info.
 */
public class InfoResponse {

    public InfoResponse() {}

    /**
     * String containing the current data stored server-side in OneGraph data format.
     */
    public String serializedOG;

    /**
     * Contains the line numbers of the lines that are deemed RDF non-compatible of the {@link InfoResponse#serializedOG}.
     */
    public Set<Integer> rdfNonCompatibleLines = new HashSet<>();

    /**
     * Contains the line numbers of the lines that are deemed LPG non-compatible of the {@link InfoResponse#serializedOG}.
     */
    public Set<Integer> lpgNonCompatibleLines = new HashSet<>();

    /**
     * Contains the line numbers of the lines that are deemed RDF pairwise non-compatible of the {@link InfoResponse#serializedOG}.
     */
    public Set<Integer> pairwiseNonCompatibleLines = new HashSet<>();
}
