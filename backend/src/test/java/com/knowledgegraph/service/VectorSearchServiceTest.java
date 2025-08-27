package com.knowledgegraph.service;

import com.knowledgegraph.dto.SearchResponseDTO;
import com.knowledgegraph.dto.SearchResultDTO;
import com.knowledgegraph.model.Embedding;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.EmbeddingRepository;
import com.knowledgegraph.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VectorSearchService Tests")
class VectorSearchServiceTest {

    @Mock
    private EmbeddingRepository embeddingRepository;
    
    @Mock
    private NodeRepository nodeRepository;
    
    @Mock
    private EmbeddingService embeddingService;
    
    @Mock
    private JdbcTemplate jdbcTemplate;
    
    @InjectMocks
    private VectorSearchService vectorSearchService;
    
    private Node testNode;
    private Embedding testEmbedding;
    private float[] testVector;
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up VectorSearchService test environment...");
        
        // Set default values for @Value fields
        ReflectionTestUtils.setField(vectorSearchService, "defaultThreshold", 0.7);
        ReflectionTestUtils.setField(vectorSearchService, "defaultK", 10);
        
        // Setup test data
        testNode = new Node();
        testNode.setId(UUID.randomUUID());
        testNode.setName("Test Node");
        testNode.setType(NodeType.CONCEPT);
        testNode.setSourceUri("http://example.com/test");
        testNode.setProperties(Map.of("key", "value"));
        testNode.setCreatedAt(LocalDateTime.now());
        testNode.setUpdatedAt(LocalDateTime.now());
        
        testVector = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        
        testEmbedding = new Embedding();
        testEmbedding.setId(UUID.randomUUID());
        testEmbedding.setNode(testNode);
        testEmbedding.setVector(testVector);
        testEmbedding.setContentSnippet("Test content snippet");
        testEmbedding.setModelVersion("test-model-v1");
        testEmbedding.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("findSimilar - Success with default parameters")
    void testFindSimilar_DefaultParameters() {
        System.out.println("Testing findSimilar with default parameters...");
        
        String queryText = "test query";
        float[] queryVector = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
        
        List<Object[]> mockResults = createMockVectorResults();
        
        when(embeddingService.generateEmbedding(queryText)).thenReturn(queryVector);
        when(embeddingRepository.findSimilarEmbeddings(anyString(), eq(0.7), eq(10)))
            .thenReturn(mockResults);
        when(nodeRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(testNode));
        
        SearchResponseDTO response = vectorSearchService.findSimilar(queryText, null, null);
        
        System.out.println("Similar results found: " + response.getTotalElements());
        assertNotNull(response);
        assertEquals(queryText, response.getQuery());
        assertEquals(SearchResponseDTO.SearchType.VECTOR, response.getSearchType());
        assertNotNull(response.getSearchTimeMs());
        assertTrue(response.getSearchTimeMs() >= 0);
        assertNotNull(response.getMinScore());
        assertNotNull(response.getMaxScore());
        
        verify(embeddingService).generateEmbedding(queryText);
        verify(embeddingRepository).findSimilarEmbeddings(anyString(), eq(0.7), eq(10));
        
        System.out.println("✓ findSimilar with default parameters works correctly");
    }
    
    @Test
    @DisplayName("findSimilar - Success with custom threshold and limit")
    void testFindSimilar_CustomParameters() {
        System.out.println("Testing findSimilar with custom parameters...");
        
        String queryText = "custom query";
        double threshold = 0.85;
        int limit = 5;
        float[] queryVector = new float[]{0.2f, 0.3f, 0.4f, 0.5f, 0.6f};
        
        List<Object[]> mockResults = createMockVectorResults();
        
        when(embeddingService.generateEmbedding(queryText)).thenReturn(queryVector);
        when(embeddingRepository.findSimilarEmbeddings(anyString(), eq(threshold), eq(limit)))
            .thenReturn(mockResults.subList(0, Math.min(limit, mockResults.size())));
        when(nodeRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(testNode));
        
        SearchResponseDTO response = vectorSearchService.findSimilar(queryText, threshold, limit);
        
        assertNotNull(response);
        assertEquals(limit, response.getPageSize());
        assertEquals(SearchResponseDTO.SearchType.VECTOR, response.getSearchType());
        
        verify(embeddingRepository).findSimilarEmbeddings(anyString(), eq(threshold), eq(limit));
        
        System.out.println("✓ findSimilar with custom parameters works correctly");
    }
    
