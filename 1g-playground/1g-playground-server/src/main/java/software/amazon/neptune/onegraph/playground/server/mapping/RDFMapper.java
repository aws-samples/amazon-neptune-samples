package software.amazon.neptune.onegraph.playground.server.mapping;

import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;
import lombok.NonNull;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * Performs conversions between RDF4J {@link Model}s and {@link OGDataset}s in both directions.
 */
public class RDFMapper {

    /**
     * The OneGraph dataset this mapper is currently modifying.
     */
    public final OGDataset dataset;

    /**
     * Creates the mapper using an existing OneGraph dataset
     * @param dataset An existing OneGraph dataset
     */
    public RDFMapper(@NonNull OGDataset dataset) {
        this.dataset = dataset;
    }

    /**
     * Creates the mapper, initializes an empty OneGraph dataset
     */
    public RDFMapper() {
        this.dataset = new OGDataset();
    }

    /**
     * Adds an RDF data model to the current OneGraph dataset
     * @param rdfData An RDF data model
     */
    public void addRDFModelToOG(@NonNull Model rdfData) {

        Map <Statement, OGTripleStatement> rdfToOGStatement = new HashMap<>();

        // Iterate over all RDF statements, create either property or relationship statements in OG.
        for (Statement rdfStatement : rdfData) {
            // Create a copy of the statement without its context/graph.
            Statement rdfStatementNoCTX = SimpleValueFactory.getInstance().createStatement(rdfStatement.getSubject(),
                    rdfStatement.getPredicate(), rdfStatement.getObject());

            // Either obtain an existing OG statement or create a new one.
            OGTripleStatement ogStatement = rdfToOGStatement.get(rdfStatementNoCTX);
            if (ogStatement == null) {
                ogStatement = this.createOGStatementFromRDFStatement(rdfStatementNoCTX);
                this.dataset.addStatement(ogStatement);
                rdfToOGStatement.put(rdfStatementNoCTX, ogStatement);
            }
            this.dataset.addMembershipStatementForStatement(ogStatement, rdfStatement.getContext());
        }
    }

    /**
     * Creates an RDF Model from the current OneGraph dataset.
     * @return The RDF data model created from the OneGraph dataset
     */
    public Model createRDFModelFromOGDataset() {

        Model rdf = new LinkedHashModel();

        for (OGPropertyStatement statement : this.dataset.getPropertyStatements()) {
            Optional<Set<OGGraph<?>>> graphsForStatement = this.dataset.graphsForStatement(statement);
            if (graphsForStatement.isPresent()) {
                for (OGGraph<?> graph : graphsForStatement.get()) {
                    Optional<Statement> rdfStatement = this.transformToRDFStatement(statement, graph);
                    rdfStatement.ifPresent(rdf::add);
                }
            }
        }
        for (OGRelationshipStatement statement : this.dataset.getRelationshipStatements()) {
            Optional<Set<OGGraph<?>>> graphsForStatement = this.dataset.graphsForStatement(statement);
            if (graphsForStatement.isPresent()) {
                for (OGGraph<?> graph : graphsForStatement.get()) {
                    Optional<Statement> rdfStatement = this.transformToRDFStatement(statement, graph);
                    rdfStatement.ifPresent(rdf::add);
                }
            }
        }
        return rdf;
    }

    private OGTripleStatement createOGStatementFromRDFStatement(Statement statement) throws NoSuchElementException {
        final Resource subject = statement.getSubject();
        final IRI predicate = statement.getPredicate();
        final Value object = statement.getObject();

        OGSimpleNodeIRI predicateSN = this.dataset.createOrObtainSimpleNode(predicate);
        OGSimpleNode<?> subjectSN;
        if (subject instanceof IRI) {
            subjectSN = this.dataset.createOrObtainSimpleNode((IRI) subject);
        } else {
            subjectSN = this.dataset.createOrObtainSimpleNode((BNode) subject);
        }

        if (object instanceof Literal) {
            Literal objLit = (Literal) object;
            OGValue<Literal> value = new OGValue<>(objLit);
            return new OGPropertyStatement(subjectSN, predicateSN, value);
        }

        OGSimpleNode<?> objectSN;
        if (object instanceof IRI) {
            objectSN = this.dataset.createOrObtainSimpleNode((IRI) object);
        } else {
            objectSN = this.dataset.createOrObtainSimpleNode((BNode) object);
        }
        return new OGRelationshipStatement(subjectSN, predicateSN, objectSN);
    }

    private Optional<Statement> transformToRDFStatement(OGPropertyStatement statement, OGGraph<?> graph) {
        final Optional<Resource> subject = statement.subject.convertToResource();
        final Optional<IRI> predicate = statement.predicate.iriPredicate();

        if (!(subject.isPresent() && predicate.isPresent())) {
            return Optional.empty();
        }
        return Optional.of(createRDFStatement(subject.get(), predicate.get(), statement.object.literalValue(), graph));
    }

    private Optional<Statement> transformToRDFStatement(OGRelationshipStatement statement, OGGraph<?> graph) {
        final Optional<Resource> subject = statement.subject.convertToResource();
        final Optional<IRI> predicate = statement.predicate.iriPredicate();
        final Optional<Resource> object = statement.object.convertToResource();

        if (!(subject.isPresent() && predicate.isPresent() && object.isPresent())) {
            return Optional.empty();
        }
        return Optional.of(createRDFStatement(subject.get(), predicate.get(), object.get(), graph));
    }

    private Statement createRDFStatement(Resource subject, IRI predicate, Value object, OGGraph<?> graph) {
        ValueFactory factory = SimpleValueFactory.getInstance();
        return factory.createStatement(subject, predicate, object, (Resource) graph.graphName);
    }
}
