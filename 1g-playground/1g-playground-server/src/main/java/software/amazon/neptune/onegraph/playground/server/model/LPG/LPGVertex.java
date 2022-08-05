package software.amazon.neptune.onegraph.playground.server.model.LPG;

import lombok.NonNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Implementation of the formal definition of a property graph vertex.
 */
public class LPGVertex extends LPGElement  {

    /**
     * Vertices can have multiple labels, but labels can't be duplicate.
     */
    private final LinkedHashSet<String> labels = new LinkedHashSet<>();

    /**
     * Adds new labels to this vertex
     * @param labels The labels
     */
    public LPGVertex(@NonNull String... labels) {
        for (String label : labels) {
            this.addLabel(label);
        }
    }

    /**
     * Adds a new label to this vertex
     * @param label The label
     */
    public void addLabel(@NonNull String label) {
        this.labels.add(label);
    }

    /**
     * Returns the list of labels this vertex has, if no labels are specified for this vertex will return
     * 1 label with value "vertex"
     * @return The labels of this vertex.
     */
    public Collection<String> getLabels() {
        if (labels.size() == 0) {
            return Collections.singletonList("vertex");
        }
        return labels;
    }

    @Override
    public String toString() {
        return "(V" + this.getId() + " - labels: " + this.getLabels() + ")";
    }
}