    @Test
    @DisplayName("findSimilar - Empty results")
    void testFindSimilar_EmptyResults() {
        System.out.println("Testing findSimilar with empty results...");
        
        String queryText = "no matches";
        
        when(embeddingService.generateEmbedding(queryText)).thenReturn(testVector);
        when(embeddingRepository.findSimilarEmbeddings(anyString(), anyDouble(), anyInt()))
            .thenReturn(Collections.emptyList());
        
        SearchResponseDTO response = vectorSearchService.findSimilar(queryText, null, null);
        
        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertTrue(response.getResults().isEmpty());
        assertEquals(0.0, response.getMinScore());
        assertEquals(0.0, response.getMaxScore());
        
        System.out.println("✓ findSimilar handles empty results correctly");
    }
    
    @Test
    @DisplayName("findSimilarNodes - Success")
    void testFindSimilarNodes_Success() {
        System.out.println("Testing findSimilarNodes...");
        
        UUID nodeId = testNode.getId();
        int limit = 5;
        
        when(embeddingRepository.findByNode_Id(nodeId))
            .thenReturn(List.of(testEmbedding));
        when(embeddingRepository.findDiverseSimilarEmbeddings(anyString(), anyDouble(), anyInt()))
            .thenReturn(createMockVectorResultsWithNodeId(nodeId));
        when(nodeRepository.findById(nodeId))
            .thenReturn(Optional.of(testNode));
        when(nodeRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(testNode));
        
        SearchResponseDTO response = vectorSearchService.findSimilarNodes(nodeId, limit);
        
        System.out.println("Similar nodes found: " + response.getTotalElements());
        assertNotNull(response);
        assertTrue(response.getQuery().contains("Similar to"));
        assertEquals(SearchResponseDTO.SearchType.VECTOR, response.getSearchType());
        // Should filter out the source node
        assertTrue(response.getResults().stream().noneMatch(r -> r.getId().equals(nodeId)));
        
        System.out.println("✓ findSimilarNodes works correctly");
    }
    
    @Test
    @DisplayName("findSimilarNodes - No embeddings for node")
    void testFindSimilarNodes_NoEmbeddings() {
        System.out.println("Testing findSimilarNodes with no embeddings...");
        
        UUID nodeId = UUID.randomUUID();
        
        when(embeddingRepository.findByNode_Id(nodeId))
            .thenReturn(Collections.emptyList());
        
        SearchResponseDTO response = vectorSearchService.findSimilarNodes(nodeId, 10);
        
        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertTrue(response.getResults().isEmpty());
        assertEquals(SearchResponseDTO.SearchType.VECTOR, response.getSearchType());
        
        System.out.println("✓ findSimilarNodes handles missing embeddings correctly");
    }
    
    @Test
    @DisplayName("knnSearch - Success")
    void testKnnSearch_Success() {
        System.out.println("Testing k-NN search...");
        
        float[] queryVector = new float[]{0.3f, 0.4f, 0.5f, 0.6f, 0.7f};
        int k = 5;
        double threshold = 0.75;
        
        List<Object[]> mockResults = createMockVectorResults();
        
        when(embeddingRepository.findSimilarEmbeddings(anyString(), eq(threshold), eq(k)))
            .thenReturn(mockResults);
        when(nodeRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(testNode));
        
        SearchResponseDTO response = vectorSearchService.knnSearch(queryVector, k, threshold);
        
        System.out.println("k-NN search results: " + response.getTotalElements());
        assertNotNull(response);
        assertEquals("k-NN vector search", response.getQuery());
        assertEquals(SearchResponseDTO.SearchType.VECTOR, response.getSearchType());
        assertEquals(k, response.getPageSize());
        assertNotNull(response.getSearchTimeMs());
        
        System.out.println("✓ k-NN search works correctly");
    }
    
