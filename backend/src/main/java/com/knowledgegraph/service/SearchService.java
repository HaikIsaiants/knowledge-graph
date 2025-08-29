package com.knowledgegraph.service;

import com.knowledgegraph.dto.SearchResponseDTO;
import com.knowledgegraph.dto.SearchResultDTO;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.DocumentRepository;
import com.knowledgegraph.repository.EdgeRepository;
import com.knowledgegraph.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SearchService {
    
    private final NodeRepository nodeRepository;
    private final DocumentRepository documentRepository;
    private final EdgeRepository edgeRepository;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Search nodes using full-text search
     */
    public SearchResponseDTO searchNodes(String query, NodeType type, Pageable pageable) {
        return executeTimedSearch(() -> {
            log.debug("Searching nodes with query: '{}', type: {}", query, type);
            
            Page<Node> results = nodeRepository.search(
                query, 
                type != null ? type.name() : null, 
                pageable
            );
            
            return buildSearchResponse(
                results.map(node -> convertToSearchResult(node, query)),
                query,
                SearchResponseDTO.SearchType.FULL_TEXT,
                getTypeFacets(query)
            );
        });
    }
    
    /**
     * Search documents using full-text search
     */
    public SearchResponseDTO searchDocuments(String query, Pageable pageable) {
        return executeTimedSearch(() -> {
            log.debug("Searching documents with query: '{}'", query);
            
            Page<Document> results = documentRepository.searchByContent(query, pageable);
            
            return buildSearchResponse(
                results.map(doc -> convertDocumentToSearchResult(doc, query)),
                query,
                SearchResponseDTO.SearchType.FULL_TEXT,
                null
            );
        });
    }
    
    /**
     * Search with highlighted snippets
     */
    public SearchResponseDTO searchWithHighlight(String query, Pageable pageable) {
        long startTime = System.currentTimeMillis();
        log.debug("Searching with highlights for query: '{}'", query);
        
        // SQL query using ts_headline for highlighting
        String sql = """
            SELECT n.id, n.type, n.name, n.properties, n.source_uri, 
                   n.created_at, n.updated_at,
                   ts_rank(to_tsvector('english', n.name || ' ' || COALESCE(n.properties::text, '')), plainto_tsquery('english', ?)) as score,
                   kg.search_with_highlight(?, n.name || ' ' || COALESCE(n.properties::text, '')) as snippet
            FROM kg.nodes n
            WHERE to_tsvector('english', n.name || ' ' || COALESCE(n.properties::text, '')) @@ plainto_tsquery('english', ?)
            ORDER BY score DESC
            LIMIT ? OFFSET ?
            """;
        
        List<SearchResultDTO> results = jdbcTemplate.query(
            sql,
            ps -> {
                ps.setString(1, query);
                ps.setString(2, query);
                ps.setString(3, query);
                ps.setInt(4, pageable.getPageSize());
                ps.setLong(5, pageable.getOffset());
            },
            (rs, rowNum) -> SearchResultDTO.builder()
                .id(UUID.fromString(rs.getString("id")))
                .type(NodeType.valueOf(rs.getString("type")))
                .title(rs.getString("name"))
                .highlightedSnippet(rs.getString("snippet"))
                .score(rs.getDouble("score"))
                .sourceUri(rs.getString("source_uri"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
                .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
                .build()
        );
        
        // Get total count
        String countSql = """
            SELECT COUNT(*) FROM kg.nodes n
            WHERE to_tsvector('english', n.name || ' ' || COALESCE(n.properties::text, '')) @@ plainto_tsquery('english', ?)
            """;
        Long totalCount = jdbcTemplate.queryForObject(countSql, Long.class, query);
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        return SearchResponseDTO.builder()
            .results(results)
            .totalElements(totalCount != null ? totalCount : 0)
            .totalPages((int) Math.ceil(totalCount / (double) pageable.getPageSize()))
            .currentPage(pageable.getPageNumber())
            .pageSize(pageable.getPageSize())
            .query(query)
            .searchType(SearchResponseDTO.SearchType.FULL_TEXT)
            .searchTimeMs(searchTime)
            .build();
    }
    
    /**
     * Get suggested queries based on current query
     */
    public String[] getSuggestedQueries(String query) {
        // Simplified synonym-based suggestions
        Map<String, List<String>> synonyms = Map.of(
            "person", List.of("people", "individual", "user"),
            "organization", List.of("company", "business", "corp"),
            "document", List.of("file", "paper", "report")
        );
        
        return Stream.concat(
            // Individual words from multi-word queries
            query.contains(" ") ? Arrays.stream(query.split("\\s+")) : Stream.empty(),
            // Synonym replacements
            synonyms.entrySet().stream()
                .filter(e -> query.toLowerCase().contains(e.getKey()))
                .flatMap(e -> e.getValue().stream()
                    .map(synonym -> query.toLowerCase().replace(e.getKey(), synonym)))
        )
        .distinct()
        .limit(5)
        .toArray(String[]::new);
    }
    
    /**
     * Convert Node to SearchResultDTO
     */
    private SearchResultDTO convertToSearchResult(Node node, String query) {
        // Get connection count
        Integer connectionCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM kg.edges WHERE source_id = ? OR target_id = ?",
            Integer.class,
            node.getId(), node.getId()
        );
        
        return SearchResultDTO.builder()
            .id(node.getId())
            .type(node.getType())
            .title(node.getName())
            .snippet(generateSnippet(node))
            .score(null) // Will be set if using ranking
            .sourceUri(node.getSourceUri())
            .metadata(node.getProperties())
            .createdAt(node.getCreatedAt())
            .updatedAt(node.getUpdatedAt())
            .connectionCount(connectionCount)
            .build();
    }
    
    /**
     * Convert Document to SearchResultDTO
     */
    private SearchResultDTO convertDocumentToSearchResult(Document doc, String query) {
        return SearchResultDTO.builder()
            .id(doc.getId())
            .title(doc.getUri())
            .snippet(doc.getContent() != null && doc.getContent().length() > 200 
                ? doc.getContent().substring(0, 200) + "..." 
                : doc.getContent())
            .sourceUri(doc.getUri())
            .contentType(doc.getContentType())
            .metadata(doc.getMetadata())
            .createdAt(doc.getCreatedAt())
            .updatedAt(doc.getUpdatedAt())
            .documentId(doc.getId().toString())
            .build();
    }
    
    /**
     * Generate snippet from node properties
     */
    private String generateSnippet(Node node) {
        if (node.getProperties() == null || node.getProperties().isEmpty()) {
            return node.getName();
        }
        
        String snippet = node.getProperties().entrySet().stream()
            .filter(e -> e.getValue() != null)
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(". ", "", "."));
        
        return snippet.length() > 200 ? snippet.substring(0, 200) + "..." : snippet;
    }
    
    /**
     * Get type facets for filtering
     */
    private Map<String, Long> getTypeFacets(String query) {
        String sql = """
            SELECT type, COUNT(*) as count
            FROM kg.nodes n
            WHERE to_tsvector('english', n.name || ' ' || COALESCE(n.properties::text, '')) @@ plainto_tsquery('english', ?)
            GROUP BY type
            """;
        
        Map<String, Long> facets = new HashMap<>();
        
        jdbcTemplate.query(sql, 
            ps -> ps.setString(1, query),
            rs -> {
                facets.put(rs.getString("type"), rs.getLong("count"));
            }
        );
        
        return facets;
    }
    
    /**
     * Helper method to execute search with timing
     */
    private SearchResponseDTO executeTimedSearch(java.util.function.Supplier<SearchResponseDTO> searchOperation) {
        long startTime = System.currentTimeMillis();
        SearchResponseDTO response = searchOperation.get();
        response.setSearchTimeMs(System.currentTimeMillis() - startTime);
        return response;
    }
    
    /**
     * Helper method to build search response from results
     */
    private SearchResponseDTO buildSearchResponse(
            Page<SearchResultDTO> results,
            String query,
            SearchResponseDTO.SearchType searchType,
            Map<String, Long> typeFacets) {
        
        return SearchResponseDTO.builder()
            .results(results.getContent())
            .totalElements(results.getTotalElements())
            .totalPages(results.getTotalPages())
            .currentPage(results.getNumber())
            .pageSize(results.getSize())
            .query(query)
            .searchType(searchType)
            .typeFacets(typeFacets)
            .build();
    }
}