package software.amazon.neptune.onegraph.playground.server.io.parsing.ogparser;

import software.amazon.neptune.onegraph.playground.server.io.parsing.ParserException;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.dataset.OGDataset;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGGraph;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGReifiableElement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeBNode;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.elements.OGSimpleNodeIRI;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGPropertyStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGRelationshipStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGTripleStatement;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.values.OGValue;
import software.amazon.neptune.onegraph.playground.server.model.onegraph.statements.OGMembershipStatement;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.NonNull;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses OneGraph files.
 */
public class OGParser {

    // Maps statement ids to the statements created for them.
    private final Map<String, OGTripleStatement> sidToStatement = new HashMap<>();

    // Graphs added during parsing.
    private final Map<Resource, OGGraph<?>> newGraphs = new HashMap<>();

    // Simple nodes added during parsing.
    private final Map<IRI, OGSimpleNodeIRI> newIRISimpleNodes = new HashMap<>();

    // Simple nodes added during parsing.
    private final Map<BNode , OGSimpleNodeBNode> newBNodeSimpleNodes = new HashMap<>();

    // The names of BNodes are not used in the creation of the actual BNode,
    // This prevents naming collisions of BNodes across different loads.
    // This map to keep track of which BNode was created for which BNode name
    private final Map<String , BNode> bNodeNameToBNode = new HashMap<>();