    @Test
    @DisplayName("getNodeVector - Vector exists")
    void testGetNodeVector_VectorExists() {
        System.out.println("Testing getNodeVector when vector exists...");
        
        UUID nodeId = testNode.getId();
        
        when(embeddingRepository.findByNode_Id(nodeId))
            .thenReturn(List.of(testEmbedding));
        
        float[] vector = vectorSearchService.getNodeVector(nodeId);
        
        assertNotNull(vector);
        assertArrayEquals(testVector, vector);
        
        verify(embeddingRepository).findByNode_Id(nodeId);
        verify(embeddingService, never()).generateEmbedding(anyString());
        
        System.out.println("✓ getNodeVector returns existing vector correctly");
    }
    
    @Test
    @DisplayName("getNodeVector - Generate new vector")
    void testGetNodeVector_GenerateNewVector() {
        System.out.println("Testing getNodeVector when vector needs to be generated...");
        
        UUID nodeId = testNode.getId();
        float[] newVector = new float[]{0.8f, 0.9f, 1.0f};
        
        when(embeddingRepository.findByNode_Id(nodeId))
            .thenReturn(Collections.emptyList());
        when(nodeRepository.findById(nodeId))
            .thenReturn(Optional.of(testNode));
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(newVector);
        
        float[] vector = vectorSearchService.getNodeVector(nodeId);
        
        assertNotNull(vector);
        assertArrayEquals(newVector, vector);
        
        verify(embeddingService).generateEmbedding(contains(testNode.getName()));
        
        System.out.println("✓ getNodeVector generates new vector when needed");
    }
    
