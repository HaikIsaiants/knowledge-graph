package com.knowledgegraph.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.knowledgegraph.model.NodeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultDTO {
    private UUID id;
    private NodeType type;
    private String title;
    private String snippet;
    private String highlightedSnippet;
    private Double score;
    private String sourceUri;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Additional fields for different result types
    private String documentId;
    private Integer pageNumber;
    private String contentType;
    
    // For graph context
    private Integer connectionCount;
    private String[] relatedTypes;
}