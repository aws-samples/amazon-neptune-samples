package software.amazon.neptune.onegraph.playground.server.mapping;

import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import lombok.NonNull;

/**
 * Performs conversions between {@link LPGGraph}s and {@link OGDataset}s in both directions.
 */
public class LPGMapper {

    /**
     * The OneGraph dataset this mapper is currently modifying.
     */
    public final OGDataset dataset;

    /**
     *  The mapping configuration
     */
    public final LPGMappingConfiguration configuration;

    /**
     * Create a mapper with an existing dataset and existing configuration.
     * @param dataset An existing OneGraph dataset.
     * @param configuration The mapping configuration.
     */
    public LPGMapper(@NonNull OGDataset dataset,
                     @NonNull LPGMappingConfiguration configuration) {
        this.dataset = dataset;
        this.configuration = configuration;
    }

    /**
     * Create a mapper with a new dataset and existing configuration.
     * @param configuration The mapping configuration.
     */
    public LPGMapper(@NonNull LPGMappingConfiguration configuration) {
        this.dataset = new OGDataset();
        this.configuration = configuration;
    }

    /**
     * Adds the given labeled property graph to the current OneGraph dataset
     * @param graph The labeled property graph.
     */
    public void addLPGToOGDataset(@NonNull LPGGraph graph) {
        LPGToOG mapper = new LPGToOG(dataset, configuration);
        mapper.addLPGToOGDataset(graph);
    }

    /**
     * Creates a labeled property graph from the current OneGraph dataset
     * @return The labeled property graph created.
     */
    public LPGGraph createLPGFromOGDataset() {
        OGToLPG mapper = new OGToLPG(dataset, configuration);
        return mapper.createLPGFromOGDataset();
    }
}
