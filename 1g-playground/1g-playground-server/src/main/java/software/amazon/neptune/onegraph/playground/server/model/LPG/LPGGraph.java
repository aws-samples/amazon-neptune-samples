package software.amazon.neptune.onegraph.playground.server.model.LPG;

import lombok.NonNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the formal definition of a property graph.
 * A labeled property graph has a set of vertices and edges.
 */
public class LPGGraph {

    /**
     * The vertices of this labeled property graph.
     */
    public Set<LPGVertex> vertices = new HashSet<>();

    /**
     * The edges of this labeled property graph.
     */
    public Set<LPGEdge> edges = new HashSet<>();

    /**
     * Adds a new vertex to this graph, if the vertex already exists in the graph nothing happens.
     * @param v The vertex to add.
     */
    public void addVertex(@NonNull LPGVertex v) {
        vertices.add(v);
    }

    /**
     * Adds a new edge to this graph, if the edge already exists in the graph nothing happens.
     * @param e The edge to add.
     */
    public void addEdge(@NonNull LPGEdge e) {
        edges.add(e);
    }

    /**
     * Adds new vertices to this graph, if a vertex already exists in the graph nothing happens.
     * @param vertices The vertices to add.
     */
    public void addVertices(@NonNull LPGVertex... vertices) {
        for (LPGVertex v : vertices) {
            this.addVertex(v);
        }
    }

    /**
     * Adds new edges to this graph, if an edge already exists in the graph nothing happens.
     * @param edges The edges to add.
     */
    public void addEdges(@NonNull LPGEdge... edges) {
        for (LPGEdge e : edges) {
            this.addEdge(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("---- Vertices ----\n");
        for (LPGVertex v : vertices) {
            str.append(v.toString()).append("\n");
            str.append("properties: ").append(v.getProperties().toString()).append("\n");
            str.append("\n");
        }
        str.append("---- Edges ----\n");
        for (LPGEdge e : edges) {
            str.append(e.toString()).append("\n");
            str.append("properties: ").append(e.getProperties().toString()).append("\n");
            str.append("\n");
        }
        return str.toString();
    }
}
