package software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset;

import com.github.jsonldjava.utils.Obj;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGMembershipStatement;
import com.google.common.collect.Iterables;
import lombok.NonNull;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.springframework.lang.Nullable;

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * A {@link OGDataset} represents a OneGraph dataset. It contains {@link OGStatement}s, {@link OGGraph}s
 * and, {@link OGSimpleNode}s.
 */
public class OGDataset {

    // Gets incremented every time a new formal number is generated.
    private long idIncrementer = 0;

    // This index maps statements to graphs that appear in membership statements together.
    private final HashMap<OGStatement, Set<OGGraph<?>>> statementsToGraphs = new HashMap<>();

    // Stores all relationship statements in this dataset
    private final LinkedHashSet<OGRelationshipStatement> relStatements  = new LinkedHashSet<>();
    // Stores all property statements in this dataset
    private final LinkedHashSet<OGPropertyStatement> propStatements = new LinkedHashSet<>();
    // Stores all membership statements in this dataset
    private final LinkedHashSet<OGMembershipStatement> memStatements = new LinkedHashSet<>();

    // Stores all graphs in this dataset
    private final Map<Object, OGGraph<?>> graphNameToGraph = new HashMap<>();
    // Stores all simple nodes in this dataset
    private final Map<IRI, OGSimpleNodeIRI> iriToSimpleNode = new HashMap<>();
    private final Map<BNode, OGSimpleNodeBNode> bNodeToSimpleNode = new HashMap<>();

    // Index from LPG Edge IDs to their respective relationship statements.
    private final Map<Object, OGRelationshipStatement> lpgEdgeIDToRelStatement = new HashMap<>();

    /**
     * Adds given triple statement to this dataset, together with a membership statement assigning the statement
     * to the {@code DEFAULT GRAPH}.
     * @param statement The statement
     */
    public void addStatementAndAddToDefaultGraph(@NonNull OGTripleStatement statement) {
        addStatementAndAddToGraph(statement, OGGraph.DEFAULT_GRAPH_IRI);
    }

    /**
     * Adds given triple statement to this dataset, together with a membership statement assigning the statement
     * to the {@code graphName}. Will create a new {@link OGGraph} with given {@code graphName} in {@link OGDataset#getGraphs()}
     * if there was no existing graph with that name yet.
     * @param statement The statement
     * @param graphName The graph name to use in the membership statement
     */
    public void addMembershipStatementForStatement(@NonNull OGTripleStatement statement, Resource graphName) {
        OGGraph<?> graph = this.createOrObtainGraph(graphName);
        OGMembershipStatement membership = new OGMembershipStatement(statement, graph);
        addStatement(membership);
    }

    /**
     * Retrieves all graphs a given statement is in, or the empty optional if that statement is not in a graph.
     * @param statement Optional containing the set of graphs or empty.
     * @return The set of graphs this statement is in, or the empty optional if this statement is in no graphs.
     */
    public Optional<Set<OGGraph<?>>> graphsForStatement(@NonNull OGTripleStatement statement) {
        if (statementsToGraphs.containsKey(statement)) {
            return Optional.of(statementsToGraphs.get(statement));
        }
        return Optional.empty();
    }

    /**
     * Adds given relationship statement to this dataset in the graph with given {@code graphName}, adds a
     * membership statement.
     * @param statement The relationship statement
     * @param graphName The name of the graph, pass {@code null} to indicate the default graph.
     */
    public void addStatementAndAddToGraph(@NonNull OGTripleStatement statement, @Nullable Resource graphName) {
        addStatement(statement);
        addMembershipStatementForStatement(statement, graphName);
    }

    /**
     * Adds given relationship statement to this dataset
     * @param statement The relationship statement
     */
    public void addStatement(@NonNull OGRelationshipStatement statement) {
        statement.setLocalId(getUniqueID());
        if (statement.edgeIDLPG != null) {
            this.lpgEdgeIDToRelStatement.put(statement.edgeIDLPG, statement);
        }
        relStatements.add(statement);
    }

    /**
     * Gets the relationship statement tied to the given edge ID if there exists any.
     * @param edgeID The edge ID to look for.
     * @return The relationship statement or the empty optional.
     */
    public Optional<OGRelationshipStatement> getRelationshipStatementForLPGEdgeID(Object edgeID) {
        return Optional.ofNullable(this.lpgEdgeIDToRelStatement.get(edgeID));
    }

    /**
     * Adds given triple statement to this dataset
     * @param statement The relationship statement
     */
    public void addStatement(@NonNull OGTripleStatement statement) {
        if (statement instanceof OGRelationshipStatement) {
            addStatement((OGRelationshipStatement) statement);
        } else {
            addStatement((OGPropertyStatement) statement);
        }
    }