    /**
     * Parses a OneGraph file.
     * @param stream The input stream of the file.
     * @param dataset The dataset the parsed data should be added to.
     * @throws ParserException When an exception is encountered during parsing.
     */
    public void parseOGTriples(@NonNull InputStream stream, @NonNull OGDataset dataset) throws ParserException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));) {
            CharacterReader reader = new CharacterReader();
            Tokenizer tokenizer = new Tokenizer();

            int lineNum = 1;
            ArrayList<List<Token>> tokenizedLines = new ArrayList<>();
            for (String line; (line = br.readLine()) != null; ) {
                if (line.length() == 0) {
                    continue;
                }

                LinkedList<Token> tokens = new LinkedList<>();

                reader.load(line);

                // Keep collecting tokens until EOL or start of comment.
                Optional<Token> token = tokenizer.nextToken(reader);
                while (token.isPresent() && token.get().type != TokenType.COMMENT) {
                    Token t = token.get();
                    t.lineNum = lineNum;
                    tokens.add(t);

                    token = tokenizer.nextToken(reader);
                }
                // Check if this was a comment-only line.
                if (tokens.size() == 0 &&
                        token.isPresent() &&
                        token.get().type == TokenType.COMMENT) {
                    continue;
                }
                // 3 is the minimum amount of tokens on one line (subject, predicate and object),
                if (tokens.size() < 3) {
                    throw ParserException.unexpectedEndOfLine(line);
                }
                tokenizedLines.add(tokens);
                lineNum++;
            }
            processTokenizedLines(tokenizedLines, dataset);
        } catch (IOException e) {
            throw new ParserException("Parsing failed with exception: ", e);
        }
    }

    // Process a list of lines into statements and add them to the given data set.
    // This algorithm processes the lines twice to handle reification.
    private void processTokenizedLines(List<List<Token>> lines, OGDataset dataset) throws ParserException {

        BiMap<List<Token>, OGRelationshipStatement> lineToRelStatement = HashBiMap.create();
        BiMap<List<Token>, OGPropertyStatement> lineToPropStatement = HashBiMap.create();
        Set<OGTripleStatement> statementsThatReify = new HashSet<>();

        // First create empty property and relationship statements for each line.
        createEmptyStatements(lines, lineToRelStatement, lineToPropStatement, statementsThatReify, dataset);

        // Next fill the statements with their appropriate values (subject, object) and create membership statements.
        Set<OGMembershipStatement> membershipStatements = new HashSet<>();
        materializeStatementsAndAddMembership(lines, lineToRelStatement, lineToPropStatement, membershipStatements, dataset);

        if (hasReferenceCycle(statementsThatReify)) {
            throw ParserException.referenceCycle();
        }

        addAllToDataset(membershipStatements,
                lineToRelStatement.values(),
                lineToPropStatement.values(),
                dataset);
    }

    private OGGraph<?> getGraphForName(Resource graphName, OGDataset dataset) {
        OGGraph <?> g;
        if (dataset.containsGraphForName(graphName)) {
            g = dataset.createOrObtainGraph(graphName);
        } else {
            g = newGraphs.computeIfAbsent(graphName, k -> new OGGraph<>(graphName));
        }
        return g;
    }

    private OGSimpleNodeIRI getSNFor(IRI linkedComp, OGDataset dataset) {
        OGSimpleNodeIRI sn;
        if (dataset.containsSimpleNodeForComponent(linkedComp)) {
            sn = dataset.createOrObtainSimpleNode(linkedComp);
        } else {
            sn = newIRISimpleNodes.computeIfAbsent(linkedComp, k -> new OGSimpleNodeIRI(linkedComp));
        }
        return sn;
    }

    private OGSimpleNodeBNode getSNFor(BNode linkedComp, OGDataset dataset) {
        OGSimpleNodeBNode sn;
        if (dataset.containsSimpleNodeForComponent(linkedComp)) {
            sn = dataset.createOrObtainSimpleNode(linkedComp);
        } else {
            sn = newBNodeSimpleNodes.computeIfAbsent(linkedComp, k -> new OGSimpleNodeBNode(linkedComp));
        }
        return sn;
    }

    // Adds a membership statement for the given statement to the given dataset, getting a graph from the given token.
    private OGMembershipStatement createMembershipStatementForToken(Token token, OGTripleStatement stat, OGDataset dataset)
            throws ParserException {
        Resource graphName;
        switch (token.type) {
            case IRI:
                graphName = iriFromToken(token);
                break;
            case BNODE:
                graphName = bNodeFromToken(token);
                break;
            default:
                throw ParserException.invalidIRIorBNode(token.content, token.lineNum);
        }
        OGGraph <?> g = getGraphForName(graphName, dataset);
        return new OGMembershipStatement(stat, g);
    }

    // Creates a blank node from the content of the given token.
    private BNode bNodeFromToken(Token token) throws IllegalArgumentException {
        try {
            if (token.content.length() <= 2) {
                throw new IllegalArgumentException();
            }
            String name = token.content.substring(2);
            return this.bNodeNameToBNode.computeIfAbsent(name, n -> SimpleValueFactory.getInstance().createBNode());
        } catch (IllegalArgumentException e) {
            throw ParserException.invalidBNode(token.content, token.lineNum, e);
        }
    }

    // Creates an IRI from the content of the given token.
    private IRI iriFromToken(Token token) throws IllegalArgumentException {
        try {
            return SimpleValueFactory.getInstance().createIRI(token.content);
        } catch (IllegalArgumentException e) {
            throw ParserException.invalidIRI(token.content, token.lineNum, e);
        }
    }


    // Creates or obtains a reifiable element from the given token, using the given dataset.
    // If token type == SID then element will be OGTripleStatement
    // If token type == IRI then element will be Simple node
    // If token type == BNode then element will be Simple node
    private OGReifiableElement elementFromToken(Token token, OGDataset dataset) throws ParserException {
        switch (token.type) {
            case SID:
                OGReifiableElement element = sidToStatement.get(token.content);
                if (element == null) {
                    throw ParserException.missingSID(token.content, token.lineNum);
                }
                return element;
            case IRI:
                return getSNFor(iriFromToken(token), dataset);
            case BNODE:
                return getSNFor(bNodeFromToken(token), dataset);
            default:
                throw ParserException.invalidIRIorBNode(token.content, token.lineNum);
        }
    }

    // Create membership statements for the given tokens.
    private Set<OGMembershipStatement> createMembershipStatements(List<Token> tokens,
                                                                  OGTripleStatement stat,
                                                                  OGDataset dataset)
            throws ParserException {

        Set<OGMembershipStatement> membershipStatements = new HashSet<>();

        boolean processingGraphs  = false;
        boolean putInDefaultGraph = true;

        if (tokens.size() > 3) {
            for (Token t : tokens.subList(3, tokens.size())) {
                if (processingGraphs) {
                    OGMembershipStatement mem = createMembershipStatementForToken(t, stat, dataset);
                    membershipStatements.add(mem);
                } else if (t.type == TokenType.SID) {
                    sidToStatement.put(t.content, stat);
                } else if (t.type == TokenType.IN) {
                    processingGraphs = true;
                } else if (t.type == TokenType.FLAG_U){
                    putInDefaultGraph = false;
                    break;
                } else {
                    throw ParserException.unexpectedEntry(t.content, t.lineNum);
                }
            }
        }

        if (putInDefaultGraph) {
            final OGGraph<?> defGraph =  getGraphForName(OGGraph.DEFAULT_GRAPH_IRI, dataset);
            OGMembershipStatement mem = new OGMembershipStatement(stat, defGraph);
            membershipStatements.add(mem);
        }
        return membershipStatements;
    }

    private void materializeStatement(List<Token> line, OGPropertyStatement statement, OGDataset dataset) {
        Token tokenSub = line.get(0);
        statement.subject = elementFromToken(tokenSub, dataset);
    }

    private void materializeStatement(List<Token> line, OGRelationshipStatement stat, OGDataset dataset) {
        Token tokenSub = line.get(0);
        Token tokenObj = line.get(2);
        stat.subject = elementFromToken(tokenSub, dataset);
        stat.object = elementFromToken(tokenObj, dataset);
    }

    private void materializeStatementsAndAddMembership(List<List<Token>> lines,
                                             BiMap<List<Token>, OGRelationshipStatement> lineToRelStatement,
                                             BiMap<List<Token>, OGPropertyStatement> lineToPropStatement,
                                             Set<OGMembershipStatement> membershipStatements,
                                             OGDataset dataset) throws ParserException {
        for (List<Token> line : lines) {
            OGPropertyStatement prop = lineToPropStatement.get(line);
            if (prop != null) {
                materializeStatement(line, prop, dataset);
                membershipStatements.addAll(createMembershipStatements(line, prop, dataset));
            }
            OGRelationshipStatement rel = lineToRelStatement.get(line);
            if (rel != null) {
                materializeStatement(line, rel, dataset);
                membershipStatements.addAll(createMembershipStatements(line, rel, dataset));
            }
        }
    }
    // Creates a statements for the given lines
    private void createEmptyStatements(List<List<Token>> lines,
                                       BiMap<List<Token>, OGRelationshipStatement> lineToRelStatement,
                                       BiMap<List<Token>, OGPropertyStatement> lineToPropStatement,
                                       Set<OGTripleStatement> statementsThatReify,
                                       OGDataset dataset) {
        for (List<Token> line : lines) {
            Token tokenSub = line.get(0);
            Token tokenPred = line.get(1);
            Token tokenObj = line.get(2);

            OGTripleStatement statement;
            OGSimpleNodeIRI predicate = getSNFor(iriFromToken(tokenPred), dataset);
            if (tokenObj.type == TokenType.LITERAL) {
                OGValue<?> object = new OGValue<>(LiteralProcessing.fromToken(tokenObj));
                OGPropertyStatement prop = new OGPropertyStatement(predicate, object);
                statement = prop;
                lineToPropStatement.put(line, prop);
            } else {
                OGRelationshipStatement rel = new OGRelationshipStatement(predicate);
                statement = rel;
                lineToRelStatement.put(line, rel);
            }
            if (line.size() > 3 && line.get(3).type == TokenType.SID) {
                sidToStatement.put(line.get(3).content, statement);
            }

            if (tokenSub.type == TokenType.SID || tokenObj.type == TokenType.SID) {
                statementsThatReify.add(statement);
            }
        }
    }

    private void addAllToDataset(Set<OGMembershipStatement> membershipStatements,
                                        Set<OGRelationshipStatement> relStatements,
                                        Set<OGPropertyStatement> propStatements,
                                        OGDataset dataset) {
        for (OGMembershipStatement m : membershipStatements) {
            dataset.addStatement(m);
        }
        for (OGRelationshipStatement r : relStatements) {
            dataset.addStatement(r);
        }
        for (OGPropertyStatement p : propStatements) {
            dataset.addStatement(p);
        }
        for (OGSimpleNodeBNode sn : newBNodeSimpleNodes.values()) {
            dataset.addSimpleNode(sn);
        }
        for (OGSimpleNodeIRI sn : newIRISimpleNodes.values()) {
            dataset.addSimpleNode(sn);
        }
        for (OGGraph<?> g : newGraphs.values()) {
            dataset.addGraph(g);
        }
    }

    private boolean hasReferenceCycle(Set<OGTripleStatement> statements) {
        Set<OGTripleStatement> visited = new HashSet<>();
        Set<OGTripleStatement> stack = new HashSet<>();
        for (OGTripleStatement statement : statements) {
            if (!visited.contains(statement)) {
                if (hasReferenceCycle(statement, visited, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasReferenceCycle(OGTripleStatement statement,
                                            Set<OGTripleStatement> visited,
                                            Set<OGTripleStatement> stack) {

        visited.add(statement);
        stack.add(statement);

        Set<OGTripleStatement> neighbours = new HashSet<>();
        if (statement instanceof OGRelationshipStatement) {
            OGRelationshipStatement rel = (OGRelationshipStatement) statement;
            if (rel.subject instanceof OGTripleStatement) {
               neighbours.add((OGTripleStatement) rel.subject);
            }
            if (rel.object instanceof OGTripleStatement) {
                neighbours.add((OGTripleStatement) rel.object);
            }
        } else {
            OGPropertyStatement prop = (OGPropertyStatement) statement;
            if (prop.subject instanceof OGTripleStatement) {
                neighbours.add((OGTripleStatement) prop.subject);
            }
        }
        for (OGTripleStatement neigh : neighbours) {
            if (!visited.contains(neigh)) {
                if (hasReferenceCycle(neigh, visited, stack)) {
                    return true;
                }
            } else if (stack.contains(neigh)) {
                return true;
            }
        }
        stack.remove(statement);
        return false;
    }
}
