package com.knowledgegraph.service;

import com.knowledgegraph.dto.SearchResponseDTO;
import com.knowledgegraph.dto.SearchResultDTO;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.DocumentRepository;
import com.knowledgegraph.repository.EdgeRepository;
import com.knowledgegraph.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService Tests")
class SearchServiceTest {

    @Mock
    private NodeRepository nodeRepository;
    
    @Mock
    private DocumentRepository documentRepository;
    
    @Mock
    private EdgeRepository edgeRepository;
    
    @Mock
    private JdbcTemplate jdbcTemplate;
    
    @InjectMocks
    private SearchService searchService;
    
    private Node testNode;
    private Document testDocument;
    private Pageable pageable;
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up SearchService test environment...");
        
        // Setup test data
        testNode = new Node();
        testNode.setId(UUID.randomUUID());
        testNode.setName("Test Node");
        testNode.setType(NodeType.PERSON);
        testNode.setSourceUri("http://example.com/test");
        testNode.setProperties(Map.of("key1", "value1", "key2", "value2"));
        testNode.setCreatedAt(LocalDateTime.now());
        testNode.setUpdatedAt(LocalDateTime.now());
        
        testDocument = new Document();
        testDocument.setId(UUID.randomUUID());
        testDocument.setUri("file://test.pdf");
        testDocument.setContent("Test document content with important information");
        testDocument.setContentType("application/pdf");
        testDocument.setMetadata(Map.of("author", "Test Author"));
        testDocument.setCreatedAt(LocalDateTime.now());
        testDocument.setUpdatedAt(LocalDateTime.now());
        
