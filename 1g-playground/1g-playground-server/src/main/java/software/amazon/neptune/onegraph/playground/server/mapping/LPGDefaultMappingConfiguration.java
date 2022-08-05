package software.amazon.neptune.onegraph.playground.server.mapping;

import lombok.NonNull;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;

import java.util.Optional;

class LPGDefaultMappingConfiguration extends LPGMappingConfiguration {

    @Override
    public Optional<NLMEntry<?>> nodeLabelMapping(@NonNull String label) {
        Optional<NLMEntry<?>> entry = super.nodeLabelMapping(label);
        if (!entry.isPresent()) {
            IRI object = SimpleValueFactory.getInstance().createIRI(URLConstants.DEFAULT_NODE_LABEL + label);
            NLMEntry<?> e = new NLMEntryIRI(RDF.TYPE, object);
            return Optional.of(e);
        }
        return entry;
    }

    @Override
    public Optional<ELMorPNMEntry<?>> edgeLabelMapping(@NonNull String label) {
        Optional<ELMorPNMEntry<?>> entry = super.edgeLabelMapping(label);
        if (!entry.isPresent()) {
            IRI edge = SimpleValueFactory.getInstance().createIRI(URLConstants.EDGE + label);
            ELMorPNMEntry<?> e = new ELMorPNMEntryIRI(edge);
            return Optional.of(e);
        }
        return entry;
    }

    @Override
    public Optional<ELMorPNMEntry<?>> propertyNameMapping(@NonNull String pName) {
        Optional<ELMorPNMEntry<?>> entry = super.propertyNameMapping(pName);
        if (!entry.isPresent()) {
            IRI pNameIRI = SimpleValueFactory.getInstance().createIRI(URLConstants.PROPERTY_NAME + pName);
            ELMorPNMEntry<?> e = new ELMorPNMEntryIRI(pNameIRI);
            return Optional.of(e);
        }
        return entry;
    }

    @Override
    public Optional<String> edgeLabelMappingInverse(@NonNull IRI predicate) {
        Optional<String> entry = super.edgeLabelMappingInverse(predicate);
        if (entry.isPresent()) {
            return entry;
        }
        // In a default mapping configuration the namespace of the predicate  is always EDGE
        if (predicate.getNamespace().equals(URLConstants.EDGE)) {
            return Optional.of(predicate.getLocalName());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> nodeLabelMappingInverse(@NonNull IRI predicate, @NonNull IRI object) {
        Optional<String> entry = super.nodeLabelMappingInverse(predicate, object);
        if (entry.isPresent()) {
            return entry;
        }
        // In a default mapping configuration the predicate = RDF.Type and the namespace of the object is always
        // DEFAULT_NODE_LABEL
        if (predicate.equals(RDF.TYPE) &&
                object.getNamespace().equals(URLConstants.DEFAULT_NODE_LABEL)) {
            return Optional.of(object.getLocalName());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> propertyNameMappingInverse(@NonNull IRI predicate) {
        Optional<String> entry = super.propertyNameMappingInverse(predicate);
        if (entry.isPresent()) {
            return entry;
        }
        // In a default mapping configuration the namespace of the predicate  is always PROPERTY_NAME
        if (predicate.getNamespace().equals(URLConstants.PROPERTY_NAME)) {
            return Optional.of(predicate.getLocalName());
        }
        return Optional.empty();
    }
}
