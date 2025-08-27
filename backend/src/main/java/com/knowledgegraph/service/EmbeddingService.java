package com.knowledgegraph.service;

import com.knowledgegraph.model.Embedding;
import com.knowledgegraph.model.Node;

import java.util.List;
import java.util.Map;

/**
 * Interface for generating vector embeddings from text
 */
public interface EmbeddingService {

    /**
     * Generate embedding for a single text
     */
    float[] generateEmbedding(String text);

    /**
     * Generate embeddings for multiple texts (batch processing)
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * Create and save embedding for a node
     */
    Embedding createEmbedding(Node node, String text);

    /**
     * Create and save embeddings for multiple nodes
     */
    List<Embedding> createEmbeddings(Map<Node, String> nodeTextMap);

    /**
     * Get the model version/name being used
     */
    String getModelVersion();

    /**
     * Get the dimension of the embeddings
     */
    int getEmbeddingDimension();

    /**
     * Calculate cosine similarity between two vectors
     */
    double calculateSimilarity(float[] vector1, float[] vector2);

    /**
     * Find most similar embeddings to a query vector
     */
    List<Embedding> findSimilar(float[] queryVector, int topK, double threshold);
}