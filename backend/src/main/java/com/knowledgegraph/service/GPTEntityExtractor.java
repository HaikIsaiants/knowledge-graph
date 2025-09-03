package com.knowledgegraph.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.NodeRepository;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GPTEntityExtractor {
    
    private final NodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    @Value("${openai.completion.model:gpt-5-nano}")
    private String model;
    
    private static final int MAX_TOKENS_PER_CHUNK = 2000;
    private static final int MAX_RESPONSE_TOKENS = 2000;
    
    /**
     * Extract entities from text using GPT
     */
    public List<Node> extractEntities(String text, UUID documentId) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            log.warn("OpenAI API key not configured, skipping entity extraction");
            return new ArrayList<>();
        }
        
        log.info("Starting entity extraction with model: {}", model);
        OpenAiService openAiService = new OpenAiService(apiKey, java.time.Duration.ofHours(1));
        List<Node> allNodes = new ArrayList<>();
        
        try {
            // Split text into manageable chunks
            List<String> chunks = splitIntoChunks(text);
            log.info("Split text into {} chunks for processing", chunks.size());
            
            for (String chunk : chunks) {
                log.info("Processing chunk...");
                // First pass: Extract entities
                List<ExtractedEntity> entities = extractEntitiesFromChunk(openAiService, chunk);
                
                // Second pass: Classify entities (if needed)
                entities = classifyEntities(openAiService, entities, chunk);
                
                // Create nodes from entities
                for (ExtractedEntity entity : entities) {
                    Node node = createNodeFromEntity(entity, documentId);
                    allNodes.add(node);
                }
            }
            
            // Deduplicate entities across chunks
            allNodes = deduplicateNodes(allNodes);
            
            // Save nodes to database
            allNodes = nodeRepository.saveAll(allNodes);
            
            log.info("Extracted {} entities from document", allNodes.size());
            
        } catch (Exception e) {
            log.error("Error extracting entities with GPT: " + e.getMessage(), e);
            e.printStackTrace();
        }
        
        return allNodes;
    }
    
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        
        // Simple splitting by character count (roughly 4 chars per token)
        int chunkSize = MAX_TOKENS_PER_CHUNK * 4;
        
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
        }
        
        return chunks;
    }
    
    private List<ExtractedEntity> extractEntitiesFromChunk(OpenAiService service, String chunk) {
        String prompt = """
            Extract ALL important entities from this text.
            Include: people, organizations, places, concepts, theories, ideas, events, 
            documents, schools of thought, technical terms, products, technologies, dates.
            
            Return as JSON array with format:
            [{"name": "entity name", "context": "brief context where it appears"}]
            
            Text: %s
            """.formatted(chunk);
        
        try {
            // GPT-5 models have specific limitations:
            // 1. Don't support temperature parameter (only default value of 1.0)
            // 2. Don't support maxTokens (use max_completion_tokens instead)
            var requestBuilder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(Arrays.asList(
                            new ChatMessage("system", "You are an entity extraction system. Return only valid JSON."),
                            new ChatMessage("user", prompt)
                    ));
            
            // Only add temperature and maxTokens for non-GPT-5 models
            if (!model.startsWith("gpt-5")) {
                requestBuilder.temperature(0.1);
                requestBuilder.maxTokens(MAX_RESPONSE_TOKENS);
            }
            
            ChatCompletionRequest request = requestBuilder.build();
            
            log.info("Calling OpenAI API with model: {}", model);
            ChatCompletionResult result = service.createChatCompletion(request);
            log.info("Received response from OpenAI");
            String response = result.getChoices().get(0).getMessage().getContent();
            
            // Parse JSON response
            return parseEntityResponse(response);
            
        } catch (Exception e) {
            log.error("Error calling GPT for entity extraction", e);
            return new ArrayList<>();
        }
    }
    
    private List<ExtractedEntity> classifyEntities(OpenAiService service, List<ExtractedEntity> entities, String context) {
        if (entities.isEmpty()) return entities;
        
        String entityNames = entities.stream()
                .map(e -> e.name)
                .collect(Collectors.joining(", "));
        
        String prompt = """
            Classify these entities into types:
            PERSON, ORGANIZATION, LOCATION, CONCEPT, THEORY, EVENT, DOCUMENT, 
            TERM, DATE, SCHOOL_OF_THOUGHT, METHOD, TECHNOLOGY, PRODUCT, LAW, PRINCIPLE, OTHER
            
            Entities: %s
            
            Context: %s
            
            Return as JSON array:
            [{"name": "entity", "type": "TYPE"}]
            """.formatted(entityNames, context.substring(0, Math.min(context.length(), 500)));
        
        try {
            var requestBuilder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(Arrays.asList(
                            new ChatMessage("system", "You are an entity classification system. Return only valid JSON."),
                            new ChatMessage("user", prompt)
                    ));
            
            // Only add temperature and maxTokens for non-GPT-5 models
            if (!model.startsWith("gpt-5")) {
                requestBuilder.temperature(0.1);
                requestBuilder.maxTokens(MAX_RESPONSE_TOKENS);
            }
            
            ChatCompletionRequest request = requestBuilder.build();
            
            log.info("Calling OpenAI API with model: {}", model);
            ChatCompletionResult result = service.createChatCompletion(request);
            log.info("Received response from OpenAI");
            String response = result.getChoices().get(0).getMessage().getContent();
            
            // Parse and merge classification with entities
            return mergeClassifications(entities, response);
            
        } catch (Exception e) {
            log.error("Error classifying entities", e);
            return entities;
        }
    }
    
    private List<ExtractedEntity> parseEntityResponse(String jsonResponse) {
        List<ExtractedEntity> entities = new ArrayList<>();
        
        try {
            // Clean up response if needed
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7);
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
            
            List<Map<String, String>> parsed = objectMapper.readValue(jsonResponse, List.class);
            
            for (Map<String, String> item : parsed) {
                ExtractedEntity entity = new ExtractedEntity();
                entity.name = item.get("name");
                entity.context = item.getOrDefault("context", "");
                entity.type = NodeType.ENTITY; // Default type
                entities.add(entity);
            }
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse entity JSON: {}", jsonResponse, e);
        }
        
        return entities;
    }
    
    private List<ExtractedEntity> mergeClassifications(List<ExtractedEntity> entities, String classificationJson) {
        try {
            // Clean up response
            classificationJson = classificationJson.trim();
            if (classificationJson.startsWith("```json")) {
                classificationJson = classificationJson.substring(7);
            }
            if (classificationJson.endsWith("```")) {
                classificationJson = classificationJson.substring(0, classificationJson.length() - 3);
            }
            
            List<Map<String, String>> classifications = objectMapper.readValue(classificationJson, List.class);
            
            Map<String, String> typeMap = new HashMap<>();
            for (Map<String, String> item : classifications) {
                typeMap.put(item.get("name").toLowerCase(), item.get("type"));
            }
            
            // Update entity types
            for (ExtractedEntity entity : entities) {
                String typeStr = typeMap.get(entity.name.toLowerCase());
                if (typeStr != null) {
                    try {
                        entity.type = NodeType.valueOf(typeStr);
                    } catch (IllegalArgumentException e) {
                        entity.type = NodeType.OTHER;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to merge classifications", e);
        }
        
        return entities;
    }
    
    private Node createNodeFromEntity(ExtractedEntity entity, UUID documentId) {
        Node node = new Node();
        node.setName(entity.name);
        node.setType(entity.type);
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        
        // Store additional metadata
        Map<String, Object> properties = new HashMap<>();
        properties.put("context", entity.context);
        properties.put("extracted_by", "GPT");
        properties.put("confidence", entity.confidence);
        properties.put("source_document_id", documentId.toString());
        node.setProperties(properties);
        
        return node;
    }
    
    private List<Node> deduplicateNodes(List<Node> nodes) {
        Map<String, Node> uniqueNodes = new HashMap<>();
        
        for (Node node : nodes) {
            String key = node.getName().toLowerCase() + "_" + node.getType();
            
            if (!uniqueNodes.containsKey(key)) {
                uniqueNodes.put(key, node);
            } else {
                // Merge properties if duplicate found
                Node existing = uniqueNodes.get(key);
                Map<String, Object> mergedProps = new HashMap<>(existing.getProperties());
                mergedProps.putAll(node.getProperties());
                existing.setProperties(mergedProps);
            }
        }
        
        return new ArrayList<>(uniqueNodes.values());
    }
    
    // Inner class for extracted entities
    private static class ExtractedEntity {
        String name;
        String context;
        NodeType type = NodeType.ENTITY;
        double confidence = 0.8;
    }
}