    /**
     * Adds given property statement to this dataset
     * @param statement The property statement
     */
    public void addStatement(@NonNull OGPropertyStatement statement) {
        statement.setLocalId(getUniqueID());
        propStatements.add(statement);
    }


    /**
     * Adds given membership statement to this dataset
     * @param statement The membership statement
     */
    public void addStatement(@NonNull OGMembershipStatement statement) {
        statement.setLocalId(getUniqueID());
        Set<OGGraph<?>> memStatementsForStatement = statementsToGraphs.computeIfAbsent(statement.statement, k -> new HashSet<>());
        memStatementsForStatement.add(statement.graph);
        memStatements.add(statement);
    }

    /**
     * Check if the given statement is asserted in a graph in this dataset.
     * @param stat The statement.
     * @return {@code true} if the given statement is asserted in some graph, {@code false} otherwise.
     */
    public boolean isStatementAsserted(@NonNull OGStatement stat) {
        return statementsToGraphs.containsKey(stat);
    }

    /**
     * Creates a new graph in the dataset with given graph name, or obtains an existing one.
     * @param graphName The name of the graph, use {@link OGGraph#DEFAULT_GRAPH_IRI} or
     * {@code null} to indicate the default graph.
     * @return The graph with given graph name.
     */
    public OGGraph<?> createOrObtainGraph(@Nullable Resource graphName) {
        if (graphName == null) {
            graphName = OGGraph.DEFAULT_GRAPH_IRI;
        }

        OGGraph<?> graph;
        if (!this.containsGraphForName(graphName)) {
            graph = new OGGraph<>(graphName);
            addGraph(graph);
        } else {
            graph = this.graphNameToGraph.get(graphName);
        }
        return graph;
    }

    /**
     * Adds a new graph in the dataset.
     * @param graph The graph
     */
    public void addGraph(@NonNull OGGraph<?> graph) {
        if (!this.graphNameToGraph.containsKey(graph.graphName)) {
            this.graphNameToGraph.put(graph.graphName, graph);
            graph.setLocalId(getUniqueID());
        }
    }

    /**
     * Checks if the dataset contains a graph with given name
     * @param graphName The name of the graph, use {@link OGGraph#DEFAULT_GRAPH_IRI} to indicate the default graph.
     * @return {@code true} if the dataset contains a graph with given name, false otherwise.
     */
    public boolean containsGraphForName(@NonNull Resource graphName) {
        return this.graphNameToGraph.containsKey(graphName);
    }

    /**
     * Checks if the dataset contains a simple node with given component
     * @param linkedComponent The component linked to the simple node.
     * @return {@code true} if the dataset contains a simple node with given component, false otherwise.
     */
    public boolean containsSimpleNodeForComponent(@NonNull BNode linkedComponent) {
        return this.bNodeToSimpleNode.containsKey(linkedComponent);
    }

    /**
     * Checks if the dataset contains a simple node with given component
     * @param linkedComponent The component linked to the simple node.
     * @return {@code true} if the dataset contains a simple node with given component, false otherwise.
     */
    public boolean containsSimpleNodeForComponent(@NonNull IRI linkedComponent) {
        return this.iriToSimpleNode.containsKey(linkedComponent);
    }

    /**
     * Checks if the dataset contains a simple node with given component
     * @param linkedComponent The component linked to the simple node.
     * @return {@code true} if the dataset contains a simple node with given component, false otherwise.
     */
    public boolean containsSimpleNodeForComponent(@NonNull LPGVertex linkedComponent) {
        IRI idIRI = SimpleValueFactory.getInstance().createIRI(URLConstants.ID + linkedComponent.getId());
        return containsSimpleNodeForComponent(idIRI);
    }

    /**
     * Gets an iterable for all {@link OGMembershipStatement}s in this dataset
     * @return The iterable
     */
    public Iterable<OGMembershipStatement> getMembershipStatements() {
        return memStatements;
    }

    /**
     * Gets an iterable for all {@link OGRelationshipStatement}s in this dataset
     * @return The iterable
     */
    public Iterable<OGRelationshipStatement> getRelationshipStatements() {
        return relStatements;
    }

    /**
     * Gets an iterable for all {@link OGPropertyStatement}s in this dataset
     * @return The iterable
     */
    public Iterable<OGPropertyStatement> getPropertyStatements() {
        return propStatements;
    }

    /**
     * Gets an iterable for all {@link OGSimpleNode}s in this dataset
     * @return The iterable
     */
    public Iterable<OGSimpleNode<?>> getSimpleNodes() {
        return Iterables.concat(iriToSimpleNode.values(),
                bNodeToSimpleNode.values());
    }

