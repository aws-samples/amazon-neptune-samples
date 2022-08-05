package software.amazon.neptune.onegraph.playground.server.io.parsing;

import software.amazon.neptune.onegraph.playground.server.mapping.LPGMappingConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Parser LPG mapping configuration files and creates {@link LPGMappingConfiguration} objects.
 * The configuration file is in JSON format and has structure:
 * {
 *     "default_node_label" : "IRI",
 *     "node_label_mapping" : {
 *         "entry1" : ["IRI","IRI_or_STRING"],
 *         "entry2" : ["IRI","IRI_or_STRING"],
 *         ...
 *     },
 *     "edge_label_mapping" : {
 *         "entry1" : "IRI_or_STRING",
 *         "entry2" : "IRI_or_STRING",
 *         ...
 *     },
 *     "property_name_mapping" : {
 *          "entry1" : "IRI_or_STRING",
 *          "entry2" : "IRI_or_STRING",
 *          ...
 *     }
 * }
 */
public class ConfigurationParser {

    /**
     * Parses a labeled property map configuration file.
     * @param path The path to the configuration file.
     * @param lpgConfig The LPG mapping configuration the contents of the file will be added to.
     * @throws ParserException if an error occurs during the process
     */
    public static void parseConfiguration(Path path, LPGMappingConfiguration lpgConfig) throws ParserException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Configuration c = mapper.readValue(path.toFile(), Configuration.class);
            c.addSelfToLPGMappingConfiguration(lpgConfig);
        } catch (IOException e) {
            if (e.getCause() != null) {
                throw new ParserException("Parsing configuration file at " + path + " failed", e.getCause());
            }
            throw new ParserException("Parsing configuration file at " + path + " failed", e);
        }
    }

    /**
     * Holds the JSON.
     */
    private static class Configuration {

        private String default_node_label;

        @JsonIgnore
        private IRI default_node_label_IRI;

        @JsonIgnore
        private final Map <String, IRI> nlmPredicateForKey = new HashMap<>();

        @JsonIgnore
        private final Map <String, String> nlmObjectForKey = new HashMap<>();

        private Map <String, String[]> node_label_mapping;

        @Setter
        private Map <String, String> edge_label_mapping;

        @Setter
        private Map <String, String> property_name_mapping;

        public void setDefault_node_label(String default_node_label) throws IllegalArgumentException {
            this.default_node_label = default_node_label;
            this.default_node_label_IRI = SimpleValueFactory.getInstance().createIRI(default_node_label);
        }

        public void setNode_label_mapping(Map <String, String[]> node_label_mapping) throws IllegalArgumentException {
            this.node_label_mapping = node_label_mapping;
            for (String key : node_label_mapping.keySet()) {
                String[] value = node_label_mapping.get(key);
                if (value.length != 2) {
                    throw new IllegalArgumentException("node_label_mapping array entries should be of size 2.");
                }
                nlmPredicateForKey.put(key, SimpleValueFactory.getInstance().createIRI(value[0]));
                nlmObjectForKey.put(key, value[1]);
            }
        }

        /**
         * Add this object to the mapping configuration.
         * @param config The mapping configuration to add the values of this {@link Configuration} to.
         */
        public void addSelfToLPGMappingConfiguration(LPGMappingConfiguration config) {
            if (this.default_node_label_IRI != null) {
                config.defaultNodeLabelPredicate = this.default_node_label_IRI;
            }
            // nlmPredicateForKey and nlmObjectForKey will have the same keys.
            for (Entry<String, IRI> entry : this.nlmPredicateForKey.entrySet()) {
                String object = this.nlmObjectForKey.get(entry.getKey());
                config.addToNodeLabelMapping(entry.getKey(), entry.getValue(), object);
            }
            for (Entry<String, String> entry : this.edge_label_mapping.entrySet()) {
                config.addToEdgeLabelMapping(entry.getKey(), entry.getValue());
            }
            for (Entry<String, String> entry : this.property_name_mapping.entrySet()) {
                config.addToPropertyNameMapping(entry.getKey(), entry.getValue());
            }
        }
    }
}
