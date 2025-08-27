package com.knowledgegraph.service;

import com.knowledgegraph.dto.SearchResponseDTO;
import com.knowledgegraph.dto.SearchResultDTO;
import com.knowledgegraph.model.Embedding;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.repository.EmbeddingRepository;
import com.knowledgegraph.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class VectorSearchService {
    
    private final EmbeddingRepository embeddingRepository;
    private final NodeRepository nodeRepository;
    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;
    
    @Value("${search.vector.threshold:0.7}")
    private double defaultThreshold;
    
    @Value("${search.vector.k:10}")
    private int defaultK;
    
    /**
     * Find similar nodes using vector similarity search
     */
    public SearchResponseDTO findSimilar(String queryText, Double threshold, Integer limit) {
        long startTime = System.currentTimeMillis();
        log.debug("Vector search for: '{}', threshold: {}, limit: {}", queryText, threshold, limit);
        
        // Use Optional for cleaner null handling
        double searchThreshold = Optional.ofNullable(threshold).orElse(defaultThreshold);
        int searchLimit = Optional.ofNullable(limit).orElse(defaultK);
        
        // Generate query vector and search
        String vectorString = arrayToPostgresVector(
            embeddingService.generateEmbedding(queryText)
        );
        
        List<SearchResultDTO> searchResults = convertVectorResults(
            embeddingRepository.findSimilarEmbeddings(vectorString, searchThreshold, searchLimit),
            queryText
        );
        
        return SearchResponseDTO.builder()
            .results(searchResults)
            .totalElements(searchResults.size())
            .totalPages(1)
            .currentPage(0)
            .pageSize(searchLimit)
            .query(queryText)
            .searchType(SearchResponseDTO.SearchType.VECTOR)
            .searchTimeMs(System.currentTimeMillis() - startTime)
            .minScore(searchResults.stream().mapToDouble(r -> Optional.ofNullable(r.getScore()).orElse(0.0)).min().orElse(0.0))
            .maxScore(searchResults.stream().mapToDouble(r -> Optional.ofNullable(r.getScore()).orElse(0.0)).max().orElse(0.0))
            .build();
    }
    
    /**
     * Find nodes similar to a given node
     */
    public SearchResponseDTO findSimilarNodes(UUID nodeId, Integer limit) {
        long startTime = System.currentTimeMillis();
        log.debug("Finding nodes similar to: {}", nodeId);
        
        int searchLimit = limit != null ? limit : defaultK;
        
        // Get embeddings for the source node
        List<Embedding> nodeEmbeddings = embeddingRepository.findByNode_Id(nodeId);
        
        if (nodeEmbeddings.isEmpty()) {
            log.warn("No embeddings found for node: {}", nodeId);
            return SearchResponseDTO.builder()
                .results(new ArrayList<>())
                .totalElements(0)
                .searchType(SearchResponseDTO.SearchType.VECTOR)
                .searchTimeMs(System.currentTimeMillis() - startTime)
                .build();
        }
        
        // Use the first embedding's vector
        Embedding sourceEmbedding = nodeEmbeddings.get(0);
        String vectorString = arrayToPostgresVector(sourceEmbedding.getVector());
        
        // Find similar embeddings
        List<Object[]> results = embeddingRepository.findDiverseSimilarEmbeddings(
            vectorString,
            defaultThreshold,
            searchLimit + 1  // +1 to exclude self
        );
        
        // Filter out the source node and convert
        List<SearchResultDTO> searchResults = results.stream()
            .filter(row -> !nodeId.equals(row[1]))  // row[1] is node_id
            .map(row -> convertVectorResult(row))
            .limit(searchLimit)
            .collect(Collectors.toList());
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        Node sourceNode = nodeRepository.findById(nodeId).orElse(null);
        String queryDescription = sourceNode != null ? 
            "Similar to: " + sourceNode.getName() : 
            "Similar to node: " + nodeId;
        
        return SearchResponseDTO.builder()
            .results(searchResults)
            .totalElements(searchResults.size())
            .totalPages(1)
            .currentPage(0)
            .pageSize(searchLimit)
            .query(queryDescription)
            .searchType(SearchResponseDTO.SearchType.VECTOR)
            .searchTimeMs(searchTime)
            .build();
    }
    
    /**
     * Perform k-NN search with configurable parameters
     */
    public SearchResponseDTO knnSearch(float[] queryVector, int k, double threshold) {
        long startTime = System.currentTimeMillis();
        log.debug("k-NN search with k={}, threshold={}", k, threshold);
        
        String vectorString = arrayToPostgresVector(queryVector);
        
        // Perform k-NN search
        List<Object[]> results = embeddingRepository.findSimilarEmbeddings(
            vectorString,
            threshold,
            k
        );
        
        List<SearchResultDTO> searchResults = convertVectorResults(results, "k-NN search");
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        return SearchResponseDTO.builder()
            .results(searchResults)
            .totalElements(searchResults.size())
            .totalPages(1)
            .currentPage(0)
            .pageSize(k)
            .query("k-NN vector search")
            .searchType(SearchResponseDTO.SearchType.VECTOR)
            .searchTimeMs(searchTime)
            .build();
    }
    
    /**
     * Get vector for a specific node
     */
    public float[] getNodeVector(UUID nodeId) {
        List<Embedding> embeddings = embeddingRepository.findByNode_Id(nodeId);
        
        if (embeddings.isEmpty()) {
            // Generate embedding if not exists
            Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
            
            String content = node.getName() + " " + 
                (node.getProperties() != null ? node.getProperties().toString() : "");
            
            return embeddingService.generateEmbedding(content);
        }
        
        return embeddings.get(0).getVector();
    }
    
    /**
     * Convert vector results to SearchResultDTOs
     */
    private List<SearchResultDTO> convertVectorResults(List<Object[]> results, String query) {
        return results.stream()
            .map(this::convertVectorResult)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * Convert single vector result row to SearchResultDTO
     */
    private SearchResultDTO convertVectorResult(Object[] row) {
        try {
            // row structure: id, node_id, document_id, content_snippet, model_version, created_at, similarity
            return Optional.ofNullable(row[1])
                .map(Object::toString)
                .map(UUID::fromString)
                .flatMap(nodeRepository::findById)
                .map(node -> SearchResultDTO.builder()
                    .id(node.getId())
                    .type(node.getType())
                    .title(node.getName())
                    .snippet(row[3] != null ? row[3].toString() : "")
                    .score(row[6] != null ? ((Number) row[6]).doubleValue() : 0.0)
                    .sourceUri(node.getSourceUri())
                    .metadata(node.getProperties())
                    .createdAt(node.getCreatedAt())
                    .updatedAt(node.getUpdatedAt())
                    .build())
                .orElse(null);
        } catch (Exception e) {
            log.error("Error converting vector result: ", e);
            return null;
        }
    }
    
    /**
     * Convert float array to PostgreSQL vector string format
     */
    private String arrayToPostgresVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        
        // Simplified using streams
        return Arrays.stream(vector)
            .mapToObj(String::valueOf)
            .collect(Collectors.joining(",", "[", "]"));
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    public double calculateSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }
        
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
}