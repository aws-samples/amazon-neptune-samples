package software.amazon.neptune.onegraph.playground.server.io.serializing;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.rdf4j.model.Literal;
import software.amazon.neptune.onegraph.playground.server.constants.URLConstants;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGReifiableElement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;
import com.google.common.collect.Lists;
import lombok.NonNull;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Serializes {@link OGDataset}s to file.
 */
public class OGSerializer {

    /**
     * Default predicate to use for relationship statements when their predicates are of type {@link String}.
     */
    public final IRI defaultRelPredicate;

    /**
     * Default predicate to use for property statements when their predicates are of type {@link String}.
     */
    public final IRI defaultPropPredicate;

    // Sid incrementer
    private int sidCounter = 0;

    // Token for indicating graph summation.
    private final String IN_TOKEN = "in";

    // Token for indicating no graph membership.
    private final String U_FLAG_TOKEN = "/ U";

    // Token for indicating statement ID.
    private final String SID_TOKEN = "__:SID";

    // Single space separator
    private final String SPACE = " ";
    
    /**
     * Create a new OGSerializer with a base IRI for predicates of {@link URLConstants}.{@code PREDICATE}
     */
    public OGSerializer() {
        this.defaultRelPredicate = SimpleValueFactory.getInstance().createIRI(URLConstants.EDGE);
        this.defaultPropPredicate = SimpleValueFactory.getInstance().createIRI(URLConstants.PROPERTY_NAME);
    }

    /**
     * Serializes the given {@code dataset} to the OG format.
     * @param output The output stream to write to.
     * @param dataset The dataset to serialize.
     * @return A mapping of triple statements to the line number on which they were written.
     */
    public Map<OGTripleStatement, Integer> serializeToOG(@NonNull OutputStream output,
                                                         @NonNull OGDataset dataset) {
        PrintWriter pw = new PrintWriter(output);
        final Map<OGTripleStatement, Integer> statementToLine = new HashMap<>();

        final Map<OGReifiableElement, String> elemToSID = new HashMap<>();

        for (OGRelationshipStatement statement : dataset.getRelationshipStatements()) {
            sidCounter++;

            writeSubject(pw, statement, elemToSID);
            writePredicate(pw, statement, this.defaultRelPredicate);
            writeObject(statement, pw, elemToSID);
            writeSid(pw, statement, elemToSID);
            writeGraphs(pw, statement, dataset);
            writeEOL(pw);

            statementToLine.put(statement, sidCounter);
        }
        for (OGPropertyStatement statement : dataset.getPropertyStatements()) {
            sidCounter++;

            writeSubject(pw, statement, elemToSID);
            writePredicate(pw, statement, this.defaultPropPredicate);
            writeObject(statement, pw);
            writeSid(pw, statement, elemToSID);
            writeGraphs(pw, statement, dataset);
            writeEOL(pw);

            statementToLine.put(statement, sidCounter);
        }
        return statementToLine;
    }

    private void writeSubject(PrintWriter pw,
                                          OGTripleStatement statement,
                                          Map<OGReifiableElement, String> elemToSID) {
        writeReifiableElement(statement.getSubject(), pw, elemToSID);
    }

    private void writePredicate(PrintWriter pw,
                                          OGTripleStatement statement,
                                          IRI defaultPredicate) {
        // Take the underlying IRI from the predicate or if there was none, convert the underlying String to an IRI,
        // By using the defaultPredicate.
        IRI predicate = statement.getPredicate().iriPredicate().orElseGet(() -> {
            Optional<String> stringPredicate = statement.getPredicate().stringPredicate();
            if (stringPredicate.isPresent()) {
                return SimpleValueFactory.getInstance().createIRI(defaultPredicate.getNamespace(), stringPredicate.get());
            }
            return defaultPredicate;
        });
        writeIRI(pw, predicate);
        pw.write(SPACE);
    }
    private void writeObject(OGRelationshipStatement statement,
                             PrintWriter pw,
                             Map<OGReifiableElement, String> elemToSID) {
        writeReifiableElement(statement.object, pw, elemToSID);
    }

    private void writeObject(OGPropertyStatement statement,
                             PrintWriter pw) {
        // The label(lexical-form) of the literal needs to be escaped before writing, this ensures things like
        // \n are written out literally instead of a new line.
        pw.write(escapeLiteral(statement.object.literalValue()).toString() + SPACE);
    }

    // Escapes characters in the lexical form of the given literal.
    private Literal escapeLiteral(Literal literal) {
        Optional<String> literalLang = literal.getLanguage();
        String label = StringEscapeUtils.escapeJava(literal.getLabel());
        if (literalLang.isPresent()) {
            return SimpleValueFactory.getInstance().createLiteral(label, literalLang.get());
        } else {
            return SimpleValueFactory.getInstance().createLiteral(label, literal.getDatatype());
        }
    }
    private void writeSid(PrintWriter pw,
                          OGTripleStatement statement,
                          Map<OGReifiableElement, String> elemToSID) {
        String sid = sidForElement(statement, elemToSID);
        pw.write(sid + SPACE);
    }

    private void writeEOL(PrintWriter pw) {
        pw.println(".");
        pw.flush();
    }

    private void writeIRI(PrintWriter pw, IRI iri) {
        pw.write("<" + iri.toString() + ">");
    }

    // Writes the graphs the statement is in.
    private void writeGraphs(PrintWriter pw,
                             OGTripleStatement statement,
                             OGDataset dataset) {
        Optional<Set<OGGraph<?>>> graphs = dataset.graphsForStatement(statement);
        // Check if the statement is even asserted
        if (graphs.isPresent()) {
            // Do not write the "in" keyword when the statement is merely in the default graph.
            if (!(graphs.get().size() == 1 &&
                    Lists.newArrayList(graphs.get()).get(0).graphName == OGGraph.DEFAULT_GRAPH_IRI)) {
                pw.write(IN_TOKEN);
            }
            boolean addComma = false;
            for (OGGraph<?> g : graphs.get()) {
                if (g.graphName.equals(OGGraph.DEFAULT_GRAPH_IRI)) {
                    continue;
                }
                if (addComma) {
                    pw.write(", ");
                } else {
                    pw.write(SPACE);
                }
                addComma = true;

                // Writing the graphs
                if (g.graphName instanceof IRI) {
                    writeIRI(pw, (IRI) g.graphName);
                } else {
                    pw.write(g.graphName.toString());
                }
            }
            pw.write(SPACE);
        } else {
            pw.write(U_FLAG_TOKEN + SPACE);
        }
    }

    // Retrieves or creates a sid for the given element.
    private String sidForElement(OGReifiableElement elem, Map<OGReifiableElement, String> elemToSID) {
        return elemToSID.computeIfAbsent(elem, k -> SID_TOKEN + sidCounter);
    }

    // Writes out a reifiable element this will end up either writing a RDF Resource or a SID.
    private void writeReifiableElement(OGReifiableElement elem,
                                       PrintWriter pw,
                                      Map<OGReifiableElement, String> elemToSID) {
        Optional<IRI> elementIRI = elem.convertToIRI();
        if (elementIRI.isPresent()) {
            writeIRI(pw, elementIRI.get());
            pw.write(SPACE);
            return;
        }
        
        Optional<Resource> elementResource = elem.convertToResource();
        if (elementResource.isPresent()) {
            pw.write(elementResource.get() + SPACE);
            return;
        }

        String sid = sidForElement(elem, elemToSID);
        if (sid != null) {
            pw.write(sid + SPACE);
        }
    }
}
