package com.knowledgegraph.service;

import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.DocumentRepository;
import com.knowledgegraph.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvIngestionService extends AbstractIngestionService {

    private final NodeRepository nodeRepository;
    private final FileStorageService fileStorageService;
    private final EmbeddingService embeddingService;
    private final GPTEntityExtractor gptEntityExtractor;

    @Transactional
    public IngestionResult processCsvFile(String filePath, UUID jobId) {
        log.info("Processing CSV file: {}", filePath);
        
        IngestionResult.IngestionResultBuilder resultBuilder = IngestionResult.builder()
                .jobId(jobId)
                .processedAt(LocalDateTime.now());
        
        List<IngestionResult.ProcessingError> errors = new ArrayList<>();
        List<UUID> createdNodeIds = new ArrayList<>();
        List<UUID> createdDocumentIds = new ArrayList<>();
        
        int totalRecords = 0;
        int successCount = 0;
        int errorCount = 0;
        
        try {
            // First, create a document record for the CSV file
            Document document = createDocumentRecord(filePath, "text/csv", "CSV");
            createdDocumentIds.add(document.getId());
            
            // Parse CSV file
            Path path = Paths.get(filePath);
            try (Reader reader = new FileReader(path.toFile());
                 CSVParser csvParser = new CSVParser(reader, 
                     CSVFormat.DEFAULT.builder()
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .setIgnoreEmptyLines(true)
                         .setTrim(true)
                         .build())) {
                
                List<String> headers = csvParser.getHeaderNames();
                log.info("CSV headers: {}", headers);
                
                for (CSVRecord record : csvParser) {
                    totalRecords++;
                    try {
                        Node node = processRecord(record, headers, document);
                        if (node != null) {
                            createdNodeIds.add(node.getId());
                            successCount++;
                        }
                    } catch (Exception e) {
                        errorCount++;
                        errors.add(IngestionResult.ProcessingError.builder()
                                .lineNumber((int) record.getRecordNumber())
                                .errorMessage(e.getMessage())
                                .errorType(e.getClass().getSimpleName())
                                .build());
                        log.error("Error processing record at line {}: {}", 
                                 record.getRecordNumber(), e.getMessage());
                    }
                }
            }
            
            // Extract entities from all CSV text content using GPT
            try {
                log.info("Extracting entities from CSV content using GPT");
                StringBuilder allContent = new StringBuilder();
                
                // Collect all text from CSV for entity extraction
                CSVFormat csvFormat2 = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build();
                    
                try (Reader reader2 = new FileReader(path.toFile());
                     CSVParser csvParser2 = new CSVParser(reader2, csvFormat2)) {
                    
                    for (CSVRecord record : csvParser2) {
                        for (String value : record) {
                            if (value != null && !value.trim().isEmpty()) {
                                allContent.append(value).append(" ");
                            }
                        }
                        allContent.append("\n");
                    }
                }
                
                // Extract entities using GPT
                List<Node> extractedEntities = gptEntityExtractor.extractEntities(
                    allContent.toString(),
                    document.getId()
                );
                
                // Add extracted entity IDs to the result
                for (Node entity : extractedEntities) {
                    createdNodeIds.add(entity.getId());
                }
                
                log.info("Extracted {} entities from CSV content", extractedEntities.size());
                
            } catch (Exception e) {
                log.warn("Failed to extract entities using GPT: {}", e.getMessage());
                // Continue without entity extraction - not a fatal error
            }
            
            log.info("CSV processing complete. Total: {}, Success: {}, Errors: {}", 
                     totalRecords, successCount, errorCount);
            
            return resultBuilder
                    .success(errorCount == 0)
                    .message(String.format("Processed %d records from CSV file", totalRecords))
                    .totalRecords(totalRecords)
                    .successCount(successCount)
                    .errorCount(errorCount)
                    .createdNodeIds(createdNodeIds)
                    .createdDocumentIds(createdDocumentIds)
                    .errors(errors)
                    .build();
                    
        } catch (IOException e) {
            log.error("Failed to process CSV file: {}", filePath, e);
            return buildErrorResult(jobId, "Failed to process CSV file: " + e.getMessage(), "IOException");
        }
    }


    private Node processRecord(CSVRecord record, List<String> headers, Document document) {
        // Determine node type based on content
        NodeType nodeType = determineNodeType(record);
        
        // Extract name from record
        String name = extractName(record, headers);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("No name found in record");
        }
        
        // Create properties map from CSV record
        Map<String, Object> properties = new HashMap<>();
        for (String header : headers) {
            String value = record.get(header);
            if (value != null && !value.trim().isEmpty()) {
                properties.put(header, value);
            }
        }
        
        // Check if node already exists based on name and type
        List<Node> existingNodes = nodeRepository.findByNameAndType(name, nodeType);
        if (!existingNodes.isEmpty()) {
            log.debug("Node already exists: {} ({})", name, nodeType);
            // Update properties of existing node
            Node existingNode = existingNodes.get(0);
            existingNode.getProperties().putAll(properties);
            Node savedNode = nodeRepository.save(existingNode);
            
            // Update embedding for the node
            try {
                String embeddingText = name + " " + properties.getOrDefault("description", "").toString();
                embeddingService.createEmbedding(savedNode, embeddingText);
                log.debug("Updated embedding for node: {}", name);
            } catch (Exception e) {
                log.error("Failed to update embedding for node {}: {}", name, e.getMessage());
            }
            
            return savedNode;
        }
        
        // Create new node
        Node node = new Node();
        node.setName(name);
        node.setType(nodeType);
        node.setProperties(properties);
        node.setSourceUri(document.getUri());
        node.setCapturedAt(LocalDateTime.now());
        
        Node savedNode = nodeRepository.save(node);
        log.debug("Created node: {} ({})", name, nodeType);
        
        // Generate embedding for the node
        try {
            String embeddingText = name + " " + properties.getOrDefault("description", "").toString();
            embeddingService.createEmbedding(savedNode, embeddingText);
            log.debug("Created embedding for node: {}", name);
        } catch (Exception e) {
            log.error("Failed to create embedding for node {}: {}", name, e.getMessage());
            // Continue processing even if embedding fails
        }
        
        return savedNode;
    }

    private NodeType determineNodeType(CSVRecord record) {
        // Simple heuristics to determine node type
        // Can be made more sophisticated based on requirements
        
        if (hasField(record, "email") || hasField(record, "role") || 
            hasField(record, "firstName") || hasField(record, "lastName")) {
            return NodeType.PERSON;
        }
        
        if (hasField(record, "organization") || hasField(record, "company") ||
            hasField(record, "department") || hasField(record, "industry")) {
            return NodeType.ORGANIZATION;
        }
        
        if (hasField(record, "location") || hasField(record, "address") ||
            hasField(record, "city") || hasField(record, "country")) {
            return NodeType.PLACE;
        }
        
        if (hasField(record, "date") || hasField(record, "eventDate") ||
            hasField(record, "startDate")) {
            return NodeType.EVENT;
        }
        
        if (hasField(record, "product") || hasField(record, "sku") ||
            hasField(record, "price")) {
            return NodeType.ITEM;
        }
        
        // Default to CONCEPT if no specific type is determined
        return NodeType.CONCEPT;
    }

    private String extractName(CSVRecord record, List<String> headers) {
        // Priority order for name extraction
        String[] nameFields = {"name", "title", "label", "fullName", "firstName", 
                              "organizationName", "companyName", "productName", "id"};
        
        for (String field : nameFields) {
            if (record.isMapped(field)) {
                String value = record.get(field);
                if (value != null && !value.trim().isEmpty()) {
                    // If it's firstName, try to combine with lastName
                    if (field.equals("firstName") && record.isMapped("lastName")) {
                        String lastName = record.get("lastName");
                        if (lastName != null && !lastName.trim().isEmpty()) {
                            return value + " " + lastName;
                        }
                    }
                    return value;
                }
            }
        }
        
        // If no standard name field found, use the first non-empty field
        for (String header : headers) {
            String value = record.get(header);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        
        return null;
    }

    private boolean hasField(CSVRecord record, String fieldName) {
        try {
            if (record.isMapped(fieldName)) {
                String value = record.get(fieldName);
                return value != null && !value.trim().isEmpty();
            }
        } catch (Exception e) {
            // Field doesn't exist
        }
        return false;
    }
}