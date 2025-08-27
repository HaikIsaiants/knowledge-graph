package com.knowledgegraph.dto;

import com.knowledgegraph.model.NodeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeDetailDTO {
    private UUID id;
    private NodeType type;
    private String name;
    private Map<String, Object> properties;
    private String sourceUri;
    private LocalDateTime capturedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Related information
    private List<EdgeDTO> outgoingEdges;
    private List<EdgeDTO> incomingEdges;
    private List<CitationDTO> citations;
    private List<EmbeddingDTO> embeddings;
    
    // Statistics
    private Integer totalConnections;
    private Map<String, Integer> connectionsByType;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeDTO {
        private UUID id;
        private UUID nodeId;
        private String nodeName;
        private NodeType nodeType;
        private String edgeType;
        private Map<String, Object> properties;
        private String direction; // "outgoing" or "incoming"
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CitationDTO {
        private UUID documentId;
        private String documentUri;
        private String contentSnippet;
        private Integer startOffset;
        private Integer endOffset;
        private Integer pageNumber;
        private LocalDateTime extractedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbeddingDTO {
        private UUID id;
        private String contentSnippet;
        private String modelVersion;
        private LocalDateTime createdAt;
    }
}