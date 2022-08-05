package software.amazon.neptune.onegraph.playground.server.constants;

/**
 * Class containing URLs constants used to create IRIs.
 */
public final class URLConstants {

    /**
     * Base URL for IRIs, used for mapping from LPG property names and labels to RDF.
     */
    public static final String BASE = "http://aws.amazon.com/neptune/ogplayground/data";

    /**
     * Default URL used for LPG node labels to RDF IRIs.
     */
    public static final String DEFAULT_NODE_LABEL = BASE + "/label/";

    /**
     * Default URL used for LPG edge labels to RDF IRIs.
     */
    public static final String EDGE = BASE + "/edge/";

    /**
     * Default URL used for vertex IDs.
     */
    public static final String ID = BASE + "/id/";

    /**
     * Default URL used for LPG property names to RDF IRIs.
     */
    public static final String PROPERTY_NAME = BASE + "/propertyName/";

    /**
     * The default graph, when statements are translated over from RDF Models to OneGraph, statements having
     * the {@code null} context/graph will be put in this graph.
     */
    public static final String DEFAULT_GRAPH = "http://aws.amazon.com/neptune/vocab/v01/DefaultNamedGraph";
}
