package software.amazon.neptune.onegraph.playground.server.service;

import lombok.NonNull;
import software.amazon.neptune.onegraph.playground.server.api.response.InfoResponse;
import software.amazon.neptune.onegraph.playground.server.io.serializing.OGSerializer;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;
import software.amazon.neptune.onegraph.playground.server.state.State;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service that handles the info command.
 */
public class InfoService extends Service {

    /**
     * Initialize the info service with the given state.
     * @param state The state.
     */
    public InfoService(State state) {
        super(state);
    }

    /**
     * Gets information about the current OG Data set in the {@link State},
     * info includes:
     * - if the data set is RDF or LPG Compatible.
     * - all Statements in the data set.
     * - for each statement whether it is RDF or LPG compatible.
     * @return Info about the current OG Data set in the {@link State}.
     */
    public InfoResponse getInfo() {
        InfoResponse response = new InfoResponse();

        OGSerializer serializer = new OGSerializer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Map<OGTripleStatement, Integer> statementToLine = serializer.serializeToOG(outputStream, state.getOgDataset());
        response.serializedOG = outputStream.toString();

        Map<String, Integer> occurrences = getTripleOccurrences(statementToLine.keySet());
        for (Map.Entry<OGTripleStatement, Integer> entry : statementToLine.entrySet()) {
            OGTripleStatement triple = entry.getKey();
            Integer lineNumber = entry.getValue();
            if (!isRDFCompatible(triple, state.getOgDataset())) {
                response.rdfNonCompatibleLines.add(lineNumber);
            } else if (occurrences.get(getTripleFingerprint(triple)) > 1){
                response.pairwiseNonCompatibleLines.add(lineNumber);
            }
            if (!isLPGCompatible(triple, state.getOgDataset())) {
                response.lpgNonCompatibleLines.add(lineNumber);
            }

        }
        return response;
    }

    // Get a uniquely identifying string from the given triple, this is used to detect
    // RDF pairwise incompatibility.
    private String getTripleFingerprint(@NonNull OGTripleStatement triple) {
        int s = triple.getSubject().hashCode();
        int p = triple.getPredicate().hashCode();
        int o = triple.getObject().hashCode();
        return s + String.valueOf(p) + o;
    }

    // Count how many times the 'same' (same s p and o) triple occurs in the given set.
    private Map<String, Integer> getTripleOccurrences(@NonNull Set<OGTripleStatement> triples) {

        Map<String, Integer> occurrences = new HashMap<>();
        for (OGTripleStatement s : triples) {

            String fp = getTripleFingerprint(s);
            occurrences.merge(fp, 1, Integer::sum);
        }
        return occurrences;
    }

    // Returns true if the given statement is RDF compatible, false otherwise.
    private boolean isRDFCompatible(@NonNull OGTripleStatement statement, @NonNull OGDataset dataset) {
        Optional<Set<OGGraph<?>>> graphs = dataset.graphsForStatement(statement);
        // Check if the statement is asserted.
        if (!graphs.isPresent()) {
            return false;
        }

        // These 2 checks test if the subject/object are bnodes or iris.
        boolean isOutNode = statement.getSubject().canBeOutNodeOfEdge();
        boolean isInNode = statement.getObject().canBeInNodeOfEdge();
        if (isOutNode && isInNode) {
            return true;
        }
        // This checks if the subject is a bnode, iri, or vertex and the object is a value.
        return statement.canBecomeNodeProperty();
    }

    // Returns true if the given statement is LPG compatible, false otherwise.
    private boolean isLPGCompatible(@NonNull OGTripleStatement statement, @NonNull OGDataset dataset) {
        Optional<Set<OGGraph<?>>> graphs = dataset.graphsForStatement(statement);
        // Check if the statement is asserted.
        if (!graphs.isPresent()) {
            return false;
        }

        boolean isOutNode = statement.getSubject().canBeOutNodeOfEdge();
        boolean isInNode = statement.getObject().canBeInNodeOfEdge();
        if (isOutNode && isInNode) {
            return true;
        }

        if (statement.getSubject().canBeOutNodeOfEdge()) {
            return true;
        } else {
            return this.isStatementAboutEdgeProperty(statement, dataset);

        }
    }

    private boolean isStatementAboutEdgeProperty(@NonNull OGTripleStatement statement, @NonNull OGDataset dataset) {
        // Check if this statement is about an edge property, also check if the relationship statement that must be then in
        // subject position, is asserted in some graph.
        if (statement.canBecomeEdgeProperty() && statement.getSubject() instanceof OGRelationshipStatement) {
            OGRelationshipStatement relStat = (OGRelationshipStatement) statement.getSubject();
            Optional<Set<OGGraph<?>>> relGraphs = dataset.graphsForStatement(relStat);
            return relGraphs.isPresent();
        }
        return false;
    }
}
