package software.amazon.neptune.onegraph.playground.server.api.response;

import java.util.HashSet;
import java.util.Set;

/**
 * Response format of client for info command.
 */
public class InfoResponse {

    public InfoResponse() {}

    /**
     * Contains the serialized onegraph string.
     */
    public String serializedOG;

    /**
     * Contains line numbers of lines in the {@link #serializedOG} that are not RDF compatible.
     */
    public Set<Integer> rdfNonCompatibleLines = new HashSet<>();

    /**
     * Contains line numbers of lines in the {@link #serializedOG} that are not LPG compatible.
     */
    public Set<Integer> lpgNonCompatibleLines = new HashSet<>();

    /**
     * Contains line numbers of lines in the {@link #serializedOG} that are pairwise RDF non-compatible.
     */
    public Set<Integer> pairwiseNonCompatibleLines = new HashSet<>();
}
