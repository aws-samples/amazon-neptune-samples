package software.amazon.neptune.onegraph.playground.server.mapping;

import software.amazon.neptune.onegraph.playground.server.io.parsing.LPGParser;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGEdge;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGVertex;
import software.amazon.neptune.onegraph.playground.server.model.RDF.LiteralConverter;
import lombok.NonNull;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Performs conversion from NeptuneCSV-RDF-Models created through {@link LPGParser} to {@link LPGGraph}.
 */
public class NeptuneCSVConverter {

    /**
     * Converts an RDF4J {@link Model} parsed by {@link LPGParser#parseNeptuneCSV(Path)} to an LPGGraph
     * @param nodes The nodes model object returned by {@link LPGParser#parseNeptuneCSV(Path)}
     * @param edges The edges model object returned by {@link LPGParser#parseNeptuneCSV(Path)}
     * @return A labeled property graph
     */
    public static LPGGraph convertNeptuneCSVtoLPG(@NonNull Model nodes, @NonNull Model edges) throws ConverterException {
        LPGGraph graph = new LPGGraph();
        Map<Resource, LPGVertex> rtn = addNodesToGraph(graph, nodes);
        addEdgesToGraph(graph, edges, rtn);
        return graph;
    }

    private static Map<Resource, LPGVertex> addNodesToGraph(LPGGraph graph, Model nodes) throws ConverterException {
        Map<Resource, LPGVertex> resourceToNode = new HashMap<>();
        // When this IRI is in predicate position of a Statement we know that the
        // statement is about the label of the vertex
        IRI labelPred = LPGParser.NEPTUNECSV_PREDICATE_LABEL_IRI;
        for (Statement nodeStat : nodes) {
            LPGVertex vertex = resourceToNode.computeIfAbsent(nodeStat.getSubject(), s -> {
                LPGVertex v = new LPGVertex();
                if (s instanceof IRI) {
                    v.setId(((IRI) s).getLocalName());
                }
                return v;
            });

            if (nodeStat.getPredicate().equals(labelPred) && nodeStat.getObject() instanceof IRI) {
                IRI objIRI = (IRI) nodeStat.getObject();
                // This statement is about a vertex label
                vertex.addLabel(objIRI.getLocalName());
            } else if (nodeStat.getObject() instanceof Literal){
                Literal objLit = (Literal) nodeStat.getObject();
                // This statement is about a vertex property
                Object value = LiteralConverter.convertToObject(objLit);
                vertex.addPropertyValue(nodeStat.getPredicate().getLocalName(), value);
            } else {
                throw new ConverterException(String.format("Unexpected object encountered %s", nodeStat.getObject()));
            }
            graph.addVertex(vertex);
        }
        return resourceToNode;
    }

    private static void addEdgesToGraph(LPGGraph graph, Model edges, Map<Resource, LPGVertex> rtn) throws ConverterException {
        Map<Resource, LPGEdge> statementCTXToEdge = new HashMap<>();

        // The given Model contains statements about edges and statements about edge properties.
        // handle statements about edge properties later, because first the edges need to be added.
        LinkedList<Statement> statementsAboutEdgeProperties = new LinkedList<>();

        for (Statement statement : edges) {
            // If the statement does not have a default context then we know this is an edge.
            // other statements are edge properties.
            if (!statement.getContext().toString().equals(LPGParser.NEPTUNECSV_DEFAULT_CTX)) {
                if (statement.getSubject() instanceof IRI &&
                        statement.getObject() instanceof IRI) {
                    IRI sub  = (IRI) statement.getSubject();
                    IRI pred = statement.getPredicate();
                    IRI obj  = (IRI) statement.getObject();

                    // If there are edges that are tied to vertices that are not yet created from the nodes file, then
                    // create them implicitly.
                    LPGVertex outV = rtn.computeIfAbsent(sub, s -> {
                        LPGVertex v = new LPGVertex();
                        v.setId(((IRI) s).getLocalName());
                        return v;
                    });
                    LPGVertex inV = rtn.computeIfAbsent(obj, o -> {
                        LPGVertex v = new LPGVertex();
                        v.setId(((IRI) o).getLocalName());
                        return v;
                    });

                    LPGEdge e = new LPGEdge(outV, inV, pred.getLocalName());
                    if (statement.getContext() instanceof IRI) {
                        // The context of the statement contains the edge ID.
                        e.setId(((IRI) statement.getContext()).getLocalName());
                    } else {
                        throw new ConverterException("Contexts of statements in the \"edges\" Model must be IRIs");
                    }
                    statementCTXToEdge.put(statement.getContext(), e);
                    graph.addEdge(e);
                }
            } else {
                // Process later
                statementsAboutEdgeProperties.add(statement);
            }
        }

        for (Statement statement : statementsAboutEdgeProperties) {
            LPGEdge edge = statementCTXToEdge.get(statement.getSubject());
            if (edge == null) {
                throw new ConverterException(String.format("The statement %s references an edge with an id %s that is not in the data.",
                        statement, statement.getContext()));
            }
            if (statement.getObject() instanceof Literal) {
                Literal objLit = (Literal) statement.getObject();
                // This statement is about an edge property
                Object value = LiteralConverter.convertToObject(objLit);
                edge.addPropertyValue(statement.getPredicate().getLocalName(), value);
            } else {
                throw new ConverterException(String.format("The statement %s acts as an edge property but does not have a literal object.",
                        statement));
            }
        }
    }
}
