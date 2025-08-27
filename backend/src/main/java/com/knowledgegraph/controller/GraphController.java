package com.knowledgegraph.controller;

import com.knowledgegraph.dto.GraphNeighborhoodDTO;
import com.knowledgegraph.service.GraphTraversalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/graph")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Graph", description = "Graph traversal and analysis endpoints")
public class GraphController {
    
    private final GraphTraversalService graphTraversalService;
    
    @GetMapping("/neighborhood/{nodeId}")
    @Operation(summary = "Get node neighborhood", 
               description = "Retrieve n-hop neighborhood around a node")
    public ResponseEntity<GraphNeighborhoodDTO> getNeighborhood(
            @Parameter(description = "Center node ID") 
            @PathVariable UUID nodeId,
            
            @Parameter(description = "Number of hops (1-3)") 
            @RequestParam(defaultValue = "1") int hops) {
        
        log.info("Getting {}-hop neighborhood for node: {}", hops, nodeId);
        
        GraphNeighborhoodDTO neighborhood = graphTraversalService.getNeighborhood(nodeId, hops);
        
        return ResponseEntity.ok(neighborhood);
    }
    
    @GetMapping("/path")
    @Operation(summary = "Find path between nodes", 
               description = "Find shortest path between two nodes")
    public ResponseEntity<Map<String, Object>> findPath(
            @RequestParam UUID from,
            @RequestParam UUID to,
            @RequestParam(defaultValue = "5") int maxHops) {
        
        log.info("Finding path from {} to {} (max {} hops)", from, to, maxHops);
        
        List<UUID> path = graphTraversalService.findShortestPath(from, to, maxHops);
        
        return ResponseEntity.ok(Map.of(
            "from", from,
            "to", to,
            "path", path,
            "distance", path.isEmpty() ? -1 : path.size() - 1,
            "found", !path.isEmpty()
        ));
    }
    
    @PostMapping("/subgraph")
    @Operation(summary = "Extract subgraph", 
               description = "Extract subgraph for given node IDs")
    public ResponseEntity<GraphNeighborhoodDTO> extractSubgraph(@RequestBody Set<UUID> nodeIds) {
        log.info("Extracting subgraph for {} nodes", nodeIds.size());
        
        if (nodeIds.isEmpty() || nodeIds.size() > 100) {
            throw new IllegalArgumentException("Node count must be between 1 and 100");
        }
        
        return ResponseEntity.ok(graphTraversalService.extractSubgraph(nodeIds));
    }
    
    @GetMapping("/component/{nodeId}")
    @Operation(summary = "Get connected component", 
               description = "Find all nodes in the connected component containing this node")
    public ResponseEntity<Map<String, Object>> getConnectedComponent(@PathVariable UUID nodeId) {
        log.info("Finding connected component for node: {}", nodeId);
        
        Set<UUID> component = graphTraversalService.getConnectedComponent(nodeId);
        
        return ResponseEntity.ok(Map.of(
            "nodeId", nodeId,
            "componentSize", component.size(),
            "nodeIds", component
        ));
    }
    
    @PostMapping("/centrality")
    @Operation(summary = "Calculate centrality", 
               description = "Calculate centrality scores for given nodes")
    public ResponseEntity<Map<UUID, Double>> calculateCentrality(@RequestBody Set<UUID> nodeIds) {
        log.info("Calculating centrality for {} nodes", nodeIds.size());
        
        if (nodeIds.isEmpty() || nodeIds.size() > 1000) {
            throw new IllegalArgumentException("Node count must be between 1 and 1000");
        }
        
        return ResponseEntity.ok(graphTraversalService.calculateCentrality(nodeIds));
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get graph statistics", 
               description = "Retrieve overall graph statistics and metrics")
    public ResponseEntity<Map<String, Object>> getGraphStatistics() {
        log.info("Getting graph statistics");
        
        Map<String, Object> stats = graphTraversalService.getGraphStatistics();
        
        return ResponseEntity.ok(stats);
    }
}