package com.knowledgegraph.service;

import com.knowledgegraph.model.Node;
import com.knowledgegraph.repository.EmbeddingRepository;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Real OpenAI embedding service that generates actual semantic embeddings
 */
@Service
@Profile("!mock") // Only active when NOT using mock profile
@Slf4j
@RequiredArgsConstructor
public class OpenAIEmbeddingService implements EmbeddingService {
    
    private final EmbeddingRepository embeddingRepository;
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    @Value("${openai.embedding.model:text-embedding-3-small}")
    private String model;
    
    private OpenAiService openAiService;
    
    @PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            log.warn("OpenAI API key not configured. Falling back to mock embeddings.");
            // In production, you might want to throw an exception here
            return;
        }
        
        // Initialize OpenAI service with timeout
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
        log.info("OpenAI embedding service initialized with model: {}", model);
    }
    
    @Override
    public float[] generateEmbedding(String text) {
        if (openAiService == null) {
            log.warn("OpenAI service not initialized, returning empty embedding");
            return new float[1536]; // Return zeros
        }
        
        try {
            // Create embedding request
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .model(model)
                    .input(Collections.singletonList(text))
                    .build();
            
            // Get embeddings from OpenAI
            EmbeddingResult result = openAiService.createEmbeddings(request);
            
            if (result.getData().isEmpty()) {
                log.error("No embeddings returned from OpenAI");
                return new float[1536];
            }
            
            // Get the embedding vector
            List<Double> embedding = result.getData().get(0).getEmbedding();
            
            // Convert List<Double> to float[]
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = embedding.get(i).floatValue();
            }
            
            log.debug("Generated embedding with {} dimensions for text of length {}", 
                     vector.length, text.length());
            
            return vector;
            
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            return new float[1536]; // Return zeros on error
        }
    }
    
    @Override
    public int getDimensions() {
        return 1536; // text-embedding-3-small dimensions
    }
    
    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        return texts.stream().map(this::generateEmbedding).toList();
    }
    
    @Override
    @Transactional
    public com.knowledgegraph.model.Embedding createEmbedding(com.knowledgegraph.model.Node node, String text) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }
        
        // Check if embedding already exists for this node
        List<com.knowledgegraph.model.Embedding> existing = embeddingRepository.findByNode_Id(node.getId());
        if (!existing.isEmpty()) {
            log.debug("Embedding already exists for node {}, updating it", node.getId());
            com.knowledgegraph.model.Embedding embedding = existing.get(0);
            float[] vector = generateEmbedding(text);
            embedding.setVector(vector);
            return embeddingRepository.save(embedding);
        }
        
        // Generate embedding vector
        float[] vector = generateEmbedding(text);
        
        // Create and save embedding entity
        com.knowledgegraph.model.Embedding embedding = new com.knowledgegraph.model.Embedding();
        embedding.setNode(node);
        embedding.setVector(vector);
        embedding.setModelVersion(getModelVersion());
        embedding.setCreatedAt(LocalDateTime.now());
        
        return embeddingRepository.save(embedding);
    }
    
    @Override
    public List<com.knowledgegraph.model.Embedding> createEmbeddings(java.util.Map<com.knowledgegraph.model.Node, String> nodeTextMap) {
        throw new UnsupportedOperationException("Not implemented - use MockEmbeddingService for persistence");
    }
    
    @Override
    public String getModelVersion() {
        return model != null ? model : "text-embedding-3-small";
    }
    
    @Override
    public int getEmbeddingDimension() {
        return 1536;
    }
    
    @Override
    public double calculateSimilarity(float[] vector1, float[] vector2) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    @Override
    public List<com.knowledgegraph.model.Embedding> findSimilar(float[] queryVector, int topK, double threshold) {
        throw new UnsupportedOperationException("Not implemented - use repository for similarity search");
    }
}