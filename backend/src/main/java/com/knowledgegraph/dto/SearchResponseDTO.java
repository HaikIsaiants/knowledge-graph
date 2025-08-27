package com.knowledgegraph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDTO {
    private List<SearchResultDTO> results;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private String query;
    private SearchType searchType;
    private Long searchTimeMs;
    
    // Facets for filtering
    private Map<String, Long> typeFacets;
    private Map<String, Long> sourceFacets;
    
    // Search metadata
    private Double minScore;
    private Double maxScore;
    private String[] suggestedQueries;
    
    public enum SearchType {
        FULL_TEXT,
        VECTOR,
        HYBRID,
        GRAPH
    }
}