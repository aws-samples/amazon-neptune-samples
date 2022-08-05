package software.amazon.neptune.onegraph.playground.server.mapping;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import java.util.Optional;

/**
 * Holds configuration for the mapping between LPG and OneGraph
 * A configuration consists of 3 mappings and 1 property:
 * - node label mapping
 *      name to IRI x (IRI union String)
 * - edge label mapping:
 *      name to (IRI union String)
 * - property name mapping:
 *      name to (IRI union String)
 * - Default node label:
 *      IRI
 * See "Semantics-Aware LPG-to-1G Mapping"
 */
public class LPGMappingConfiguration {

    /**
     * When set to {@code false} then <br />
     * {@link #propertyNameMappingInverse(IRI)} <br />
     * {@link #edgeLabelMappingInverse(IRI)} <br />
     * {@link #nodeLabelMappingInverse(IRI, IRI)} <br />
     * will always return empty optionals. <br />
     *
     * Default value {@code false}.
     */
    public boolean reversible = true;

    /** The IRI to use in predicate position of property statements about vertex labels, default value is RDF.TYPE */
    public IRI defaultNodeLabelPredicate = RDF.TYPE;

    private final BiMap<String, NLMEntry<?>> nodeLabelMapping = HashBiMap.create();
    private final BiMap<String, ELMorPNMEntry<?>> edgeLabelMapping = HashBiMap.create();
    private final BiMap<String, ELMorPNMEntry<?>> propertyNameMapping = HashBiMap.create();

    public static LPGMappingConfiguration defaultConfiguration() {
        return new LPGDefaultMappingConfiguration();
    }

    /**
     * Returns the associated node label mapping entry for the given {@code label} or an empty optional if
     * no entry was found.
     * @param label Input of the mapping
     * @return The appropriate node label mapping entry or an empty optional if there was none found.
     */
    public Optional<NLMEntry<?>> nodeLabelMapping(@NonNull String label) {
        return Optional.ofNullable(this.nodeLabelMapping.get(label));
    }

    /**
     * Returns the associated label of the given {@code predicate} and {@code object} or an empty optional if
     * no entry was found. This is the inverse of {@link #nodeLabelMapping}. When {@link LPGMappingConfiguration#reversible} is set to
     * {@code false}, this method will always return an empty optional.
     * @param predicate The predicate of this node label mapping entry.
     * @param object The object of this node label mapping entry.
     * @return The appropriate label or an empty optional if there was none found.
     */
    public Optional<String> nodeLabelMappingInverse(@NonNull IRI predicate,
                                                    @NonNull  IRI object) {
        NLMEntry<?> request = new NLMEntryIRI(predicate, object);
        return Optional.ofNullable(this.nodeLabelMapping.inverse().get(request));
    }

    /**
     * Returns the associated label of the given {@code predicate} and {@code object} or an empty optional if
     * no entry was found. This is the inverse of {@link #nodeLabelMapping}. When {@link LPGMappingConfiguration#reversible} is set to
     * {@code false}, this method will always return an empty optional.
     * @param predicate The predicate of this node label mapping entry.
     * @param object The object of this node label mapping entry.
     * @return The appropriate label or an empty optional if there was none found.
     */
    public Optional<String> nodeLabelMappingInverse(@NonNull IRI predicate,
                                                    @NonNull String object) {
        NLMEntry<?> request = new NLMEntryString(predicate, object);
        return Optional.ofNullable(this.nodeLabelMapping.inverse().get(request));
    }

    /**
     * Returns the associated node edge mapping entry for the given {@code label} or an empty optional if
     * no entry was found.
     * @param label Input of the mapping
     * @return The appropriate edge label mapping entry or an empty optional if there was none found.
     */
    public Optional<ELMorPNMEntry<?>> edgeLabelMapping(@NonNull String label) {
        return Optional.ofNullable(this.edgeLabelMapping.get(label));
    }

