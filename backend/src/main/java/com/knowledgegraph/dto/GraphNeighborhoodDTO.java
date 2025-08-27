package com.knowledgegraph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNeighborhoodDTO {
    private UUID centerNodeId;
    private Integer requestedHops;
    private Integer actualHops;
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    private Map<Integer, Integer> nodesPerHop; // hop level -> node count
    private Long totalNodes;
    private Long totalEdges;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphNode {
        private UUID id;
        private String type;
        private String name;
        private Map<String, Object> properties;
        private Integer hopLevel;
        private Double centrality; // betweenness centrality in subgraph
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphEdge {
        private UUID id;
        private UUID sourceId;
        private UUID targetId;
        private String type;
        private Map<String, Object> properties;
        private Integer hopLevel;
    }
}