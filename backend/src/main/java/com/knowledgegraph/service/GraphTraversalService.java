package com.knowledgegraph.service;

import com.knowledgegraph.dto.GraphNeighborhoodDTO;
import com.knowledgegraph.model.Edge;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.repository.EdgeRepository;
import com.knowledgegraph.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
public class GraphTraversalService {
    
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * Get n-hop neighborhood for a node
     */
    @Cacheable(value = "neighborhoods", key = "#nodeId + '_' + #maxHops")
    public GraphNeighborhoodDTO getNeighborhood(UUID nodeId, int maxHops) {
        log.debug("Getting {}-hop neighborhood for node: {}", maxHops, nodeId);
        
        if (maxHops < 1 || maxHops > 3) {
            throw new IllegalArgumentException("Hops must be between 1 and 3");
        }
        
        Set<UUID> visitedNodes = new HashSet<>();
        Set<UUID> visitedEdges = new HashSet<>();
        Map<UUID, Integer> nodeHopLevels = new HashMap<>();
        List<GraphNeighborhoodDTO.GraphNode> nodes = new ArrayList<>();
        List<GraphNeighborhoodDTO.GraphEdge> edges = new ArrayList<>();
        
        // Start with the center node
        Node centerNode = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
        
        visitedNodes.add(nodeId);
        nodeHopLevels.put(nodeId, 0);
        nodes.add(convertToGraphNode(centerNode, 0));
        
        // Traverse hop by hop
        Set<UUID> currentLevel = new HashSet<>();
        currentLevel.add(nodeId);
        
        for (int hop = 1; hop <= maxHops; hop++) {
            Set<UUID> nextLevel = new HashSet<>();
            
            for (UUID currentNodeId : currentLevel) {
                // Get outgoing edges
                List<Edge> outgoing = edgeRepository.findBySourceId(currentNodeId);
                for (Edge edge : outgoing) {
                    if (!visitedEdges.contains(edge.getId())) {
                        visitedEdges.add(edge.getId());
                        edges.add(convertToGraphEdge(edge, hop));
                        
                        if (!visitedNodes.contains(edge.getTarget().getId())) {
                            visitedNodes.add(edge.getTarget().getId());
                            nodeHopLevels.put(edge.getTarget().getId(), hop);
                            nodes.add(convertToGraphNode(edge.getTarget(), hop));
                            nextLevel.add(edge.getTarget().getId());
                        }
                    }
                }
                
                // Get incoming edges
                List<Edge> incoming = edgeRepository.findByTargetId(currentNodeId);
                for (Edge edge : incoming) {
                    if (!visitedEdges.contains(edge.getId())) {
                        visitedEdges.add(edge.getId());
                        edges.add(convertToGraphEdge(edge, hop));
                        
                        if (!visitedNodes.contains(edge.getSource().getId())) {
                            visitedNodes.add(edge.getSource().getId());
                            nodeHopLevels.put(edge.getSource().getId(), hop);
                            nodes.add(convertToGraphNode(edge.getSource(), hop));
                            nextLevel.add(edge.getSource().getId());
                        }
                    }
                }
            }
            
            currentLevel = nextLevel;
            if (currentLevel.isEmpty()) {
                break; // No more nodes to explore
            }
        }
        
        // Calculate nodes per hop
        Map<Integer, Integer> nodesPerHop = nodes.stream()
            .collect(Collectors.groupingBy(
                GraphNeighborhoodDTO.GraphNode::getHopLevel,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        return GraphNeighborhoodDTO.builder()
            .centerNodeId(nodeId)
            .requestedHops(maxHops)
            .actualHops(nodesPerHop.size() - 1) // -1 for center node
            .nodes(nodes)
            .edges(edges)
            .nodesPerHop(nodesPerHop)
            .totalNodes((long) nodes.size())
            .totalEdges((long) edges.size())
            .build();
    }
    
    /**
     * Find shortest path between two nodes
     */
    public List<UUID> findShortestPath(UUID sourceId, UUID targetId, int maxHops) {
        log.debug("Finding path from {} to {} (max {} hops)", sourceId, targetId, maxHops);
        
        if (sourceId.equals(targetId)) {
            return List.of(sourceId);
        }
        
        // BFS to find shortest path
        Queue<List<UUID>> queue = new LinkedList<>();
        Set<UUID> visited = new HashSet<>();
        
        queue.add(List.of(sourceId));
        visited.add(sourceId);
        
        while (!queue.isEmpty() && queue.peek().size() <= maxHops) {
            List<UUID> path = queue.poll();
            UUID currentNode = path.get(path.size() - 1);
            
            // Get neighbors
            List<UUID> neighbors = getNeighborIds(currentNode);
            
            for (UUID neighbor : neighbors) {
                if (neighbor.equals(targetId)) {
                    // Found the target
                    List<UUID> completePath = new ArrayList<>(path);
                    completePath.add(targetId);
                    return completePath;
                }
                
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    List<UUID> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(newPath);
                }
            }
        }
        
        return Collections.emptyList(); // No path found
    }
    
    /**
     * Extract subgraph for given node IDs
     */
    public GraphNeighborhoodDTO extractSubgraph(Set<UUID> nodeIds) {
        log.debug("Extracting subgraph for {} nodes", nodeIds.size());
        
        // Get all nodes using streams
        List<GraphNeighborhoodDTO.GraphNode> nodes = nodeIds.stream()
            .map(nodeRepository::findById)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(node -> convertToGraphNode(node, 0))
            .collect(Collectors.toList());
        
        // Get edges between these nodes using streams
        List<GraphNeighborhoodDTO.GraphEdge> edges = nodeIds.stream()
            .flatMap(sourceId -> edgeRepository.findBySourceId(sourceId).stream())
            .filter(edge -> nodeIds.contains(edge.getTarget().getId()))
            .map(edge -> convertToGraphEdge(edge, 0))
            .collect(Collectors.toList());
        
        return GraphNeighborhoodDTO.builder()
            .nodes(nodes)
            .edges(edges)
            .totalNodes((long) nodes.size())
            .totalEdges((long) edges.size())
            .build();
    }
    
    /**
     * Get connected components containing a node
     */
    public Set<UUID> getConnectedComponent(UUID nodeId) {
        log.debug("Finding connected component for node: {}", nodeId);
        
        Set<UUID> component = new HashSet<>();
        Queue<UUID> queue = new LinkedList<>();
        
        queue.add(nodeId);
        component.add(nodeId);
        
        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            List<UUID> neighbors = getNeighborIds(current);
            
            for (UUID neighbor : neighbors) {
                if (!component.contains(neighbor)) {
                    component.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        
        return component;
    }
    
    /**
     * Calculate node centrality in subgraph
     */
    public Map<UUID, Double> calculateCentrality(Set<UUID> nodeIds) {
        // Simplified using stream collectors
        int normalizer = Math.max(nodeIds.size() - 1, 1); // Avoid division by zero
        
        return nodeIds.stream()
            .collect(Collectors.toMap(
                nodeId -> nodeId,
                nodeId -> (double) getNeighborIds(nodeId).size() / normalizer
            ));
    }
    
    /**
     * Get graph statistics
     */
    @Cacheable(value = "graphStats")
    public Map<String, Object> getGraphStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalNodes", nodeRepository.count());
        stats.put("totalEdges", edgeRepository.count());
        
        // Get node type distribution
        String nodeTypeSql = """
            SELECT type, COUNT(*) as count 
            FROM kg.nodes 
            GROUP BY type
            """;
        
        Map<String, Long> nodeTypes = new HashMap<>();
        jdbcTemplate.query(nodeTypeSql, rs -> {
            nodeTypes.put(rs.getString("type"), rs.getLong("count"));
        });
        stats.put("nodeTypes", nodeTypes);
        
        // Get edge type distribution
        String edgeTypeSql = """
            SELECT type, COUNT(*) as count 
            FROM kg.edges 
            GROUP BY type
            """;
        
        Map<String, Long> edgeTypes = new HashMap<>();
        jdbcTemplate.query(edgeTypeSql, rs -> {
            edgeTypes.put(rs.getString("type"), rs.getLong("count"));
        });
        stats.put("edgeTypes", edgeTypes);
        
        // Average connections per node
        String avgConnectionsSql = """
            SELECT AVG(connection_count) as avg_connections
            FROM (
                SELECT source_id as node_id, COUNT(*) as connection_count
                FROM kg.edges
                GROUP BY source_id
                UNION ALL
                SELECT target_id as node_id, COUNT(*) as connection_count
                FROM kg.edges
                GROUP BY target_id
            ) as connections
            """;
        
        Double avgConnections = jdbcTemplate.queryForObject(avgConnectionsSql, Double.class);
        stats.put("avgConnectionsPerNode", avgConnections);
        
        return stats;
    }
    
    /**
     * Get neighbor IDs for a node
     */
    private List<UUID> getNeighborIds(UUID nodeId) {
        // Simplified using Stream.concat
        return Stream.concat(
            edgeRepository.findBySourceId(nodeId).stream()
                .map(edge -> edge.getTarget().getId()),
            edgeRepository.findByTargetId(nodeId).stream()
                .map(edge -> edge.getSource().getId())
        )
        .distinct()
        .collect(Collectors.toList());
    }
    
    /**
     * Convert Node to GraphNode DTO
     */
    private GraphNeighborhoodDTO.GraphNode convertToGraphNode(Node node, int hopLevel) {
        return GraphNeighborhoodDTO.GraphNode.builder()
            .id(node.getId())
            .type(node.getType().name())
            .name(node.getName())
            .properties(node.getProperties())
            .hopLevel(hopLevel)
            .build();
    }
    
    /**
     * Convert Edge to GraphEdge DTO
     */
    private GraphNeighborhoodDTO.GraphEdge convertToGraphEdge(Edge edge, int hopLevel) {
        return GraphNeighborhoodDTO.GraphEdge.builder()
            .id(edge.getId())
            .sourceId(edge.getSource().getId())
            .targetId(edge.getTarget().getId())
            .type(edge.getType().name())
            .properties(edge.getProperties())
            .hopLevel(hopLevel)
            .build();
    }
}