    /**
     * Returns the associated label of the given {@code predicate} or an empty optional if
     * no entry was found. This is the inverse of {@link #edgeLabelMapping}. When {@link LPGMappingConfiguration#reversible} is set to
     * {@code false}, this method will always return an empty optional.
     * @param predicate The predicate of this edge label mapping entry.
     * @return The appropriate label or an empty optional if there was none found.
     */
    public Optional<String> edgeLabelMappingInverse(@NonNull IRI predicate) {
        ELMorPNMEntry<?> request = new ELMorPNMEntryIRI(predicate);
        return Optional.ofNullable(this.edgeLabelMapping.inverse().get(request));
    }

    /**
     * Returns the associated label of the given {@code predicate} or an empty optional if
     * no entry was found. This is the inverse of {@link #edgeLabelMapping}. When {@link LPGMappingConfiguration#reversible} is set to
     * {@code false}, this method will always return an empty optional.
     * @param predicate The predicate of this edge label mapping entry.
     * @return The appropriate label or an empty optional if there was none found.
     */
    public Optional<String> edgeLabelMappingInverse(@NonNull String predicate) {
        ELMorPNMEntry<?> request = new ELMorPNMEntryString(predicate);
        return Optional.ofNullable(this.edgeLabelMapping.inverse().get(request));
    }

    /**
     * Returns the associated property name mapping entry for the given {@code pName} or an empty optional if
     * no entry was found.
     * @param pName Input of the mapping
     * @return The appropriate property name mapping entry or an empty optional if there was none found.
     */
    public Optional<ELMorPNMEntry<?>> propertyNameMapping(@NonNull String pName) {
        return Optional.ofNullable(this.propertyNameMapping.get(pName));
    }

    /**
     * Returns the associated label of the given {@code predicate} or an empty optional if
     * no entry was found. This is the inverse of {@link #propertyNameMapping}. When {@link LPGMappingConfiguration#reversible} is set to
     * {@code false}, this method will always return an empty optional.
     * @param predicate The predicate of this property name mapping entry.
     * @return The appropriate label or an empty optional if there was none found.
     */
    public Optional<String> propertyNameMappingInverse(@NonNull IRI predicate) {
        ELMorPNMEntry<?> request = new ELMorPNMEntryIRI(predicate);
        return Optional.ofNullable(this.propertyNameMapping.inverse().get(request));
    }

    /**
     * Returns the associated label of the given {@code predicate} or an empty optional if
     * no entry was found. This is the inverse of {@link #propertyNameMapping}. When {@link LPGMappingConfiguration#reversible} is set to
     * {@code false}, this method will always return an empty optional.
     * @param predicate The predicate of this property name mapping entry.
     * @return The appropriate label or an empty optional if there was none found.
     */
    public Optional<String> propertyNameMappingInverse(@NonNull String predicate) {
        ELMorPNMEntry<?> request = new ELMorPNMEntryString(predicate);
        return Optional.ofNullable(this.propertyNameMapping.inverse().get(request));
    }

    /**
     * Adds a new {@link NLMEntry} to this configuration using the given parameters.
     * @param label The input of this mapping entry.
     * @param predicate The first output of this mapping entry.
     * @param object The second output of this mapping entry.
     */
    public void addToNodeLabelMapping(@NonNull String label,
                                      @NonNull IRI predicate,
                                      @NonNull String object) {
        NLMEntry<?> entry;
        // Try to create an iri out of the object.
        try {
            IRI i = SimpleValueFactory.getInstance().createIRI(object);
            entry = new NLMEntryIRI(predicate, i);
        } catch (IllegalArgumentException e) {
            entry = new NLMEntryString(predicate, object);
        }
        this.nodeLabelMapping.put(label, entry);
    }

