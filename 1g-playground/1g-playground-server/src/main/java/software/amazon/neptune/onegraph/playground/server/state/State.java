package software.amazon.neptune.onegraph.playground.server.state;

import lombok.NonNull;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import software.amazon.neptune.onegraph.playground.server.api.request.ConfigType;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMapper;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.mapping.RDFMapper;
import software.amazon.neptune.onegraph.playground.server.mapping.TinkerPopConverter;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import lombok.Getter;
import lombok.Setter;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * All state of the application is kept in this component.
 */
public class State {

    /**
     *  The current OneGraph data set used throughout the application.
     */
    @Getter
    private OGDataset ogDataset;

    /**
     *  A repository for the {@link #rdfModel}.
     */
    @Getter
    private final Repository rdfRepository;

    /**
     * The current RDF {@link Model} mapped from the {@link #ogDataset}.
     */
    @Getter
    private Model rdfModel;

    /**
     * The current LPG mapping configuration.
     */
    @Getter
    @Setter
    private LPGMappingConfiguration lpgMappingConfiguration;

    /**
     * The path the LPG mapping configuration was loaded from.
     */
    @Getter
    @Setter
    private String pathConfigurationLoadedFrom = null;

    /**
     * The type of the LPG mapping configuration.
     */
    @Getter
    @Setter
    private ConfigType configType = ConfigType.DEFAULT;

    /**
     * The current TinkerPop {@link Graph} mapped from the {@link #ogDataset}.
     */
    @Getter
    private Graph tinkerPopGraph;

    /**
     * The current LPG mapped from the {@link #ogDataset}.
     */
    @Getter
    private LPGGraph lpgGraph;

    /**
     * Time in ms before queries time out.
     *
     * default = 15000
     */
    @Getter
    @Setter
    private long queryTimeoutMillis = 15000;

    public State() {
        this.ogDataset = new OGDataset();

        this.lpgMappingConfiguration = LPGMappingConfiguration.defaultConfiguration();
        this.lpgMappingConfiguration.reversible = false;
        this.tinkerPopGraph = TinkerGraph.open();
        this.rdfRepository = new SailRepository(new MemoryStore());
        this.rdfRepository.init();
        this.lpgGraph = new LPGGraph();
        this.rdfModel = new LinkedHashModel();
    }

    /**
     * Reloads {@link #rdfModel}, {@link #rdfRepository}, {@link #lpgGraph} and {@link #tinkerPopGraph}.
     */
    public void reloadDerivativeData() {
        this.reloadDerivativeData(new StringBuilder());
    }

    /**
     * Reloads {@link #rdfModel}, {@link #rdfRepository}, {@link #lpgGraph} and {@link #tinkerPopGraph}.
     * @param statusBuilder Logs info/warnings encountered during the reloading of the derivative data models to this builder.
     */
    public void reloadDerivativeData(@NonNull StringBuilder statusBuilder) {
        RDFMapper rdfMapper = new RDFMapper(this.ogDataset);
        this.rdfModel = rdfMapper.createRDFModelFromOGDataset();

        try (RepositoryConnection con = rdfRepository.getConnection()) {
            con.clear();
            con.add(this.rdfModel);
        }
        LPGMapper lpgMapper = new LPGMapper(this.ogDataset, this.lpgMappingConfiguration);
        this.lpgGraph = lpgMapper.createLPGFromOGDataset();

        TinkerPopConverter converter = new TinkerPopConverter(new TinkerPopConverter.TinkerPopConverterDelegate() {
            @Override
            public void unsupportedValueFound(String infoString) {
                statusBuilder.append(infoString).append("\n");
            }

            @Override
            public void metaPropertyFound(String infoString) {
                statusBuilder.append(infoString).append("\n");
            }

            @Override
            public void multiLabeledVertexFound(String infoString) {
                statusBuilder.append(infoString).append("\n");
            }
        });
        this.tinkerPopGraph = converter.convertLPGToTinkerGraph(this.lpgGraph);
    }

    /**
     * Clears all data in the state.
     * @return {@code true} if there was data to clear, {@code false} otherwise.
     */
    public boolean clear() {
        boolean isEmpty = this.ogDataset.isEmpty();

        // Clear the dataset regardless if it was empty.
        this.ogDataset = new OGDataset();
        reloadDerivativeData();

        return !isEmpty;
    }
}
