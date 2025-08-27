package com.knowledgegraph.service;

import com.knowledgegraph.model.Embedding;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.repository.EmbeddingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockEmbeddingService implements EmbeddingService {

    private final EmbeddingRepository embeddingRepository;
    private static final int EMBEDDING_DIMENSION = 384; // Typical small model dimension
    private static final String MODEL_VERSION = "mock-v1.0";
    private final Random random = new Random();

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        // Generate deterministic "random" embeddings based on text hash
        // This ensures the same text always produces the same embedding
        float[] embedding = new float[EMBEDDING_DIMENSION];
        
        try {
            // Hash the text to get a seed
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes());
            
            // Use hash to seed random number generator for consistency
            Random seededRandom = new Random(bytesToLong(hash));
            
            // Generate normalized random vector
            double magnitude = 0.0;
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                embedding[i] = (float) seededRandom.nextGaussian();
                magnitude += embedding[i] * embedding[i];
            }
            
            // Normalize the vector to unit length
            magnitude = Math.sqrt(magnitude);
            if (magnitude > 0) {
                for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                    embedding[i] /= magnitude;
                }
            }
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback to truly random embeddings
            log.warn("Failed to create deterministic embedding, using random", e);
            for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
                embedding[i] = (float) random.nextGaussian();
            }
        }

        log.debug("Generated mock embedding for text of length: {}", text.length());
        return embedding;
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        List<float[]> embeddings = new ArrayList<>();
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        
        log.debug("Generated {} mock embeddings", embeddings.size());
        return embeddings;
    }

    @Override
    @Transactional
    public Embedding createEmbedding(Node node, String text) {
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        // Check if embedding already exists for this node
        List<Embedding> existingEmbeddings = embeddingRepository.findByNode_Id(node.getId());
        if (!existingEmbeddings.isEmpty()) {
            log.debug("Embedding already exists for node: {}", node.getName());
            return existingEmbeddings.get(0);
        }

        // Generate embedding vector
        float[] vector = generateEmbedding(text);

        // Create embedding entity
        Embedding embedding = new Embedding();
        embedding.setNode(node);
        embedding.setVector(vector);
        embedding.setModelVersion(MODEL_VERSION);
        embedding.setContentSnippet(text.length() > 500 ? 
            text.substring(0, 500) + "..." : text);
        embedding.setCreatedAt(LocalDateTime.now());

        Embedding savedEmbedding = embeddingRepository.save(embedding);
        log.info("Created embedding for node: {} using model: {}", node.getName(), MODEL_VERSION);
        
        return savedEmbedding;
    }

    @Override
    @Transactional
    public List<Embedding> createEmbeddings(Map<Node, String> nodeTextMap) {
        if (nodeTextMap == null || nodeTextMap.isEmpty()) {
            return new ArrayList<>();
        }

        List<Embedding> embeddings = new ArrayList<>();
        
        for (Map.Entry<Node, String> entry : nodeTextMap.entrySet()) {
            try {
                Embedding embedding = createEmbedding(entry.getKey(), entry.getValue());
                embeddings.add(embedding);
            } catch (Exception e) {
                log.error("Failed to create embedding for node: {}", 
                         entry.getKey().getName(), e);
            }
        }
        
        log.info("Created {} embeddings from {} nodes", 
                embeddings.size(), nodeTextMap.size());
        return embeddings;
    }

    @Override
    public String getModelVersion() {
        return MODEL_VERSION;
    }

    @Override
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }

    @Override
    public double calculateSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null) {
            throw new IllegalArgumentException("Vectors cannot be null");
        }
        
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        // Calculate cosine similarity
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    @Override
    public List<Embedding> findSimilar(float[] queryVector, int topK, double threshold) {
        // In a real implementation, this would use pgvector or another vector database
        // For mock, we'll just return some random embeddings from the database
        
        List<Embedding> allEmbeddings = embeddingRepository.findAll();
        
        // Calculate similarities and sort
        List<EmbeddingSimilarity> similarities = new ArrayList<>();
        for (Embedding embedding : allEmbeddings) {
            if (embedding.getVector() != null) {
                double similarity = calculateSimilarity(queryVector, embedding.getVector());
                if (similarity >= threshold) {
                    similarities.add(new EmbeddingSimilarity(embedding, similarity));
                }
            }
        }

        // Sort by similarity descending
        similarities.sort((a, b) -> Double.compare(b.similarity, a.similarity));

        // Return top K
        List<Embedding> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, similarities.size()); i++) {
            results.add(similarities.get(i).embedding);
        }

        log.debug("Found {} similar embeddings (threshold: {}, topK: {})", 
                 results.size(), threshold, topK);
        return results;
    }

    /**
     * Convert byte array to long for seeding - simplified using ByteBuffer
     */
    private long bytesToLong(byte[] bytes) {
        // Use first 8 bytes as seed, pad with zeros if needed
        byte[] seedBytes = java.util.Arrays.copyOf(bytes, 8);
        return java.nio.ByteBuffer.wrap(seedBytes).getLong();
    }

    /**
     * Helper class for similarity sorting
     */
    private static class EmbeddingSimilarity {
        final Embedding embedding;
        final double similarity;

        EmbeddingSimilarity(Embedding embedding, double similarity) {
            this.embedding = embedding;
            this.similarity = similarity;
        }
    }
}