    /**
     * Gets an iterable for all {@link OGGraph}s in this dataset
     * @return The iterable
     */
    public Iterable<OGGraph<?>> getGraphs() {
        return graphNameToGraph.values();
    }

    /**
     * Returns {@code true} if this dataset is empty, {@code false} otherwise.
     * @return {@code true} if this dataset is empty, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return this.memStatements.isEmpty()
                && this.propStatements.isEmpty()
                && this.relStatements.isEmpty()
                && this.bNodeToSimpleNode.isEmpty()
                && this.iriToSimpleNode.isEmpty()
                && this.graphNameToGraph.isEmpty();
    }

    /**
     * Either create a new {@link OGSimpleNode} in the dataset from given {@code linkedComponent},
     * or retrieve existing {@link OGSimpleNode} that has the same {@code linkedComponent} from the dataset.
     * @param linkedComponent The {@link IRI} linked component.
     * @return Newly created or obtained simple node.
     */
    public OGSimpleNodeIRI createOrObtainSimpleNode(@NonNull IRI linkedComponent) {
        return this.iriToSimpleNode.computeIfAbsent(linkedComponent, iri -> new OGSimpleNodeIRI(iri, getUniqueID()));
    }

    /**
     * Either create a new {@link OGSimpleNode} in the dataset from given {@code linkedComponent},
     * or retrieve existing {@link OGSimpleNode} that has the same {@code linkedComponent} from the dataset.
     * @param vertex The {@link LPGVertex} vertex to create or obtain the simple node from.
     * @return Newly created or obtained simple node.
     */
    public OGSimpleNodeIRI createOrObtainSimpleNode(@NonNull LPGVertex vertex) {
        // Create a vertex ID IRI from the given vertex.
        IRI idIRI = SimpleValueFactory.getInstance().createIRI(URLConstants.ID + vertex.getId());
        return createOrObtainSimpleNode(idIRI);
    }

    /**
     * Either create a new {@link OGSimpleNode} in the dataset from given {@code linkedComponent},
     * or retrieve existing {@link OGSimpleNode} that has the same {@code linkedComponent} from the dataset.
     * @param linkedComponent The {@link BNode} linked component.
     * @return Newly created or obtained simple node.
     */
    public OGSimpleNodeBNode createOrObtainSimpleNode(@NonNull BNode linkedComponent) {
        return this.bNodeToSimpleNode.computeIfAbsent(linkedComponent, bnode -> new OGSimpleNodeBNode(bnode, getUniqueID()));
    }

    /**
     * Adds the simple node to the dataset, assigns the {@link OGSimpleNode#setLocalId(long)}.
     * @param sn The simple node to add.
     */
    public void addSimpleNode(@NonNull OGSimpleNodeBNode sn) {
        this.bNodeToSimpleNode.put(sn.linkedComponent, sn);
        sn.setLocalId(getUniqueID());
    }

    /**
     * Adds the simple node to the dataset, assigns the {@link OGSimpleNode#setLocalId(long)}.
     * @param sn The simple node to add.
     */
    public void addSimpleNode(@NonNull OGSimpleNodeIRI sn) {
        this.iriToSimpleNode.put(sn.linkedComponent, sn);
        sn.setLocalId(getUniqueID());
    }

    /**
     * Gets all simple nodes that have a namespace of: {@link URLConstants#ID}.
     * @return All simple nodes in this data set with the ID namespace.
     */
    public Iterable<OGSimpleNodeIRI> getVertexSimpleNodes() {
        String idNameSpace = URLConstants.ID;
        return this.iriToSimpleNode.values().stream().filter(
                sn -> sn.linkedComponent.getNamespace().equals(idNameSpace)
        ).collect(Collectors.toList());
    }

    private long getUniqueID() {
        idIncrementer++;
        return idIncrementer;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("\n---- Relationship statements ----\n");
        for (OGRelationshipStatement stat : relStatements) {
            str.append(stat.toString()).append("\n");
        }
        str.append("\n---- Property statements ----\n");
        for (OGPropertyStatement stat : propStatements) {
            str.append(stat.toString()).append("\n");
        }
        str.append("\n---- Membership statements ----\n");
        for (OGMembershipStatement stat : memStatements) {
            str.append(stat.toString()).append("\n");
        }
        str.append("\n---- Graphs ----\n");
        for (OGGraph<?> graph : graphNameToGraph.values()) {
            str.append(graph.toString()).append("\n");
        }
        str.append("\n---- Simple nodes ----\n");
        for (OGSimpleNode<?> sn : getSimpleNodes()) {
            str.append(sn.toString()).append("\n");
        }
        return str.toString();
    }
}
