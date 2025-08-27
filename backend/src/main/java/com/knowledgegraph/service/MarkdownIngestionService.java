package com.knowledgegraph.service;

import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Edge;
import com.knowledgegraph.model.EdgeType;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.DocumentRepository;
import com.knowledgegraph.repository.EdgeRepository;
import com.knowledgegraph.repository.NodeRepository;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarkdownIngestionService extends AbstractIngestionService {

    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;

    @Transactional
    public IngestionResult processMarkdownFile(String filePath, UUID jobId) {
        log.info("Processing Markdown file: {}", filePath);
        
        IngestionResult.IngestionResultBuilder resultBuilder = IngestionResult.builder()
                .jobId(jobId)
                .processedAt(LocalDateTime.now());
        
        List<IngestionResult.ProcessingError> errors = new ArrayList<>();
        List<UUID> createdNodeIds = new ArrayList<>();
        List<UUID> createdEdgeIds = new ArrayList<>();
        List<UUID> createdDocumentIds = new ArrayList<>();
        
        try {
            // Read file content
            Path path = Paths.get(filePath);
            String content = Files.readString(path);
            
            // Create document record
            Document document = createDocumentRecord(filePath, "text/markdown", "Markdown");
            document.setContent(content);
            document.getMetadata().put("characterCount", content.length());
            document = documentRepository.save(document);
            createdDocumentIds.add(document.getId());
            
            // Parse markdown
            Parser parser = Parser.builder().build();
            com.vladsch.flexmark.util.ast.Document markdownDoc = parser.parse(content);
            
            // Process markdown structure
            MarkdownProcessor processor = new MarkdownProcessor(document);
            processor.processDocument(markdownDoc);
            
            // Save extracted entities
            for (Node node : processor.getExtractedNodes()) {
                Node savedNode = nodeRepository.save(node);
                createdNodeIds.add(savedNode.getId());
            }
            
            // Save relationships
            for (Edge edge : processor.getExtractedEdges()) {
                Edge savedEdge = edgeRepository.save(edge);
                createdEdgeIds.add(savedEdge.getId());
            }
            
            log.info("Markdown processing complete. Created {} nodes and {} edges",
                     createdNodeIds.size(), createdEdgeIds.size());
            
            return resultBuilder
                    .success(true)
                    .message(String.format("Processed Markdown file: created %d nodes and %d edges",
                            createdNodeIds.size(), createdEdgeIds.size()))
                    .totalRecords(processor.getSectionCount())
                    .successCount(createdNodeIds.size())
                    .errorCount(0)
                    .createdNodeIds(createdNodeIds)
                    .createdEdgeIds(createdEdgeIds)
                    .createdDocumentIds(createdDocumentIds)
                    .errors(errors)
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to process Markdown file: {}", filePath, e);
            return buildErrorResult(jobId, "Failed to process Markdown file: " + e.getMessage(), "IOException");
        }
    }


    private class MarkdownProcessor {
        private final Document sourceDocument;
        private final List<Node> extractedNodes = new ArrayList<>();
        private final List<Edge> extractedEdges = new ArrayList<>();
        private final Map<String, Node> headingNodes = new HashMap<>();
        private final Stack<Node> sectionStack = new Stack<>();
        private int sectionCount = 0;

        public MarkdownProcessor(Document sourceDocument) {
            this.sourceDocument = sourceDocument;
        }

        public void processDocument(com.vladsch.flexmark.util.ast.Document doc) {
            NodeVisitor visitor = new NodeVisitor(
                new VisitHandler<>(Heading.class, this::processHeading),
                new VisitHandler<>(Link.class, this::processLink),
                new VisitHandler<>(BulletListItem.class, this::processListItem),
                new VisitHandler<>(FencedCodeBlock.class, this::processCodeBlock),
                new VisitHandler<>(BlockQuote.class, this::processBlockQuote)
            );
            
            visitor.visit(doc);
            
            // Process any remaining content
            processTextContent(doc);
        }

        private void processHeading(Heading heading) {
            sectionCount++;
            String title = heading.getText().toString();
            int level = heading.getLevel();
            
            // Create node for this section
            Node sectionNode = new Node();
            sectionNode.setName(title);
            sectionNode.setType(NodeType.DOCUMENT);
            sectionNode.setSourceUri(sourceDocument.getUri());
            sectionNode.setCapturedAt(LocalDateTime.now());
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("headingLevel", level);
            properties.put("sectionNumber", sectionCount);
            properties.put("type", "markdown-section");
            
            // Get content under this heading
            String sectionContent = extractSectionContent(heading);
            if (sectionContent != null && !sectionContent.trim().isEmpty()) {
                properties.put("content", sectionContent);
                properties.put("preview", sectionContent.length() > 200 ? 
                    sectionContent.substring(0, 200) + "..." : sectionContent);
            }
            
            sectionNode.setProperties(properties);
            extractedNodes.add(sectionNode);
            
            // Create hierarchical relationships
            while (!sectionStack.isEmpty() && getLevel(sectionStack.peek()) >= level) {
                sectionStack.pop();
            }
            
            if (!sectionStack.isEmpty()) {
                // This is a subsection of the previous section
                Edge edge = new Edge();
                edge.setSource(sectionStack.peek());
                edge.setTarget(sectionNode);
                edge.setType(EdgeType.PART_OF);
                edge.setSourceUri(sourceDocument.getUri());
                
                Map<String, Object> edgeProps = new HashMap<>();
                edgeProps.put("relationship", "subsection");
                edge.setProperties(edgeProps);
                extractedEdges.add(edge);
            }
            
            sectionStack.push(sectionNode);
            headingNodes.put(title, sectionNode);
        }

        private void processLink(Link link) {
            String url = link.getUrl().toString();
            String text = link.getText().toString();
            
            // Check if it's an external link
            if (url.startsWith("http://") || url.startsWith("https://")) {
                Node linkNode = new Node();
                linkNode.setName(text.isEmpty() ? url : text);
                linkNode.setType(NodeType.DOCUMENT);
                linkNode.setSourceUri(sourceDocument.getUri());
                linkNode.setCapturedAt(LocalDateTime.now());
                
                Map<String, Object> properties = new HashMap<>();
                properties.put("url", url);
                properties.put("linkText", text);
                properties.put("type", "external-link");
                linkNode.setProperties(properties);
                
                extractedNodes.add(linkNode);
                
                // Create reference edge from current section to link
                if (!sectionStack.isEmpty()) {
                    Edge edge = new Edge();
                    edge.setSource(sectionStack.peek());
                    edge.setTarget(linkNode);
                    edge.setType(EdgeType.REFERENCES);
                    edge.setSourceUri(sourceDocument.getUri());
                    
                    Map<String, Object> edgeProps = new HashMap<>();
                    edgeProps.put("linkType", "external");
                    edge.setProperties(edgeProps);
                    extractedEdges.add(edge);
                }
            }
        }

        private void processListItem(BulletListItem item) {
            String itemText = item.getChildChars().toString().trim();
            
            // Look for patterns like "Name - Role at Organization"
            Pattern personPattern = Pattern.compile("\\*?\\*?([^-]+)\\*?\\*?\\s*-\\s*(.+?)\\s+at\\s+(.+)");
            Matcher matcher = personPattern.matcher(itemText);
            
            if (matcher.find()) {
                String name = matcher.group(1).trim();
                String role = matcher.group(2).trim();
                String organization = matcher.group(3).trim();
                
                // Create person node
                Node personNode = createPersonNode(name, role, organization);
                extractedNodes.add(personNode);
                
                // Create organization node if not exists
                Node orgNode = findOrCreateOrganizationNode(organization);
                extractedNodes.add(orgNode);
                
                // Create affiliation edge
                Edge affiliation = new Edge();
                affiliation.setSource(personNode);
                affiliation.setTarget(orgNode);
                affiliation.setType(EdgeType.AFFILIATED_WITH);
                affiliation.setSourceUri(sourceDocument.getUri());
                
                Map<String, Object> edgeProps = new HashMap<>();
                edgeProps.put("role", role);
                affiliation.setProperties(edgeProps);
                extractedEdges.add(affiliation);
            }
        }

        private void processCodeBlock(FencedCodeBlock codeBlock) {
            String language = codeBlock.getInfo().toString();
            String code = codeBlock.getContentChars().toString();
            
            if (!code.trim().isEmpty()) {
                Node codeNode = new Node();
                codeNode.setName("Code: " + (language.isEmpty() ? "snippet" : language));
                codeNode.setType(NodeType.DOCUMENT);
                codeNode.setSourceUri(sourceDocument.getUri());
                codeNode.setCapturedAt(LocalDateTime.now());
                
                Map<String, Object> properties = new HashMap<>();
                properties.put("language", language);
                properties.put("code", code);
                properties.put("type", "code-block");
                properties.put("lineCount", code.split("\n").length);
                codeNode.setProperties(properties);
                
                extractedNodes.add(codeNode);
                
                // Link to current section
                if (!sectionStack.isEmpty()) {
                    Edge edge = new Edge();
                    edge.setSource(sectionStack.peek());
                    edge.setTarget(codeNode);
                    edge.setType(EdgeType.PART_OF);
                    edge.setSourceUri(sourceDocument.getUri());
                    extractedEdges.add(edge);
                }
            }
        }

        private void processBlockQuote(BlockQuote quote) {
            String quoteText = quote.getChildChars().toString().trim();
            
            Node quoteNode = new Node();
            quoteNode.setName("Quote: " + (quoteText.length() > 50 ? 
                quoteText.substring(0, 50) + "..." : quoteText));
            quoteNode.setType(NodeType.CONCEPT);
            quoteNode.setSourceUri(sourceDocument.getUri());
            quoteNode.setCapturedAt(LocalDateTime.now());
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("content", quoteText);
            properties.put("type", "quote");
            quoteNode.setProperties(properties);
            
            extractedNodes.add(quoteNode);
        }

        private void processTextContent(com.vladsch.flexmark.util.ast.Document doc) {
            // Extract entities from text content
            String fullText = doc.getChars().toString();
            
            // Look for dates (simple pattern)
            Pattern datePattern = Pattern.compile("(Q[1-4]\\s+\\d{4}|\\d{4}-\\d{2}-\\d{2})");
            Matcher dateMatcher = datePattern.matcher(fullText);
            while (dateMatcher.find()) {
                String date = dateMatcher.group(1);
                Node dateNode = new Node();
                dateNode.setName("Date: " + date);
                dateNode.setType(NodeType.EVENT);
                dateNode.setSourceUri(sourceDocument.getUri());
                dateNode.setCapturedAt(LocalDateTime.now());
                
                Map<String, Object> properties = new HashMap<>();
                properties.put("date", date);
                properties.put("type", "timeline-event");
                dateNode.setProperties(properties);
                
                extractedNodes.add(dateNode);
            }
        }

        private String extractSectionContent(Heading heading) {
            // Get the content between this heading and the next heading of same or higher level
            com.vladsch.flexmark.util.ast.Node next = heading.getNext();
            StringBuilder content = new StringBuilder();
            
            while (next != null && !(next instanceof Heading && 
                   ((Heading)next).getLevel() <= heading.getLevel())) {
                if (!(next instanceof Heading)) {
                    content.append(next.getChars().toString()).append("\n");
                }
                next = next.getNext();
            }
            
            return content.toString().trim();
        }

        private Node createPersonNode(String name, String role, String organization) {
            Node node = new Node();
            node.setName(name);
            node.setType(NodeType.PERSON);
            node.setSourceUri(sourceDocument.getUri());
            node.setCapturedAt(LocalDateTime.now());
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("role", role);
            properties.put("organization", organization);
            properties.put("extractedFrom", "markdown-list");
            node.setProperties(properties);
            
            return node;
        }

        private Node findOrCreateOrganizationNode(String name) {
            List<Node> existing = nodeRepository.findByNameAndType(name, NodeType.ORGANIZATION);
            if (!existing.isEmpty()) {
                return existing.get(0);
            }
            
            Node node = new Node();
            node.setName(name);
            node.setType(NodeType.ORGANIZATION);
            node.setSourceUri(sourceDocument.getUri());
            node.setCapturedAt(LocalDateTime.now());
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("extractedFrom", "markdown");
            node.setProperties(properties);
            
            return node;
        }

        private int getLevel(Node node) {
            Object level = node.getProperties().get("headingLevel");
            return level != null ? (Integer) level : Integer.MAX_VALUE;
        }

        public List<Node> getExtractedNodes() {
            return extractedNodes;
        }

        public List<Edge> getExtractedEdges() {
            return extractedEdges;
        }

        public int getSectionCount() {
            return sectionCount;
        }
    }
}