    @Test
    @DisplayName("getNodeVector - Node not found")
    void testGetNodeVector_NodeNotFound() {
        System.out.println("Testing getNodeVector when node doesn't exist...");
        
        UUID nodeId = UUID.randomUUID();
        
        when(embeddingRepository.findByNode_Id(nodeId))
            .thenReturn(Collections.emptyList());
        when(nodeRepository.findById(nodeId))
            .thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.getNodeVector(nodeId);
        });
        
        System.out.println("✓ getNodeVector throws exception for non-existent node");
    }
    
    @Test
    @DisplayName("calculateSimilarity - Valid vectors")
    void testCalculateSimilarity_ValidVectors() {
        System.out.println("Testing calculateSimilarity with valid vectors...");
        
        float[] vector1 = new float[]{1.0f, 0.0f, 0.0f};
        float[] vector2 = new float[]{0.0f, 1.0f, 0.0f};
        
        double similarity = vectorSearchService.calculateSimilarity(vector1, vector2);
        
        System.out.println("Similarity score: " + similarity);
        assertTrue(similarity >= -1.0 && similarity <= 1.0);
        assertEquals(0.0, similarity, 0.001); // Orthogonal vectors
        
        // Test identical vectors
        double identicalSimilarity = vectorSearchService.calculateSimilarity(vector1, vector1);
        assertEquals(1.0, identicalSimilarity, 0.001);
        
        System.out.println("✓ calculateSimilarity computes correct cosine similarity");
    }
    
    @Test
    @DisplayName("calculateSimilarity - Different dimensions")
    void testCalculateSimilarity_DifferentDimensions() {
        System.out.println("Testing calculateSimilarity with different dimensions...");
        
        float[] vector1 = new float[]{1.0f, 0.0f};
        float[] vector2 = new float[]{0.0f, 1.0f, 0.0f};
        
        assertThrows(IllegalArgumentException.class, () -> {
            vectorSearchService.calculateSimilarity(vector1, vector2);
        });
        
        System.out.println("✓ calculateSimilarity throws exception for mismatched dimensions");
    }
    
    @Test
    @DisplayName("arrayToPostgresVector - Normal vector")
    void testArrayToPostgresVector_NormalVector() {
        System.out.println("Testing arrayToPostgresVector conversion...");
        
        float[] vector = new float[]{0.1f, 0.2f, 0.3f};
        String result = invokeArrayToPostgresVector(vector);
        
        assertEquals("[0.1,0.2,0.3]", result);
        
        System.out.println("✓ arrayToPostgresVector converts correctly");
    }
    
    @Test
    @DisplayName("arrayToPostgresVector - Empty vector")
    void testArrayToPostgresVector_EmptyVector() {
        System.out.println("Testing arrayToPostgresVector with empty vector...");
        
        float[] vector = new float[]{};
        String result = invokeArrayToPostgresVector(vector);
        
        assertEquals("[]", result);
        
        System.out.println("✓ arrayToPostgresVector handles empty vector");
    }
    
    @Test
    @DisplayName("arrayToPostgresVector - Null vector")
    void testArrayToPostgresVector_NullVector() {
        System.out.println("Testing arrayToPostgresVector with null vector...");
        
        String result = invokeArrayToPostgresVector(null);
        
        assertEquals("[]", result);
        
        System.out.println("✓ arrayToPostgresVector handles null vector");
    }
    
    @Test
    @DisplayName("findSimilar - Large result set")
    void testFindSimilar_LargeResultSet() {
        System.out.println("Testing findSimilar with large result set...");
        
        String queryText = "large dataset query";
        int limit = 100;
        
        List<Object[]> largeResults = createLargeMockVectorResults(limit);
        
        when(embeddingService.generateEmbedding(queryText)).thenReturn(testVector);
        when(embeddingRepository.findSimilarEmbeddings(anyString(), anyDouble(), eq(limit)))
            .thenReturn(largeResults);
        when(nodeRepository.findById(any(UUID.class)))
            .thenReturn(Optional.of(testNode));
        
        SearchResponseDTO response = vectorSearchService.findSimilar(queryText, null, limit);
        
        assertNotNull(response);
        assertEquals(limit, response.getTotalElements());
        assertEquals(limit, response.getResults().size());
        
        // Check scores are properly extracted
        assertTrue(response.getMaxScore() > response.getMinScore());
        
        System.out.println("✓ findSimilar handles large result sets correctly");
    }
    
    @Test
    @DisplayName("convertVectorResult - Null node handling")
    void testConvertVectorResult_NullNode() {
        System.out.println("Testing vector result conversion with null node...");
        
        Object[] row = new Object[]{
            UUID.randomUUID(),      // id
            null,                   // node_id (null)
            null,                   // document_id
            "content",              // content_snippet
            "v1",                   // model_version
            LocalDateTime.now(),    // created_at
            0.9                     // similarity
        };
        
        when(nodeRepository.findById(any())).thenReturn(Optional.empty());
        
        List<Object[]> results = List.of(row);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(testVector);
        when(embeddingRepository.findSimilarEmbeddings(anyString(), anyDouble(), anyInt()))
            .thenReturn(results);
        
        SearchResponseDTO response = vectorSearchService.findSimilar("test", null, null);
        
        // Should handle null node gracefully
        assertTrue(response.getResults().isEmpty());
        
        System.out.println("✓ Null node in vector results handled correctly");
    }
    
    // Helper methods
    
    private List<Object[]> createMockVectorResults() {
        List<Object[]> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            results.add(new Object[]{
                UUID.randomUUID(),                  // id
                UUID.randomUUID(),                  // node_id
                null,                                // document_id
                "Content snippet " + i,             // content_snippet
                "model-v1",                          // model_version
                LocalDateTime.now(),                 // created_at
                0.9 - (i * 0.1)                     // similarity
            });
        }
        return results;
    }
    
    private List<Object[]> createMockVectorResultsWithNodeId(UUID excludeNodeId) {
        List<Object[]> results = new ArrayList<>();
        // Add the node to be excluded
        results.add(new Object[]{
            UUID.randomUUID(),
            excludeNodeId,  // This should be filtered out
            null,
            "Source node content",
            "model-v1",
            LocalDateTime.now(),
            0.95
        });
        // Add other nodes
        for (int i = 0; i < 3; i++) {
            results.add(new Object[]{
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "Similar content " + i,
                "model-v1",
                LocalDateTime.now(),
                0.9 - (i * 0.1)
            });
        }
        return results;
    }
    
    private List<Object[]> createLargeMockVectorResults(int count) {
        List<Object[]> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(new Object[]{
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "Content snippet " + i,
                "model-v1",
                LocalDateTime.now(),
                1.0 - (i * 0.001)  // Decreasing similarity
            });
        }
        return results;
    }
    
    private String invokeArrayToPostgresVector(float[] vector) {
        // Use reflection to call the private method
        try {
            var method = VectorSearchService.class.getDeclaredMethod("arrayToPostgresVector", float[].class);
            method.setAccessible(true);
            return (String) method.invoke(vectorSearchService, (Object) vector);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}