    /**
     * Adds a new {@link ELMorPNMEntry} to this configuration using the given parameters.
     * @param label The input of this mapping entry.
     * @param predicate The first output of this mapping entry.
     */
    public void addToEdgeLabelMapping(@NonNull String label,
                                      @NonNull String predicate) {
        ELMorPNMEntry<?> entry;
        // Try to create an iri out of the predicate.
        try {
            IRI i = SimpleValueFactory.getInstance().createIRI(predicate);
            entry = new ELMorPNMEntryIRI(i);
        } catch (IllegalArgumentException e) {
            entry = new ELMorPNMEntryString(predicate);
        }
        this.edgeLabelMapping.put(label, entry);
    }

    /**
     * Adds a new {@link ELMorPNMEntry} to this configuration using the given parameters.
     * @param label The input of this mapping entry.
     * @param predicate The first output of this mapping entry.
     */
    public void addToPropertyNameMapping(@NonNull String label,
                                         @NonNull String predicate) {

        ELMorPNMEntry<?> entry;
        // Try to create an iri out of the predicate.
        try {
            IRI i = SimpleValueFactory.getInstance().createIRI(predicate);
            entry = new ELMorPNMEntryIRI(i);
        } catch (IllegalArgumentException e) {
            entry = new ELMorPNMEntryString(predicate);
        }
        this.propertyNameMapping.put(label, entry);
    }

    /*
    * Interface to be the delegate of a mapping entry. The respective handling method is called
    * depending on if the entry contains an IRI or String.
    */
    interface MappingEntryDelegate {

        /**
         * Called when the entry contains an IRI
         * @param i The IRI
         */
        void handleIRI(IRI i);

        /**
         * Called when the entry contains a String
         * @param s The String
         */
        void handleString(String s);
    }

    public interface MappingEntry {

        /**
         * Unpacks this mapping entry and either calls {@code handleIRI} or {@code handleString} with its underlying value on
         * the given delegate.
         * @param delegate The delegate to callback on.
         */
        void unpack(MappingEntryDelegate delegate);
    }

    /**
     * A node label mapping entry, it has a {@code predicate} and {@code object}
     * @param <P> Either an {@link IRI} or {@link String}
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    public abstract static class NLMEntry<P> implements MappingEntry {
        public final IRI predicate;
        public final P object;
    }

    /**
     * Subclass of {@link NLMEntry} with {@link IRI} type.
     */
    public static class NLMEntryIRI extends NLMEntry<IRI> {

        public NLMEntryIRI(@NonNull IRI predicate,
                           @NonNull  IRI object) {
            super(predicate, object);
        }

        @Override
        public void unpack(@NonNull MappingEntryDelegate delegate) {
            delegate.handleIRI(object);
        }
    }

    /**
     * Subclass of {@link NLMEntry} with {@link String} type.
     */
    public static class NLMEntryString extends NLMEntry<String> {

        public NLMEntryString(@NonNull IRI predicate,
                              @NonNull  String object) {
            super(predicate, object);
        }

        @Override
        public void unpack(@NonNull MappingEntryDelegate delegate) {
            delegate.handleString(object);
        }
    }

    /**
     * An edge label mapping entry, it has a {@code predicate}
     * @param <P> Either an {@link IRI} or {@link String}
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    public abstract static class ELMorPNMEntry<P> implements MappingEntry {
        public final P predicate;
    }

    /**
     * Subclass of {@link ELMorPNMEntry} with {@link IRI} type.
     */
    public static class ELMorPNMEntryIRI extends ELMorPNMEntry<IRI> {
        public ELMorPNMEntryIRI(@NonNull IRI predicate) {
            super(predicate);
        }

        @Override
        public void unpack(@NonNull MappingEntryDelegate delegate) {
            delegate.handleIRI(predicate);
        }
    }

    /**
     * Subclass of {@link ELMorPNMEntry} with {@link String} type.
     */
    public static class ELMorPNMEntryString extends ELMorPNMEntry<String> {
        public ELMorPNMEntryString(@NonNull String predicate) {
            super(predicate);
        }

        @Override
        public void unpack(@NonNull MappingEntryDelegate delegate) {
            delegate.handleString(predicate);
        }
    }
}
