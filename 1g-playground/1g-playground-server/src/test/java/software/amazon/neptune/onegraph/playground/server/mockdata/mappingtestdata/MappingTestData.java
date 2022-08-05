package software.amazon.neptune.onegraph.playground.server.mockdata.mappingtestdata;

import org.eclipse.rdf4j.model.Model;
import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import software.amazon.neptune.onegraph.playground.server.model.LPG.LPGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.mockdata.TestData;

/**
 * Contains a data model constructed to be a specific edge case to test the mappings.
 * The outputs produced by {@link #ogMappedToRDF()} and {@link #ogMappedToLPG()}
 * contain the RDF and LPG mappings of the underlying og model which is given by {@link #underlyingOG()}.
 */
public abstract class MappingTestData extends TestData {

    /**
     * Returns the RDF representation of underlying data model.
     * @return The RDF data {@link Model}.
     */
    public abstract Model ogMappedToRDF();

    /**
     * Returns the LPG representation of underlying data model.
     * @return The LPG graph {@link LPGGraph}.
     */
    public abstract LPGGraph ogMappedToLPG();

    /**
     * Returns the Mapping configuration used in the translation between OG and LPG.
     * @return The Mapping configuration used or the empty optional if no mapping configuration was used.
     */
    public abstract LPGMappingConfiguration mappingConfiguration();

    /**
     * Returns the underlying OG model.
     * @return The {@link OGDataset} that is the underlying model.
     */
    public abstract OGDataset underlyingOG();

    /**
     * The OG data set obtain when mapping the rdf model given by {@link #ogMappedToRDF()} back to OG.
     * @return The {@link OGDataset} obtained when mapping back.
     */
    public abstract OGDataset rdfMappedToOG();

    /**
     * The OG data set obtain when mapping the lpg model given by {@link #ogMappedToLPG()} back to OG.
     * @return The {@link OGDataset} obtained when mapping back.
     */
    public abstract OGDataset lpgMappedToOG();
}
