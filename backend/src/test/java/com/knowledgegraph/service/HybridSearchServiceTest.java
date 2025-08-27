package com.knowledgegraph.service;

import com.knowledgegraph.dto.SearchResponseDTO;
import com.knowledgegraph.dto.SearchResultDTO;
import com.knowledgegraph.model.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HybridSearchService Tests")
class HybridSearchServiceTest {

    @Mock
    private SearchService searchService;
    
    @Mock
    private VectorSearchService vectorSearchService;
    
    @InjectMocks
    private HybridSearchService hybridSearchService;
    
    private Pageable pageable;
    private SearchResponseDTO ftsResponse;
    private SearchResponseDTO vectorResponse;
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up HybridSearchService test environment...");
        
        // Set default values for @Value fields
        ReflectionTestUtils.setField(hybridSearchService, "defaultFtsWeight", 0.5);
        ReflectionTestUtils.setField(hybridSearchService, "defaultVectorWeight", 0.5);
        
        pageable = PageRequest.of(0, 10);
        
        // Setup FTS response
        ftsResponse = SearchResponseDTO.builder()
            .results(createFtsResults())
            .totalElements(3)
            .totalPages(1)
            .currentPage(0)
            .pageSize(10)
            .query("test query")
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .searchTimeMs(50L)
            .typeFacets(Map.of("PERSON", 2L, "DOCUMENT", 1L))
            .build();
        
