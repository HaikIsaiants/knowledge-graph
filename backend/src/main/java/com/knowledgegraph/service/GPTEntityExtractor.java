package com.knowledgegraph.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgegraph.model.Edge;
import com.knowledgegraph.model.EdgeType;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.EdgeRepository;
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
    private final EdgeRepository edgeRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    @Value("${openai.completion.model:gpt-5-nano}")
    private String model;
    
    private static final int MAX_TOKENS_PER_CHUNK = 2000;
    private static final int MAX_RESPONSE_TOKENS = 2000;
    
    /**
     * Extract entities and relationships from text using GPT
     */
    public ExtractionResult extractEntitiesAndRelationships(String text, UUID documentId) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            log.warn("OpenAI API key not configured, skipping entity extraction");
            return new ExtractionResult(new ArrayList<>(), new ArrayList<>());
        }
        
        log.info("Starting entity and relationship extraction with model: {}", model);
        OpenAiService openAiService = new OpenAiService(apiKey, java.time.Duration.ofHours(1));
        List<Node> allNodes = new ArrayList<>();
        List<ExtractedRelationship> allRelationships = new ArrayList<>();
        
        try {
            // Split text into manageable chunks
            List<String> chunks = splitIntoChunks(text);
            log.info("Split text into {} chunks for processing", chunks.size());
            
            for (String chunk : chunks) {
                log.info("Processing chunk...");
                // Extract entities AND relationships in one pass
                ChunkExtractionResult chunkResult = extractFromChunk(openAiService, chunk);
                
                // Create nodes from entities
                for (ExtractedEntity entity : chunkResult.entities) {
                    Node node = createNodeFromEntity(entity, documentId);
                    allNodes.add(node);
                }
                
                // Collect relationships
                log.info("Chunk returned {} relationships", chunkResult.relationships.size());
                allRelationships.addAll(chunkResult.relationships);
            }
            
            log.info("Total relationships collected from all chunks: {}", allRelationships.size());
            
            // Deduplicate entities across chunks
            allNodes = deduplicateNodes(allNodes);
            
            // Save nodes to database
            allNodes = nodeRepository.saveAll(allNodes);
            
            // Create and save edges from relationships
            List<Edge> edges = createEdgesFromRelationships(allRelationships, allNodes, documentId);
            edges = edgeRepository.saveAll(edges);
            
            log.info("Extracted {} entities and {} relationships from document", allNodes.size(), edges.size());
            
            return new ExtractionResult(allNodes, edges);
            
        } catch (Exception e) {
            log.error("Error extracting entities with GPT: " + e.getMessage(), e);
            e.printStackTrace();
        }
        
        return new ExtractionResult(allNodes, new ArrayList<>());
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
    
    private ChunkExtractionResult extractFromChunk(OpenAiService service, String chunk) {
        String prompt = """
            Extract entities and their relationships from this text.
            
            Entities include: people, organizations, places, concepts, theories, ideas, events, 
            documents, schools of thought, technical terms, products, technologies, dates.
            
            For each relationship, identify the source entity, target entity, relationship type,
            and the evidence from the text.
            
            IMPORTANT: Use ONLY these relationship types (organized by category):
            
            Organizational: AFFILIATED_WITH, WORKS_AT, EMPLOYED_BY, FOUNDED, FOUNDED_BY, OWNS, OWNED_BY, MANAGES, MANAGED_BY
            
            Intellectual: AUTHORED, AUTHORED_BY, CREATED, CREATED_BY, DEVELOPED, DEVELOPED_BY, INVENTED, INVENTED_BY, DISCOVERED, DISCOVERED_BY, PROPOSED, PROPOSED_BY
            
            Academic/Research: STUDIED, STUDIED_BY, TEACHES, TAUGHT_BY, INFLUENCED, INFLUENCED_BY, CITED_BY, CITES, REFERENCES, BUILDS_ON, CONTRADICTS, SUPPORTS, CRITIQUES
            
            Temporal: PRECEDED_BY, PRECEDES, OCCURRED_DURING, HAPPENED_AT
            
            Spatial: LOCATED_IN, LOCATED_AT, NEAR
            
            Hierarchical: PART_OF, CONTAINS, PARENT_OF, CHILD_OF, SUBCATEGORY_OF
            
            Event: PARTICIPATED_IN, ATTENDED, ORGANIZED, ORGANIZED_BY, PRESENTED_AT
            
            Production: PRODUCED, PRODUCED_BY, MANUFACTURED_BY, PUBLISHED_BY
            
            Comparison: SIMILAR_TO, DIFFERENT_FROM, SAME_AS, OPPOSITE_OF
            
            Causal: CAUSES, CAUSED_BY, LEADS_TO, RESULTS_IN, DERIVES_FROM
            
            Generic: RELATED_TO, ASSOCIATED_WITH, CONNECTED_TO, DEPENDS_ON, USES, USED_BY
            
            Educational/Academic: TOOK_EXAM, SCORED, RECEIVED_GRADE, ENROLLED_IN, GRADUATED_FROM, MAJORED_IN, MINORED_IN, RECEIVED_DEGREE, RECEIVED_CERTIFICATE, RECEIVED_AWARD, STUDIES_AT, RESEARCHES, ADVISES, ADVISED_BY, COLLABORATES_WITH, CO_AUTHORED, PEER_REVIEWED, SUBMITTED_TO, ACCEPTED_BY, REJECTED_BY
            
            Personal/Identity: HAS_IDENTIFIER, HAS_ID, HAS_NAME, KNOWN_AS, ALIAS_OF, LIVES_AT, RESIDED_AT, BORN_IN, BORN_ON, DIED_IN, DIED_ON
            
            Professional: REPORTS_TO, SUPERVISES, HIRED, HIRED_BY, FIRED, FIRED_BY, PROMOTED, PROMOTED_BY, TRANSFERRED_TO, WORKED_ON, CONTRIBUTED_TO, LED, MEMBER_OF, BOARD_MEMBER_OF, INVESTOR_IN, FUNDED, FUNDED_BY
            
            Legal/Ownership: OWNS_STAKE_IN, ACQUIRED, ACQUIRED_BY, MERGED_WITH, SPUN_OFF_FROM, LICENSED_TO, LICENSED_FROM, SUED, SUED_BY, SETTLED_WITH, REGULATED_BY, COMPLIES_WITH, VIOLATES
            
            Communication: MENTIONED_IN, QUOTED_IN, RESPONDED_TO, REPLIED_TO, COMMENTED_ON, SHARED, FORWARDED, TAGGED_IN, LINKS_TO
            
            Process/Workflow: PRECONDITION_OF, POSTCONDITION_OF, TRIGGERS, TRIGGERED_BY, ENABLES, BLOCKS, BLOCKED_BY, REQUIRES, REQUIRED_BY, OPTIONAL_FOR, ALTERNATIVE_TO
            
            Measurement: GREATER_THAN, LESS_THAN, EQUAL_TO, PROPORTIONAL_TO, INVERSELY_PROPORTIONAL_TO, CORRELATED_WITH, BENCHMARKED_AGAINST
            
            Medical/Health: PATIENT_OF, TREATS, DIAGNOSED_WITH, PRESCRIBED, PRESCRIBED_BY, SYMPTOM_OF, SIDE_EFFECT_OF, CONTRAINDICATED_WITH, INTERACTS_WITH
            
            Version/Evolution: VERSION_OF, UPGRADED_TO, DOWNGRADED_TO, REPLACED_BY, REPLACES, DEPRECATED_BY, FORK_OF, BRANCH_OF, MERGED_INTO
            
            Classification: INSTANCE_OF, TYPE_OF, KIND_OF, EXAMPLE_OF, CATEGORY_OF, CLASSIFIED_AS, LABELED_AS, TAGGED_AS
            
            Emotional/Social: LIKES, DISLIKES, LOVES, HATES, FEARS, TRUSTS, DISTRUSTS, RESPECTS, ADMIRES, ENVIES, FRIEND_OF, ENEMY_OF, RIVAL_OF, PARTNER_OF, MARRIED_TO, DIVORCED_FROM, PARENT_OF, SIBLING_OF, ANCESTOR_OF, DESCENDANT_OF
            
            If no specific type fits, use RELATED_TO.
            
            Return as JSON with format:
            {
                "entities": [
                    {"name": "entity name", "type": "PERSON/ORGANIZATION/CONCEPT/etc", "context": "brief context"}
                ],
                "relationships": [
                    {
                        "source": "source entity name",
                        "target": "target entity name",
                        "type": "One of the types listed above",
                        "evidence": "text showing this relationship"
                    }
                ]
            }
            
            Text: %s
            """.formatted(chunk);
        
        try {
            // GPT-5 models have specific limitations:
            // 1. Don't support temperature parameter (only default value of 1.0)
            // 2. Don't support maxTokens (use max_completion_tokens instead)
            var requestBuilder = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(Arrays.asList(
                            new ChatMessage("system", "You are an entity and relationship extraction system. Return only valid JSON."),
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
            log.info("GPT Response length: {} characters", response.length());
            log.debug("GPT Response: {}", response);
            
            // Parse JSON response
            return parseExtractionResponse(response);
            
        } catch (Exception e) {
            log.error("Error calling GPT for extraction", e);
            return new ChunkExtractionResult();
        }
    }
    
    private ChunkExtractionResult parseExtractionResponse(String jsonResponse) {
        ChunkExtractionResult result = new ChunkExtractionResult();
        
        try {
            // Clean up response if needed
            jsonResponse = jsonResponse.trim();
            if (jsonResponse.startsWith("```json")) {
                jsonResponse = jsonResponse.substring(7);
            }
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
            
            Map<String, Object> parsed = objectMapper.readValue(jsonResponse, Map.class);
            
            // Parse entities
            List<Map<String, String>> entities = (List<Map<String, String>>) parsed.get("entities");
            if (entities != null) {
                for (Map<String, String> item : entities) {
                    ExtractedEntity entity = new ExtractedEntity();
                    entity.name = item.get("name");
                    entity.context = item.getOrDefault("context", "");
                    String typeStr = item.get("type");
                    try {
                        entity.type = typeStr != null ? NodeType.valueOf(typeStr) : NodeType.ENTITY;
                    } catch (IllegalArgumentException e) {
                        entity.type = NodeType.OTHER;
                    }
                    result.entities.add(entity);
                }
            }
            
            // Parse relationships
            List<Map<String, String>> relationships = (List<Map<String, String>>) parsed.get("relationships");
            if (relationships != null) {
                log.info("Found {} relationships in GPT response", relationships.size());
                for (Map<String, String> item : relationships) {
                    ExtractedRelationship rel = new ExtractedRelationship();
                    rel.sourceName = item.get("source");
                    rel.targetName = item.get("target");
                    String typeStr = item.get("type");
                    try {
                        rel.type = typeStr != null ? EdgeType.valueOf(typeStr) : EdgeType.RELATED_TO;
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown relationship type: {}, using RELATED_TO", typeStr);
                        rel.type = EdgeType.RELATED_TO;
                    }
                    rel.evidence = item.getOrDefault("evidence", "");
                    result.relationships.add(rel);
                }
            } else {
                log.info("No relationships found in GPT response");
            }
            
        } catch (Exception e) {
            log.error("Failed to parse extraction JSON: {}", jsonResponse, e);
        }
        
        return result;
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
    
    private List<Edge> createEdgesFromRelationships(List<ExtractedRelationship> relationships, 
                                                    List<Node> nodes, UUID documentId) {
        List<Edge> edges = new ArrayList<>();
        
        // Create a map for quick node lookup by name
        Map<String, Node> nodeMap = new HashMap<>();
        for (Node node : nodes) {
            nodeMap.put(node.getName().toLowerCase(), node);
        }
        
        for (ExtractedRelationship rel : relationships) {
            Node sourceNode = nodeMap.get(rel.sourceName.toLowerCase());
            Node targetNode = nodeMap.get(rel.targetName.toLowerCase());
            
            if (sourceNode != null && targetNode != null) {
                Edge edge = new Edge();
                edge.setSource(sourceNode);
                edge.setTarget(targetNode);
                edge.setType(rel.type);
                edge.setCapturedAt(LocalDateTime.now());
                
                // Store relationship metadata
                Map<String, Object> properties = new HashMap<>();
                properties.put("evidence", rel.evidence);
                properties.put("extracted_by", "GPT");
                properties.put("confidence", rel.confidence);
                properties.put("source_document_id", documentId.toString());
                edge.setProperties(properties);
                
                edges.add(edge);
            } else {
                log.warn("Could not create edge - missing nodes: {} -> {}", 
                         rel.sourceName, rel.targetName);
            }
        }
        
        return edges;
    }
    
    // Inner classes
    private static class ExtractedEntity {
        String name;
        String context;
        NodeType type = NodeType.ENTITY;
        double confidence = 0.8;
    }
    
    private static class ExtractedRelationship {
        String sourceName;
        String targetName;
        EdgeType type = EdgeType.RELATED_TO;
        String evidence;
        double confidence = 0.8;
    }
    
    private static class ChunkExtractionResult {
        List<ExtractedEntity> entities = new ArrayList<>();
        List<ExtractedRelationship> relationships = new ArrayList<>();
    }
    
    public static class ExtractionResult {
        public final List<Node> nodes;
        public final List<Edge> edges;
        
        public ExtractionResult(List<Node> nodes, List<Edge> edges) {
            this.nodes = nodes;
            this.edges = edges;
        }
    }
}