        pageable = PageRequest.of(0, 10);
    }
    
    @Test
    @DisplayName("searchNodes - Success with query and type filter")
    void testSearchNodes_SuccessWithTypeFilter() {
        System.out.println("Testing searchNodes with type filter...");
        
        String query = "test query";
        NodeType type = NodeType.PERSON;
        List<Node> nodes = List.of(testNode);
        Page<Node> nodePage = new PageImpl<>(nodes, pageable, nodes.size());
        
        when(nodeRepository.search(query, type.name(), pageable)).thenReturn(nodePage);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
            .thenReturn(1);
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(new ArrayList<>());
        
        SearchResponseDTO response = searchService.searchNodes(query, type, pageable);
        
        System.out.println("Response total elements: " + response.getTotalElements());
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(query, response.getQuery());
        assertEquals(SearchResponseDTO.SearchType.FULL_TEXT, response.getSearchType());
        assertNotNull(response.getSearchTimeMs());
        assertTrue(response.getSearchTimeMs() >= 0);
        
        verify(nodeRepository).search(query, type.name(), pageable);
        System.out.println("✓ searchNodes with type filter works correctly");
    }
    
    @Test
    @DisplayName("searchNodes - Success without type filter")
    void testSearchNodes_SuccessWithoutTypeFilter() {
        System.out.println("Testing searchNodes without type filter...");
        
        String query = "test";
        List<Node> nodes = List.of(testNode);
        Page<Node> nodePage = new PageImpl<>(nodes, pageable, nodes.size());
        
        when(nodeRepository.search(query, null, pageable)).thenReturn(nodePage);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
            .thenReturn(2);
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(new ArrayList<>());
        
        SearchResponseDTO response = searchService.searchNodes(query, null, pageable);
        
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(query, response.getQuery());
        assertNotNull(response.getTypeFacets());
        
        System.out.println("✓ searchNodes without type filter works correctly");
    }
    
    @Test
    @DisplayName("searchDocuments - Success")
    void testSearchDocuments_Success() {
        System.out.println("Testing searchDocuments...");
        
        String query = "important information";
        List<Document> documents = List.of(testDocument);
        Page<Document> documentPage = new PageImpl<>(documents, pageable, documents.size());
        
        when(documentRepository.searchByContent(query, pageable)).thenReturn(documentPage);
        
        SearchResponseDTO response = searchService.searchDocuments(query, pageable);
        
        System.out.println("Document search results: " + response.getTotalElements());
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(query, response.getQuery());
        assertEquals(SearchResponseDTO.SearchType.FULL_TEXT, response.getSearchType());
        assertFalse(response.getResults().isEmpty());
        
        SearchResultDTO firstResult = response.getResults().get(0);
        assertEquals(testDocument.getId(), firstResult.getId());
        assertEquals(testDocument.getUri(), firstResult.getTitle());
        assertNotNull(firstResult.getSnippet());
        
        System.out.println("✓ searchDocuments works correctly");
    }
    
    @Test
    @DisplayName("searchWithHighlight - Success with results")
    void testSearchWithHighlight_Success() {
        System.out.println("Testing searchWithHighlight...");
        
        String query = "test";
        List<SearchResultDTO> mockResults = List.of(
            SearchResultDTO.builder()
                .id(UUID.randomUUID())
                .type(NodeType.PERSON)
                .title("Test Person")
                .highlightedSnippet("<mark>Test</mark> Person with highlighted content")
                .score(0.95)
                .sourceUri("http://example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()
        );
        
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(mockResults);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
            .thenReturn(1L);
        
        SearchResponseDTO response = searchService.searchWithHighlight(query, pageable);
        
        System.out.println("Highlighted results count: " + response.getResults().size());
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getResults().size());
        assertNotNull(response.getResults().get(0).getHighlightedSnippet());
        assertTrue(response.getResults().get(0).getHighlightedSnippet().contains("mark"));
        
        System.out.println("✓ searchWithHighlight works correctly");
    }
    
    @Test
    @DisplayName("searchWithHighlight - Empty results")
    void testSearchWithHighlight_EmptyResults() {
        System.out.println("Testing searchWithHighlight with empty results...");
        
        String query = "nonexistent";
        
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(Collections.emptyList());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
            .thenReturn(0L);
        
        SearchResponseDTO response = searchService.searchWithHighlight(query, pageable);
        
        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertTrue(response.getResults().isEmpty());
        assertEquals(0, response.getTotalPages());
        
        System.out.println("✓ searchWithHighlight handles empty results correctly");
    }
    
    @Test
    @DisplayName("getSuggestedQueries - With synonyms")
    void testGetSuggestedQueries_WithSynonyms() {
        System.out.println("Testing getSuggestedQueries with synonyms...");
        
        String query = "person document";
        String[] suggestions = searchService.getSuggestedQueries(query);
        
        System.out.println("Suggestions: " + Arrays.toString(suggestions));
        assertNotNull(suggestions);
        assertTrue(suggestions.length > 0);
        assertTrue(suggestions.length <= 5);
        
        // Should include individual words and synonym replacements
        List<String> suggestionList = Arrays.asList(suggestions);
        assertTrue(suggestionList.contains("person") || suggestionList.contains("document"));
        
        System.out.println("✓ getSuggestedQueries generates correct suggestions");
    }
    
    @Test
    @DisplayName("getSuggestedQueries - Single word")
    void testGetSuggestedQueries_SingleWord() {
        System.out.println("Testing getSuggestedQueries with single word...");
        
        String query = "organization";
        String[] suggestions = searchService.getSuggestedQueries(query);
        
        assertNotNull(suggestions);
        assertTrue(suggestions.length > 0);
        
        // Should include synonyms for organization
        List<String> suggestionList = Arrays.asList(suggestions);
        assertTrue(suggestionList.stream().anyMatch(s -> 
            s.contains("company") || s.contains("business") || s.contains("corp")));
        
        System.out.println("✓ getSuggestedQueries handles single words correctly");
    }
    
    @Test
    @DisplayName("getSuggestedQueries - No synonyms available")
    void testGetSuggestedQueries_NoSynonyms() {
        System.out.println("Testing getSuggestedQueries without synonyms...");
        
        String query = "random words here";
        String[] suggestions = searchService.getSuggestedQueries(query);
        
        assertNotNull(suggestions);
        // Should at least split multi-word query
        assertTrue(suggestions.length > 0);
        
        System.out.println("✓ getSuggestedQueries handles queries without synonyms");
    }
    
    @Test
    @DisplayName("searchNodes - Large dataset pagination")
    void testSearchNodes_LargeDatasetPagination() {
        System.out.println("Testing searchNodes with large dataset pagination...");
        
        String query = "test";
        Pageable largePage = PageRequest.of(5, 50);
        
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Node node = new Node();
            node.setId(UUID.randomUUID());
            node.setName("Node " + i);
            node.setType(NodeType.DOCUMENT);
            nodes.add(node);
        }
        
        Page<Node> nodePage = new PageImpl<>(nodes, largePage, 1000);
        
        when(nodeRepository.search(query, null, largePage)).thenReturn(nodePage);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
            .thenReturn(5);
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(new ArrayList<>());
        
        SearchResponseDTO response = searchService.searchNodes(query, null, largePage);
        
        assertEquals(50, response.getResults().size());
        assertEquals(1000, response.getTotalElements());
        assertEquals(20, response.getTotalPages()); // 1000 / 50
        assertEquals(5, response.getCurrentPage());
        
        System.out.println("✓ searchNodes handles large dataset pagination correctly");
    }
    
    @Test
    @DisplayName("searchDocuments - Content snippet truncation")
    void testSearchDocuments_ContentSnippetTruncation() {
        System.out.println("Testing document content snippet truncation...");
        
        String query = "test";
        
        // Create document with long content
        Document longDoc = new Document();
        longDoc.setId(UUID.randomUUID());
        longDoc.setUri("file://long-content.txt");
        
        StringBuilder longContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longContent.append("This is line ").append(i).append(" of the document. ");
        }
        longDoc.setContent(longContent.toString());
        
        Page<Document> documentPage = new PageImpl<>(List.of(longDoc), pageable, 1);
        when(documentRepository.searchByContent(query, pageable)).thenReturn(documentPage);
        
        SearchResponseDTO response = searchService.searchDocuments(query, pageable);
        
        SearchResultDTO result = response.getResults().get(0);
        assertNotNull(result.getSnippet());
        assertTrue(result.getSnippet().endsWith("..."));
        assertTrue(result.getSnippet().length() <= 203); // 200 + "..."
        
        System.out.println("✓ Document content is correctly truncated for snippets");
    }
    
    @Test
    @DisplayName("searchNodes - Node without properties")
    void testSearchNodes_NodeWithoutProperties() {
        System.out.println("Testing searchNodes with node without properties...");
        
        Node simpleNode = new Node();
        simpleNode.setId(UUID.randomUUID());
        simpleNode.setName("Simple Node");
        simpleNode.setType(NodeType.CONCEPT);
        simpleNode.setProperties(null);
        
        Page<Node> nodePage = new PageImpl<>(List.of(simpleNode), pageable, 1);
        
        when(nodeRepository.search(anyString(), any(), any())).thenReturn(nodePage);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
            .thenReturn(0);
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(new ArrayList<>());
        
        SearchResponseDTO response = searchService.searchNodes("test", null, pageable);
        
        SearchResultDTO result = response.getResults().get(0);
        assertEquals("Simple Node", result.getSnippet());
        assertNotNull(result.getMetadata());
        
        System.out.println("✓ Nodes without properties are handled correctly");
    }
    
    @Test
    @DisplayName("searchWithHighlight - Database error handling")
    void testSearchWithHighlight_DatabaseError() {
        System.out.println("Testing searchWithHighlight database error handling...");
        
        String query = "test";
        
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenThrow(new RuntimeException("Database connection error"));
        
        assertThrows(RuntimeException.class, () -> {
            searchService.searchWithHighlight(query, pageable);
        });
        
        System.out.println("✓ Database errors are propagated correctly");
    }
    
    @Test
    @DisplayName("searchNodes - Performance with caching behavior")
    void testSearchNodes_CachingBehavior() {
        System.out.println("Testing searchNodes caching behavior simulation...");
        
        String query = "cached query";
        Page<Node> nodePage = new PageImpl<>(List.of(testNode), pageable, 1);
        
        when(nodeRepository.search(query, null, pageable)).thenReturn(nodePage);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(), any()))
            .thenReturn(1);
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(new ArrayList<>());
        
        // First call
        long startTime = System.currentTimeMillis();
        SearchResponseDTO response1 = searchService.searchNodes(query, null, pageable);
        long firstCallTime = System.currentTimeMillis() - startTime;
        
        // Second call (would be cached in production)
        startTime = System.currentTimeMillis();
        SearchResponseDTO response2 = searchService.searchNodes(query, null, pageable);
        long secondCallTime = System.currentTimeMillis() - startTime;
        
        assertEquals(response1.getTotalElements(), response2.getTotalElements());
        assertEquals(response1.getQuery(), response2.getQuery());
        
        // Verify repository was called twice (no caching in test)
        verify(nodeRepository, times(2)).search(query, null, pageable);
        
        System.out.println("First call time: " + firstCallTime + "ms");
        System.out.println("Second call time: " + secondCallTime + "ms");
        System.out.println("✓ Caching behavior simulation completed");
    }
    
    @Test
    @DisplayName("searchNodes - Edge case with special characters in query")
    void testSearchNodes_SpecialCharactersInQuery() {
        System.out.println("Testing searchNodes with special characters...");
        
        String query = "test & query | with <special> \"characters\"";
        Page<Node> nodePage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        
        when(nodeRepository.search(query, null, pageable)).thenReturn(nodePage);
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(new ArrayList<>());
        
        SearchResponseDTO response = searchService.searchNodes(query, null, pageable);
        
        assertNotNull(response);
        assertEquals(query, response.getQuery());
        assertEquals(0, response.getTotalElements());
        
        System.out.println("✓ Special characters in query handled correctly");
    }
    
    @Test
    @DisplayName("searchWithHighlight - Null score handling")
    void testSearchWithHighlight_NullScoreHandling() {
        System.out.println("Testing searchWithHighlight with null scores...");
        
        String query = "test";
        
        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
            .thenReturn(List.of(
                SearchResultDTO.builder()
                    .id(UUID.randomUUID())
                    .type(NodeType.CONCEPT)
                    .title("Test")
                    .score(null) // Null score
                    .build()
            ));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString()))
            .thenReturn(1L);
        
        SearchResponseDTO response = searchService.searchWithHighlight(query, pageable);
        
        assertNotNull(response);
        assertEquals(1, response.getResults().size());
        // Null score should be handled gracefully
        
        System.out.println("✓ Null scores handled correctly");
    }
}