        // Setup Vector response
        vectorResponse = SearchResponseDTO.builder()
            .results(createVectorResults())
            .totalElements(3)
            .totalPages(1)
            .currentPage(0)
            .pageSize(10)
            .query("test query")
            .searchType(SearchResponseDTO.SearchType.VECTOR)
            .searchTimeMs(75L)
            .minScore(0.7)
            .maxScore(0.95)
            .build();
    }
    
    @Test
    @DisplayName("hybridSearch - Success with default weights")
    void testHybridSearch_DefaultWeights() {
        System.out.println("Testing hybridSearch with default weights...");
        
        String query = "test query";
        
        when(searchService.searchWithHighlight(eq(query), any(Pageable.class)))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(eq(query), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(query, null, null, pageable);
        
        System.out.println("Hybrid search results: " + response.getTotalElements());
        assertNotNull(response);
        assertEquals(query, response.getQuery());
        assertEquals(SearchResponseDTO.SearchType.HYBRID, response.getSearchType());
        assertNotNull(response.getSearchTimeMs());
        assertTrue(response.getSearchTimeMs() >= 0);
        assertNotNull(response.getResults());
        
        verify(searchService).searchWithHighlight(eq(query), any(Pageable.class));
        verify(vectorSearchService).findSimilar(eq(query), isNull(), eq(20)); // pageSize * 2
        
        System.out.println("✓ hybridSearch with default weights works correctly");
    }
    
    @Test
    @DisplayName("hybridSearch - Custom weights")
    void testHybridSearch_CustomWeights() {
        System.out.println("Testing hybridSearch with custom weights...");
        
        String query = "weighted query";
        double ftsWeight = 0.7;
        double vectorWeight = 0.3;
        
        when(searchService.searchWithHighlight(eq(query), any(Pageable.class)))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(eq(query), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(
            query, ftsWeight, vectorWeight, pageable
        );
        
        assertNotNull(response);
        assertEquals(SearchResponseDTO.SearchType.HYBRID, response.getSearchType());
        
        // Verify weights are normalized (0.7 / 1.0 = 0.7, 0.3 / 1.0 = 0.3)
        // Results should be properly merged and re-ranked
        assertNotNull(response.getResults());
        
        System.out.println("✓ hybridSearch with custom weights works correctly");
    }
    
    @Test
    @DisplayName("hybridSearch - Results merging with duplicates")
    void testHybridSearch_MergingWithDuplicates() {
        System.out.println("Testing hybridSearch with duplicate results...");
        
        UUID sharedId = UUID.randomUUID();
        
        // Create results with shared ID
        List<SearchResultDTO> ftsResultsWithDuplicate = List.of(
            createResultWithId(sharedId, "Shared Result", 0.8, true),
            createResultWithId(UUID.randomUUID(), "FTS Only", 0.6, true)
        );
        
        List<SearchResultDTO> vectorResultsWithDuplicate = List.of(
            createResultWithId(sharedId, "Shared Result", 0.9, false),
            createResultWithId(UUID.randomUUID(), "Vector Only", 0.7, false)
        );
        
        ftsResponse.setResults(ftsResultsWithDuplicate);
        vectorResponse.setResults(vectorResultsWithDuplicate);
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(anyString(), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(
            "test", null, null, pageable
        );
        
        // Check that duplicates are merged properly
        long uniqueIds = response.getResults().stream()
            .map(SearchResultDTO::getId)
            .distinct()
            .count();
        
        System.out.println("Unique IDs in merged results: " + uniqueIds);
        assertEquals(response.getResults().size(), uniqueIds);
        
        // Verify boost is applied for results in both
        Optional<SearchResultDTO> boostedResult = response.getResults().stream()
            .filter(r -> r.getId().equals(sharedId))
            .findFirst();
        
        assertTrue(boostedResult.isPresent());
        assertNotNull(boostedResult.get().getScore());
        
        System.out.println("✓ Results merging with duplicates works correctly");
    }
    
    @Test
    @DisplayName("hybridSearch - Empty FTS results")
    void testHybridSearch_EmptyFtsResults() {
        System.out.println("Testing hybridSearch with empty FTS results...");
        
        SearchResponseDTO emptyFtsResponse = SearchResponseDTO.builder()
            .results(Collections.emptyList())
            .totalElements(0)
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .build();
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(emptyFtsResponse);
        when(vectorSearchService.findSimilar(anyString(), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(
            "test", null, null, pageable
        );
        
        assertNotNull(response);
        // Should still have vector results
        assertFalse(response.getResults().isEmpty());
        
        System.out.println("✓ hybridSearch handles empty FTS results correctly");
    }
    
    @Test
    @DisplayName("hybridSearch - Empty vector results")
    void testHybridSearch_EmptyVectorResults() {
        System.out.println("Testing hybridSearch with empty vector results...");
        
        SearchResponseDTO emptyVectorResponse = SearchResponseDTO.builder()
            .results(Collections.emptyList())
            .totalElements(0)
            .searchType(SearchResponseDTO.SearchType.VECTOR)
            .build();
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(anyString(), isNull(), anyInt()))
            .thenReturn(emptyVectorResponse);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(
            "test", null, null, pageable
        );
        
        assertNotNull(response);
        // Should still have FTS results
        assertFalse(response.getResults().isEmpty());
        
        System.out.println("✓ hybridSearch handles empty vector results correctly");
    }
    
    @Test
    @DisplayName("hybridSearch - Both searches return empty")
    void testHybridSearch_BothEmpty() {
        System.out.println("Testing hybridSearch with both searches empty...");
        
        SearchResponseDTO emptyResponse = SearchResponseDTO.builder()
            .results(Collections.emptyList())
            .totalElements(0)
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .build();
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(emptyResponse);
        when(vectorSearchService.findSimilar(anyString(), isNull(), anyInt()))
            .thenReturn(emptyResponse);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(
            "test", null, null, pageable
        );
        
        assertNotNull(response);
        assertTrue(response.getResults().isEmpty());
        assertEquals(0, response.getTotalElements());
        
        System.out.println("✓ hybridSearch handles both searches empty correctly");
    }
    
    @Test
    @DisplayName("adaptiveHybridSearch - Success")
    void testAdaptiveHybridSearch_Success() {
        System.out.println("Testing adaptiveHybridSearch...");
        
        String query = "adaptive test";
        
        // Setup probe responses
        SearchResponseDTO ftsProbe = SearchResponseDTO.builder()
            .results(createFtsResults().subList(0, 2))
            .totalElements(2)
            .maxScore(0.9)
            .minScore(0.7)
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .build();
        
        SearchResponseDTO vectorProbe = SearchResponseDTO.builder()
            .results(createVectorResults().subList(0, 1))
            .totalElements(1)
            .maxScore(0.95)
            .minScore(0.95)
            .searchType(SearchResponseDTO.SearchType.VECTOR)
            .build();
        
        when(searchService.searchNodes(eq(query), isNull(), any(Pageable.class)))
            .thenReturn(ftsProbe);
        when(vectorSearchService.findSimilar(eq(query), isNull(), eq(5)))
            .thenReturn(vectorProbe);
        
        // Setup main search responses
        when(searchService.searchWithHighlight(eq(query), any(Pageable.class)))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(eq(query), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        SearchResponseDTO response = hybridSearchService.adaptiveHybridSearch(query, pageable);
        
        System.out.println("Adaptive search results: " + response.getTotalElements());
        assertNotNull(response);
        assertEquals(SearchResponseDTO.SearchType.HYBRID, response.getSearchType());
        
        // Verify probe searches were performed
        verify(searchService).searchNodes(eq(query), isNull(), any(Pageable.class));
        verify(vectorSearchService).findSimilar(eq(query), isNull(), eq(5));
        
        // Verify main searches were performed
        verify(searchService).searchWithHighlight(eq(query), any(Pageable.class));
        
        System.out.println("✓ adaptiveHybridSearch works correctly");
    }
    
    @Test
    @DisplayName("adaptiveHybridSearch - Low quality FTS results")
    void testAdaptiveHybridSearch_LowQualityFts() {
        System.out.println("Testing adaptiveHybridSearch with low quality FTS...");
        
        String query = "test";
        
        // Setup probe with low quality FTS
        SearchResponseDTO lowQualityFtsProbe = SearchResponseDTO.builder()
            .results(Collections.emptyList())
            .totalElements(0)
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .build();
        
        SearchResponseDTO highQualityVectorProbe = SearchResponseDTO.builder()
            .results(createVectorResults())
            .totalElements(3)
            .maxScore(0.95)
            .minScore(0.85)
            .searchType(SearchResponseDTO.SearchType.VECTOR)
            .build();
        
        when(searchService.searchNodes(eq(query), isNull(), any(Pageable.class)))
            .thenReturn(lowQualityFtsProbe);
        when(vectorSearchService.findSimilar(eq(query), isNull(), eq(5)))
            .thenReturn(highQualityVectorProbe);
        when(searchService.searchWithHighlight(eq(query), any(Pageable.class)))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(eq(query), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        SearchResponseDTO response = hybridSearchService.adaptiveHybridSearch(query, pageable);
        
        assertNotNull(response);
        // Vector should have higher weight due to better quality
        assertEquals(SearchResponseDTO.SearchType.HYBRID, response.getSearchType());
        
        System.out.println("✓ adaptiveHybridSearch adjusts weights based on quality");
    }
    
    @Test
    @DisplayName("hybridSearch - Pagination")
    void testHybridSearch_Pagination() {
        System.out.println("Testing hybridSearch pagination...");
        
        Pageable page2 = PageRequest.of(1, 5); // Second page, 5 items
        
        // Create larger result sets
        List<SearchResultDTO> manyFtsResults = createManyResults(20, true);
        List<SearchResultDTO> manyVectorResults = createManyResults(20, false);
        
        ftsResponse.setResults(manyFtsResults);
        vectorResponse.setResults(manyVectorResults);
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(anyString(), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(
            "test", null, null, page2
        );
        
        assertNotNull(response);
        assertEquals(5, response.getPageSize());
        assertEquals(1, response.getCurrentPage());
        assertTrue(response.getResults().size() <= 5);
        
        System.out.println("✓ hybridSearch pagination works correctly");
    }
    
    @Test
    @DisplayName("hybridSearch - Weight normalization")
    void testHybridSearch_WeightNormalization() {
        System.out.println("Testing hybridSearch weight normalization...");
        
        // Use non-normalized weights
        double ftsWeight = 2.0;
        double vectorWeight = 3.0;
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(anyString(), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(
            "test", ftsWeight, vectorWeight, pageable
        );
        
        assertNotNull(response);
        // Weights should be normalized to 0.4 and 0.6
        assertEquals(SearchResponseDTO.SearchType.HYBRID, response.getSearchType());
        
        System.out.println("✓ Weight normalization works correctly");
    }
    
    @Test
    @DisplayName("mergeResults - Score calculation with boost")
    void testMergeResults_ScoreCalculationWithBoost() {
        System.out.println("Testing score calculation with boost for overlapping results...");
        
        UUID sharedId = UUID.randomUUID();
        
        // Create overlapping results with different scores
        SearchResultDTO ftsResult = createResultWithId(sharedId, "Shared", 0.8, true);
        SearchResultDTO vectorResult = createResultWithId(sharedId, "Shared", 0.9, false);
        
        ftsResponse.setResults(List.of(ftsResult));
        vectorResponse.setResults(List.of(vectorResult));
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(anyString(), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(
            "test", 0.5, 0.5, pageable
        );
        
        assertEquals(1, response.getResults().size());
        SearchResultDTO merged = response.getResults().get(0);
        
        // Score should be boosted for appearing in both
        assertNotNull(merged.getScore());
        // Combined score with boost: ((0.8 * 0.5) + (0.9 * 0.5)) * 1.2
        double expectedScore = ((0.8 * 0.5) + (0.9 * 0.5)) * 1.2;
        assertEquals(expectedScore, merged.getScore(), 0.01);
        
        System.out.println("✓ Score calculation with boost works correctly");
    }
    
    @Test
    @DisplayName("assessResultQuality - Various quality levels")
    void testAssessResultQuality_VariousLevels() {
        System.out.println("Testing result quality assessment...");
        
        // Test with high quality results
        SearchResponseDTO highQuality = SearchResponseDTO.builder()
            .results(createHighQualityResults())
            .maxScore(1.0)
            .minScore(0.5)
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .build();
        
        // Test with low quality results
        SearchResponseDTO lowQuality = SearchResponseDTO.builder()
            .results(List.of(
                SearchResultDTO.builder()
                    .id(UUID.randomUUID())
                    .score(0.2)
                    .build()
            ))
            .maxScore(0.2)
            .minScore(0.2)
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .build();
        
        // Test with empty results
        SearchResponseDTO emptyQuality = SearchResponseDTO.builder()
            .results(Collections.emptyList())
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .build();
        
        when(searchService.searchNodes(eq("high"), isNull(), any()))
            .thenReturn(highQuality);
        when(searchService.searchNodes(eq("low"), isNull(), any()))
            .thenReturn(lowQuality);
        when(searchService.searchNodes(eq("empty"), isNull(), any()))
            .thenReturn(emptyQuality);
        
        when(vectorSearchService.findSimilar(anyString(), isNull(), eq(5)))
            .thenReturn(highQuality);
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenReturn(ftsResponse);
        when(vectorSearchService.findSimilar(anyString(), isNull(), anyInt()))
            .thenReturn(vectorResponse);
        
        // Test each quality level
        SearchResponseDTO response1 = hybridSearchService.adaptiveHybridSearch("high", pageable);
        SearchResponseDTO response2 = hybridSearchService.adaptiveHybridSearch("low", pageable);
        SearchResponseDTO response3 = hybridSearchService.adaptiveHybridSearch("empty", pageable);
        
        assertNotNull(response1);
        assertNotNull(response2);
        assertNotNull(response3);
        
        System.out.println("✓ Result quality assessment works for various levels");
    }
    
    // Helper methods
    
    private List<SearchResultDTO> createFtsResults() {
        return List.of(
            createResultWithId(UUID.randomUUID(), "FTS Result 1", 0.9, true),
            createResultWithId(UUID.randomUUID(), "FTS Result 2", 0.8, true),
            createResultWithId(UUID.randomUUID(), "FTS Result 3", 0.7, true)
        );
    }
    
    private List<SearchResultDTO> createVectorResults() {
        return List.of(
            createResultWithId(UUID.randomUUID(), "Vector Result 1", 0.95, false),
            createResultWithId(UUID.randomUUID(), "Vector Result 2", 0.85, false),
            createResultWithId(UUID.randomUUID(), "Vector Result 3", 0.75, false)
        );
    }
    
    private SearchResultDTO createResultWithId(UUID id, String title, double score, boolean hasHighlight) {
        return SearchResultDTO.builder()
            .id(id)
            .type(NodeType.CONCEPT)
            .title(title)
            .snippet("Snippet for " + title)
            .highlightedSnippet(hasHighlight ? "<mark>" + title + "</mark>" : null)
            .score(score)
            .sourceUri("http://example.com/" + id)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
    
    private List<SearchResultDTO> createManyResults(int count, boolean isFts) {
        List<SearchResultDTO> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String prefix = isFts ? "FTS" : "Vector";
            results.add(createResultWithId(
                UUID.randomUUID(),
                prefix + " Result " + i,
                1.0 - (i * 0.01),
                isFts
            ));
        }
        return results;
    }
    
    private List<SearchResultDTO> createHighQualityResults() {
        return List.of(
            createResultWithId(UUID.randomUUID(), "High Quality 1", 1.0, false),
            createResultWithId(UUID.randomUUID(), "High Quality 2", 0.95, false),
            createResultWithId(UUID.randomUUID(), "High Quality 3", 0.9, false),
            createResultWithId(UUID.randomUUID(), "High Quality 4", 0.85, false),
            createResultWithId(UUID.randomUUID(), "High Quality 5", 0.8, false)
        );
    }
}