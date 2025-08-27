package com.knowledgegraph.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Edge;
import com.knowledgegraph.model.EdgeType;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.DocumentRepository;
import com.knowledgegraph.repository.EdgeRepository;
import com.knowledgegraph.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonIngestionService extends AbstractIngestionService {

    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public IngestionResult processJsonFile(String filePath, UUID jobId) {
        log.info("Processing JSON file: {}", filePath);
        
        IngestionResult.IngestionResultBuilder resultBuilder = IngestionResult.builder()
                .jobId(jobId)
                .processedAt(LocalDateTime.now());
        
        List<IngestionResult.ProcessingError> errors = new ArrayList<>();
        List<UUID> createdNodeIds = new ArrayList<>();
        List<UUID> createdEdgeIds = new ArrayList<>();
        List<UUID> createdDocumentIds = new ArrayList<>();
        
        int totalRecords = 0;
        int successCount = 0;
        int errorCount = 0;
        
        try {
            // Create document record
            Document document = createDocumentRecord(filePath, "application/json", "JSON");
            createdDocumentIds.add(document.getId());
            
            // Parse JSON file
            File file = new File(filePath);
            JsonNode rootNode = objectMapper.readTree(file);
            
            // Process the JSON structure
            Map<String, Node> processedNodes = new HashMap<>();
            processJsonNode(rootNode, "", document, processedNodes, createdNodeIds, createdEdgeIds);
            
            totalRecords = processedNodes.size();
            successCount = createdNodeIds.size();
            
            log.info("JSON processing complete. Created {} nodes and {} edges", 
                     createdNodeIds.size(), createdEdgeIds.size());
            
            return resultBuilder
                    .success(true)
                    .message(String.format("Processed JSON file: created %d nodes and %d edges", 
                            createdNodeIds.size(), createdEdgeIds.size()))
                    .totalRecords(totalRecords)
                    .successCount(successCount)
                    .errorCount(errorCount)
                    .createdNodeIds(createdNodeIds)
                    .createdEdgeIds(createdEdgeIds)
                    .createdDocumentIds(createdDocumentIds)
                    .errors(errors)
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to process JSON file: {}", filePath, e);
            return buildErrorResult(jobId, "Failed to process JSON file: " + e.getMessage(), "IOException");
        }
    }


    private void processJsonNode(JsonNode node, String path, Document document, 
                                 Map<String, Node> processedNodes,
                                 List<UUID> createdNodeIds, 
                                 List<UUID> createdEdgeIds) {
        
        if (node.isObject()) {
            // Process object as potential entity
            Node entityNode = extractEntityFromObject(node, path, document);
            if (entityNode != null && !processedNodes.containsKey(entityNode.getName())) {
                entityNode = nodeRepository.save(entityNode);
                processedNodes.put(entityNode.getName(), entityNode);
                createdNodeIds.add(entityNode.getId());
                log.debug("Created node from object: {} at path: {}", entityNode.getName(), path);
            }
            
            // Process nested objects and arrays
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldPath = path.isEmpty() ? field.getKey() : path + "." + field.getKey();
                
                // Check for relationships
                if (field.getValue().isObject() || field.getValue().isArray()) {
                    processJsonNode(field.getValue(), fieldPath, document, 
                                  processedNodes, createdNodeIds, createdEdgeIds);
                    
                    // Create relationships between parent and child entities
                    if (entityNode != null && field.getValue().isObject()) {
                        Node childNode = extractEntityFromObject(field.getValue(), fieldPath, document);
                        if (childNode != null && processedNodes.containsKey(childNode.getName())) {
                            Edge edge = createEdge(entityNode, processedNodes.get(childNode.getName()), 
                                                 field.getKey(), document);
                            if (edge != null) {
                                createdEdgeIds.add(edge.getId());
                            }
                        }
                    }
                }
            }
            
        } else if (node.isArray()) {
            // Process array elements
            for (int i = 0; i < node.size(); i++) {
                String arrayPath = path + "[" + i + "]";
                processJsonNode(node.get(i), arrayPath, document, 
                              processedNodes, createdNodeIds, createdEdgeIds);
            }
        }
    }

    private Node extractEntityFromObject(JsonNode node, String path, Document document) {
        if (!node.isObject()) {
            return null;
        }
        
        // Extract name or identifier
        String name = extractName(node);
        if (name == null || name.trim().isEmpty()) {
            return null; // Skip objects without identifiable names
        }
        
        // Determine node type
        NodeType nodeType = determineNodeType(node, path);
        
        // Check if node already exists
        List<Node> existingNodes = nodeRepository.findByNameAndType(name, nodeType);
        if (!existingNodes.isEmpty()) {
            return existingNodes.get(0);
        }
        
        // Create properties from JSON fields
        Map<String, Object> properties = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isValueNode()) {
                properties.put(field.getKey(), field.getValue().asText());
            }
        }
        properties.put("jsonPath", path);
        
        // Create new node
        Node entityNode = new Node();
        entityNode.setName(name);
        entityNode.setType(nodeType);
        entityNode.setProperties(properties);
        entityNode.setSourceUri(document.getUri());
        entityNode.setCapturedAt(LocalDateTime.now());
        
        return entityNode;
    }

    private String extractName(JsonNode node) {
        // Priority order for name extraction
        String[] nameFields = {"name", "title", "label", "id", "key", "identifier", 
                              "fullName", "organizationName", "companyName"};
        
        for (String field : nameFields) {
            if (node.has(field) && !node.get(field).isNull()) {
                return node.get(field).asText();
            }
        }
        
        // For person objects, try to combine first and last names
        if (node.has("firstName") && node.has("lastName")) {
            return node.get("firstName").asText() + " " + node.get("lastName").asText();
        }
        
        return null;
    }

    private NodeType determineNodeType(JsonNode node, String path) {
        // Check field names for hints
        if (node.has("email") || node.has("firstName") || node.has("lastName") || 
            path.toLowerCase().contains("person") || path.toLowerCase().contains("user")) {
            return NodeType.PERSON;
        }
        
        if (node.has("organizationName") || node.has("companyName") || 
            node.has("industry") || path.toLowerCase().contains("organization") ||
            path.toLowerCase().contains("company")) {
            return NodeType.ORGANIZATION;
        }
        
        if (node.has("address") || node.has("location") || node.has("coordinates") ||
            path.toLowerCase().contains("place") || path.toLowerCase().contains("location")) {
            return NodeType.PLACE;
        }
        
        if (node.has("eventDate") || node.has("startDate") || node.has("endDate") ||
            path.toLowerCase().contains("event")) {
            return NodeType.EVENT;
        }
        
        if (node.has("price") || node.has("sku") || node.has("productName") ||
            path.toLowerCase().contains("product") || path.toLowerCase().contains("item")) {
            return NodeType.ITEM;
        }
        
        if (path.toLowerCase().contains("document") || node.has("content") || 
            node.has("text") || node.has("description")) {
            return NodeType.DOCUMENT;
        }
        
        // Default to CONCEPT
        return NodeType.CONCEPT;
    }

    private Edge createEdge(Node source, Node target, String relationshipType, Document document) {
        if (source == null || target == null || source.getId().equals(target.getId())) {
            return null;
        }
        
        // Determine edge type based on relationship
        EdgeType edgeType = determineEdgeType(relationshipType, source.getType(), target.getType());
        
        // Check if edge already exists
        boolean edgeExists = edgeRepository.existsBySource_IdAndTarget_IdAndType(
                source.getId(), target.getId(), edgeType);
        if (edgeExists) {
            return null;
        }
        
        // Create new edge
        Edge edge = new Edge();
        edge.setSource(source);
        edge.setTarget(target);
        edge.setType(edgeType);
        edge.setSourceUri(document.getUri());
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("relationshipType", relationshipType);
        properties.put("createdFrom", "JSON ingestion");
        edge.setProperties(properties);
        
        Edge savedEdge = edgeRepository.save(edge);
        log.debug("Created edge: {} --[{}]--> {}", source.getName(), edgeType, target.getName());
        
        return savedEdge;
    }

    private EdgeType determineEdgeType(String relationshipType, NodeType sourceType, NodeType targetType) {
        String relType = relationshipType.toLowerCase();
        
        if (relType.contains("organization") || relType.contains("company") || 
            relType.contains("employer") || relType.contains("affiliation")) {
            return EdgeType.AFFILIATED_WITH;
        }
        
        if (relType.contains("part") || relType.contains("component") || 
            relType.contains("child") || relType.contains("parent")) {
            return EdgeType.PART_OF;
        }
        
        if (relType.contains("reference") || relType.contains("cite") || 
            relType.contains("link")) {
            return EdgeType.REFERENCES;
        }
        
        if (relType.contains("similar") || relType.contains("related") || 
            relType.contains("like")) {
            return EdgeType.SIMILAR_TO;
        }
        
        if (relType.contains("located") || relType.contains("place") || 
            relType.contains("where")) {
            return EdgeType.LOCATED_IN;
        }
        
        if (relType.contains("produced") || relType.contains("created") || 
            relType.contains("made")) {
            return EdgeType.PRODUCED_BY;
        }
        
        // Default based on node types
        if (sourceType == NodeType.PERSON && targetType == NodeType.ORGANIZATION) {
            return EdgeType.AFFILIATED_WITH;
        }
        
        if (sourceType == NodeType.PERSON && targetType == NodeType.EVENT) {
            return EdgeType.PARTICIPATED_IN;
        }
        
        // Default to SIMILAR_TO for general relationships
        return EdgeType.SIMILAR_TO;
    }
}