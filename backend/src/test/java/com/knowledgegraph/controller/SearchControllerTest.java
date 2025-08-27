package com.knowledgegraph.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgegraph.dto.SearchResponseDTO;
import com.knowledgegraph.dto.SearchResultDTO;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.service.HybridSearchService;
import com.knowledgegraph.service.SearchService;
import com.knowledgegraph.service.VectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@DisplayName("SearchController Tests")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private SearchService searchService;
    
    @MockBean
    private VectorSearchService vectorSearchService;
    
    @MockBean
    private HybridSearchService hybridSearchService;
    
    private SearchResponseDTO mockSearchResponse;
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up SearchController test environment...");
        
        // Create mock search response
        List<SearchResultDTO> results = List.of(
            SearchResultDTO.builder()
                .id(UUID.randomUUID())
                .type(NodeType.PERSON)
                .title("John Doe")
                .snippet("Software Engineer at Example Corp")
                .highlightedSnippet("<mark>John Doe</mark> - Software Engineer")
                .score(0.95)
                .sourceUri("http://example.com/john")
                .metadata(Map.of("department", "Engineering"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build(),
            SearchResultDTO.builder()
                .id(UUID.randomUUID())
                .type(NodeType.DOCUMENT)
                .title("Technical Documentation")
                .snippet("API documentation for the system")
                .score(0.85)
                .sourceUri("http://example.com/docs")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        );
        
        mockSearchResponse = SearchResponseDTO.builder()
            .results(results)
            .totalElements(2)
            .totalPages(1)
            .currentPage(0)
            .pageSize(10)
            .query("test query")
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .searchTimeMs(50L)
            .typeFacets(Map.of("PERSON", 1L, "DOCUMENT", 1L))
            .minScore(0.85)
            .maxScore(0.95)
            .build();
    }
    
    @Test
    @DisplayName("GET /search - Full-text search with highlighting")
    void testSearch_WithHighlighting() throws Exception {
        System.out.println("Testing full-text search with highlighting...");
        
        when(searchService.searchWithHighlight(eq("john"), any(Pageable.class)))
            .thenReturn(mockSearchResponse);
        
        mockMvc.perform(get("/search")
                .param("q", "john")
                .param("page", "0")
                .param("size", "10")
                .param("highlight", "true"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.query").value("test query"))
            .andExpect(jsonPath("$.searchType").value("FULL_TEXT"))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.results", hasSize(2)))
            .andExpect(jsonPath("$.results[0].title").value("John Doe"))
            .andExpect(jsonPath("$.results[0].highlightedSnippet").exists())
            .andExpect(jsonPath("$.searchTimeMs").isNumber());
        
        verify(searchService).searchWithHighlight(eq("john"), any(Pageable.class));
        
        System.out.println("✓ Full-text search with highlighting works correctly");
    }
    
    @Test
    @DisplayName("GET /search - Search with type filter")
    void testSearch_WithTypeFilter() throws Exception {
        System.out.println("Testing search with type filter...");
        
        when(searchService.searchNodes(eq("person"), eq(NodeType.PERSON), any(Pageable.class)))
            .thenReturn(mockSearchResponse);
        
        mockMvc.perform(get("/search")
                .param("q", "person")
                .param("type", "PERSON")
                .param("highlight", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.query").value("test query"))
            .andExpect(jsonPath("$.typeFacets.PERSON").value(1));
        
        verify(searchService).searchNodes(eq("person"), eq(NodeType.PERSON), any(Pageable.class));
        
        System.out.println("✓ Search with type filter works correctly");
    }
    
    @Test
    @DisplayName("GET /search - Missing query parameter")
    void testSearch_MissingQuery() throws Exception {
        System.out.println("Testing search with missing query parameter...");
        
        mockMvc.perform(get("/search"))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Missing query parameter properly rejected");
    }
    
    @Test
    @DisplayName("GET /search/vector - Vector similarity search")
    void testVectorSearch_Success() throws Exception {
        System.out.println("Testing vector similarity search...");
        
        SearchResponseDTO vectorResponse = SearchResponseDTO.builder()
            .results(mockSearchResponse.getResults())
            .totalElements(2)
            .query("vector query")
            .searchType(SearchResponseDTO.SearchType.VECTOR)
            .searchTimeMs(75L)
            .minScore(0.7)
            .maxScore(0.95)
            .build();
        
        when(vectorSearchService.findSimilar(eq("machine learning"), eq(0.8), eq(20)))
            .thenReturn(vectorResponse);
        
        mockMvc.perform(get("/search/vector")
                .param("q", "machine learning")
                .param("threshold", "0.8")
                .param("limit", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.searchType").value("VECTOR"))
            .andExpect(jsonPath("$.minScore").value(0.7))
            .andExpect(jsonPath("$.maxScore").value(0.95));
        
        verify(vectorSearchService).findSimilar("machine learning", 0.8, 20);
        
        System.out.println("✓ Vector search works correctly");
    }
    
    @Test
    @DisplayName("GET /search/vector - Default parameters")
    void testVectorSearch_DefaultParams() throws Exception {
        System.out.println("Testing vector search with default parameters...");
        
        when(vectorSearchService.findSimilar(anyString(), isNull(), eq(10)))
            .thenReturn(mockSearchResponse);
        
        mockMvc.perform(get("/search/vector")
                .param("q", "test"))
            .andExpect(status().isOk());
        
        verify(vectorSearchService).findSimilar("test", null, 10);
        
        System.out.println("✓ Vector search with defaults works correctly");
    }
    
    @Test
    @DisplayName("GET /search/hybrid - Hybrid search")
    void testHybridSearch_Success() throws Exception {
        System.out.println("Testing hybrid search...");
        
        SearchResponseDTO hybridResponse = SearchResponseDTO.builder()
            .results(mockSearchResponse.getResults())
            .totalElements(2)
            .query("hybrid query")
            .searchType(SearchResponseDTO.SearchType.HYBRID)
            .searchTimeMs(100L)
            .build();
        
        when(hybridSearchService.hybridSearch(eq("test"), eq(0.6), eq(0.4), any(Pageable.class)))
            .thenReturn(hybridResponse);
        
        mockMvc.perform(get("/search/hybrid")
                .param("q", "test")
                .param("ftsWeight", "0.6")
                .param("vectorWeight", "0.4")
                .param("page", "0")
                .param("size", "15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.searchType").value("HYBRID"))
            .andExpect(jsonPath("$.query").value("hybrid query"));
        
        verify(hybridSearchService).hybridSearch(eq("test"), eq(0.6), eq(0.4), any(Pageable.class));
        
        System.out.println("✓ Hybrid search works correctly");
    }
    
    @Test
    @DisplayName("GET /search/hybrid - Default weights")
    void testHybridSearch_DefaultWeights() throws Exception {
        System.out.println("Testing hybrid search with default weights...");
        
        when(hybridSearchService.hybridSearch(anyString(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(mockSearchResponse);
        
        mockMvc.perform(get("/search/hybrid")
                .param("q", "test"))
            .andExpect(status().isOk());
        
        verify(hybridSearchService).hybridSearch(eq("test"), isNull(), isNull(), any(Pageable.class));
        
        System.out.println("✓ Hybrid search with default weights works correctly");
    }
    
    @Test
    @DisplayName("GET /search/adaptive - Adaptive hybrid search")
    void testAdaptiveSearch_Success() throws Exception {
        System.out.println("Testing adaptive hybrid search...");
        
        SearchResponseDTO adaptiveResponse = SearchResponseDTO.builder()
            .results(mockSearchResponse.getResults())
            .totalElements(2)
            .query("adaptive query")
            .searchType(SearchResponseDTO.SearchType.HYBRID)
            .searchTimeMs(150L)
            .build();
        
        when(hybridSearchService.adaptiveHybridSearch(eq("test"), any(Pageable.class)))
            .thenReturn(adaptiveResponse);
        
        mockMvc.perform(get("/search/adaptive")
                .param("q", "test")
                .param("page", "1")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.searchType").value("HYBRID"))
            .andExpect(jsonPath("$.query").value("adaptive query"));
        
        verify(hybridSearchService).adaptiveHybridSearch(eq("test"), any(Pageable.class));
        
        System.out.println("✓ Adaptive search works correctly");
    }
    
    @Test
    @DisplayName("GET /search/similar/{nodeId} - Find similar nodes")
    void testFindSimilarNodes_Success() throws Exception {
        System.out.println("Testing find similar nodes...");
        
        UUID nodeId = UUID.randomUUID();
        
        SearchResponseDTO similarResponse = SearchResponseDTO.builder()
            .results(mockSearchResponse.getResults())
            .totalElements(2)
            .query("Similar to node " + nodeId)
            .searchType(SearchResponseDTO.SearchType.VECTOR)
            .searchTimeMs(60L)
            .build();
        
        when(vectorSearchService.findSimilarNodes(eq(nodeId), eq(15)))
            .thenReturn(similarResponse);
        
        mockMvc.perform(get("/search/similar/{nodeId}", nodeId)
                .param("limit", "15"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.searchType").value("VECTOR"))
            .andExpect(jsonPath("$.query", containsString("Similar to node")));
        
        verify(vectorSearchService).findSimilarNodes(nodeId, 15);
        
        System.out.println("✓ Find similar nodes works correctly");
    }
    
    @Test
    @DisplayName("GET /search/similar/{nodeId} - Invalid UUID")
    void testFindSimilarNodes_InvalidUUID() throws Exception {
        System.out.println("Testing find similar with invalid UUID...");
        
        mockMvc.perform(get("/search/similar/invalid-uuid"))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Invalid UUID properly rejected");
    }
    
    @Test
    @DisplayName("GET /search/suggest - Get search suggestions")
    void testGetSuggestions_Success() throws Exception {
        System.out.println("Testing search suggestions...");
        
        String[] suggestions = {"person", "personnel", "personal", "personality", "personalize"};
        
        when(searchService.getSuggestedQueries("pers"))
            .thenReturn(suggestions);
        
        mockMvc.perform(get("/search/suggest")
                .param("q", "pers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            .andExpect(jsonPath("$[0]").value("person"))
            .andExpect(jsonPath("$[1]").value("personnel"));
        
        verify(searchService).getSuggestedQueries("pers");
        
        System.out.println("✓ Search suggestions work correctly");
    }
    
    @Test
    @DisplayName("GET /search/documents - Search documents")
    void testSearchDocuments_Success() throws Exception {
        System.out.println("Testing document search...");
        
        SearchResponseDTO documentResponse = SearchResponseDTO.builder()
            .results(List.of(mockSearchResponse.getResults().get(1))) // Only document result
            .totalElements(1)
            .query("document query")
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .searchTimeMs(40L)
            .build();
        
        when(searchService.searchDocuments(eq("API"), any(Pageable.class)))
            .thenReturn(documentResponse);
        
        mockMvc.perform(get("/search/documents")
                .param("q", "API")
                .param("page", "0")
                .param("size", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.query").value("document query"))
            .andExpect(jsonPath("$.results", hasSize(1)))
            .andExpect(jsonPath("$.results[0].type").value("DOCUMENT"));
        
        verify(searchService).searchDocuments(eq("API"), any(Pageable.class));
        
        System.out.println("✓ Document search works correctly");
    }
    
    @Test
    @DisplayName("Pagination - Large page request")
    void testPagination_LargePage() throws Exception {
        System.out.println("Testing large page request...");
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(mockSearchResponse);
        
        mockMvc.perform(get("/search")
                .param("q", "test")
                .param("page", "100")
                .param("size", "50"))
            .andExpect(status().isOk());
        
        verify(searchService).searchWithHighlight(eq("test"), argThat(pageable -> 
            pageable.getPageNumber() == 100 && pageable.getPageSize() == 50
        ));
        
        System.out.println("✓ Large page request handled correctly");
    }
    
    @Test
    @DisplayName("Error handling - Service exception")
    void testErrorHandling_ServiceException() throws Exception {
        System.out.println("Testing error handling for service exception...");
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenThrow(new RuntimeException("Database error"));
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isInternalServerError());
        
        System.out.println("✓ Service exception handled correctly");
    }
    
    @Test
    @DisplayName("Special characters in query")
    void testSpecialCharacters_InQuery() throws Exception {
        System.out.println("Testing special characters in query...");
        
        String specialQuery = "test & query | with <special> \"characters\"";
        
        when(searchService.searchWithHighlight(eq(specialQuery), any(Pageable.class)))
            .thenReturn(mockSearchResponse);
        
        mockMvc.perform(get("/search")
                .param("q", specialQuery))
            .andExpect(status().isOk());
        
        verify(searchService).searchWithHighlight(eq(specialQuery), any(Pageable.class));
        
        System.out.println("✓ Special characters in query handled correctly");
    }
    
    @Test
    @DisplayName("Empty results handling")
    void testEmptyResults_Handling() throws Exception {
        System.out.println("Testing empty results handling...");
        
        SearchResponseDTO emptyResponse = SearchResponseDTO.builder()
            .results(Collections.emptyList())
            .totalElements(0)
            .totalPages(0)
            .currentPage(0)
            .pageSize(10)
            .query("no results")
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .searchTimeMs(10L)
            .build();
        
        when(searchService.searchWithHighlight(eq("nonexistent"), any(Pageable.class)))
            .thenReturn(emptyResponse);
        
        mockMvc.perform(get("/search")
                .param("q", "nonexistent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.results", hasSize(0)));
        
        System.out.println("✓ Empty results handled correctly");
    }
    
    @Test
    @DisplayName("Concurrent requests handling")
    void testConcurrentRequests() throws Exception {
        System.out.println("Testing concurrent request handling...");
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(mockSearchResponse);
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<MvcResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            final int index = i;
            CompletableFuture<MvcResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return mockMvc.perform(get("/search")
                            .param("q", "concurrent" + index))
                        .andExpect(status().isOk())
                        .andReturn();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        
        verify(searchService, times(10)).searchWithHighlight(anyString(), any(Pageable.class));
        
        System.out.println("✓ Concurrent requests handled correctly");
    }
    
    @Test
    @DisplayName("Parameter validation - Invalid weights")
    void testParameterValidation_InvalidWeights() throws Exception {
        System.out.println("Testing invalid weight parameters...");
        
        // Negative weight
        mockMvc.perform(get("/search/hybrid")
                .param("q", "test")
                .param("ftsWeight", "-0.5"))
            .andExpect(status().isOk()); // Controller accepts, service should validate
        
        // Weight > 1
        mockMvc.perform(get("/search/hybrid")
                .param("q", "test")
                .param("vectorWeight", "1.5"))
            .andExpect(status().isOk()); // Controller accepts, service should validate
        
        System.out.println("✓ Weight parameter validation delegated to service");
    }
    
    @Test
    @DisplayName("Response headers validation")
    void testResponseHeaders() throws Exception {
        System.out.println("Testing response headers...");
        
        when(searchService.searchWithHighlight(anyString(), any(Pageable.class)))
            .thenReturn(mockSearchResponse);
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("application/json")));
        
        System.out.println("✓ Response headers are correct");
    }
    
    @Test
    @DisplayName("Performance - Large result set")
    void testPerformance_LargeResultSet() throws Exception {
        System.out.println("Testing performance with large result set...");
        
        // Create large result set
        List<SearchResultDTO> largeResults = IntStream.range(0, 100)
            .mapToObj(i -> SearchResultDTO.builder()
                .id(UUID.randomUUID())
                .type(NodeType.CONCEPT)
                .title("Result " + i)
                .snippet("Content " + i)
                .score(1.0 - (i * 0.01))
                .build())
            .toList();
        
        SearchResponseDTO largeResponse = SearchResponseDTO.builder()
            .results(largeResults)
            .totalElements(1000)
            .totalPages(10)
            .currentPage(0)
            .pageSize(100)
            .query("large query")
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .searchTimeMs(200L)
            .build();
        
        when(searchService.searchWithHighlight(eq("large"), any(Pageable.class)))
            .thenReturn(largeResponse);
        
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/search")
                .param("q", "large")
                .param("size", "100"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results", hasSize(100)));
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Large result set handling time: " + duration + "ms");
        assertThat(duration).isLessThan(1000L);
        
        System.out.println("✓ Large result set handled efficiently");
    }
}