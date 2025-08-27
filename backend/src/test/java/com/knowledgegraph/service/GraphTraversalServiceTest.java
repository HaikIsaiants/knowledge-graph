package com.knowledgegraph.service;

import com.knowledgegraph.dto.GraphNeighborhoodDTO;
import com.knowledgegraph.model.Edge;
import com.knowledgegraph.model.EdgeType;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.EdgeRepository;
import com.knowledgegraph.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GraphTraversalService Tests")
class GraphTraversalServiceTest {

    @Mock
    private NodeRepository nodeRepository;
    
    @Mock
    private EdgeRepository edgeRepository;
    
    @Mock
    private JdbcTemplate jdbcTemplate;
    
    @InjectMocks
    private GraphTraversalService graphTraversalService;
    
    private Node centerNode;
    private Node neighbor1;
    private Node neighbor2;
    private Node secondHopNode;
    private Edge edge1;
    private Edge edge2;
    private Edge edge3;
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up GraphTraversalService test environment...");
        
        // Create test nodes
        centerNode = createNode(UUID.randomUUID(), "Center Node", NodeType.PERSON);
        neighbor1 = createNode(UUID.randomUUID(), "Neighbor 1", NodeType.ORGANIZATION);
        neighbor2 = createNode(UUID.randomUUID(), "Neighbor 2", NodeType.DOCUMENT);
        secondHopNode = createNode(UUID.randomUUID(), "Second Hop Node", NodeType.CONCEPT);
        
        // Create test edges
        edge1 = createEdge(centerNode, neighbor1, EdgeType.RELATED_TO);
        edge2 = createEdge(centerNode, neighbor2, EdgeType.MENTIONS);
        edge3 = createEdge(neighbor1, secondHopNode, EdgeType.PART_OF);
    }
    
    @Test
    @DisplayName("getNeighborhood - 1-hop neighborhood")
    void testGetNeighborhood_OneHop() {
        System.out.println("Testing 1-hop neighborhood retrieval...");
        
        UUID nodeId = centerNode.getId();
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(centerNode));
        when(edgeRepository.findBySourceId(nodeId)).thenReturn(List.of(edge1, edge2));
        when(edgeRepository.findByTargetId(nodeId)).thenReturn(Collections.emptyList());
        
        GraphNeighborhoodDTO neighborhood = graphTraversalService.getNeighborhood(nodeId, 1);
        
        System.out.println("Nodes found: " + neighborhood.getTotalNodes());
        System.out.println("Edges found: " + neighborhood.getTotalEdges());
        
        assertNotNull(neighborhood);
        assertEquals(nodeId, neighborhood.getCenterNodeId());
        assertEquals(1, neighborhood.getRequestedHops());
        assertEquals(3, neighborhood.getTotalNodes()); // center + 2 neighbors
        assertEquals(2, neighborhood.getTotalEdges());
        assertNotNull(neighborhood.getNodesPerHop());
        assertEquals(1, (int) neighborhood.getNodesPerHop().get(0)); // center node
        assertEquals(2, (int) neighborhood.getNodesPerHop().get(1)); // 2 neighbors
        
        verify(nodeRepository).findById(nodeId);
        verify(edgeRepository).findBySourceId(nodeId);
        verify(edgeRepository).findByTargetId(nodeId);
        
        System.out.println("✓ 1-hop neighborhood retrieval works correctly");
    }
    
    @Test
    @DisplayName("getNeighborhood - 2-hop neighborhood")
    void testGetNeighborhood_TwoHop() {
        System.out.println("Testing 2-hop neighborhood retrieval...");
        
        UUID nodeId = centerNode.getId();
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(centerNode));
        // First hop edges
        when(edgeRepository.findBySourceId(nodeId)).thenReturn(List.of(edge1, edge2));
        when(edgeRepository.findByTargetId(nodeId)).thenReturn(Collections.emptyList());
        // Second hop edges
        when(edgeRepository.findBySourceId(neighbor1.getId())).thenReturn(List.of(edge3));
        when(edgeRepository.findByTargetId(neighbor1.getId())).thenReturn(Collections.emptyList());
        when(edgeRepository.findBySourceId(neighbor2.getId())).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(neighbor2.getId())).thenReturn(Collections.emptyList());
        
        GraphNeighborhoodDTO neighborhood = graphTraversalService.getNeighborhood(nodeId, 2);
        
        System.out.println("Total nodes in 2-hop: " + neighborhood.getTotalNodes());
        System.out.println("Total edges in 2-hop: " + neighborhood.getTotalEdges());
        System.out.println("Nodes per hop: " + neighborhood.getNodesPerHop());
        
        assertNotNull(neighborhood);
        assertEquals(nodeId, neighborhood.getCenterNodeId());
        assertEquals(2, neighborhood.getRequestedHops());
        assertEquals(4, neighborhood.getTotalNodes()); // center + 2 neighbors + 1 second-hop
        assertEquals(3, neighborhood.getTotalEdges()); // 2 first-hop + 1 second-hop
        
        System.out.println("✓ 2-hop neighborhood retrieval works correctly");
    }
    
    @Test
    @DisplayName("getNeighborhood - Invalid hop count")
    void testGetNeighborhood_InvalidHops() {
        System.out.println("Testing invalid hop count handling...");
        
        UUID nodeId = UUID.randomUUID();
        
        // Test hop count too small
        assertThrows(IllegalArgumentException.class, () -> {
            graphTraversalService.getNeighborhood(nodeId, 0);
        });
        
        // Test hop count too large
        assertThrows(IllegalArgumentException.class, () -> {
            graphTraversalService.getNeighborhood(nodeId, 4);
        });
        
        System.out.println("✓ Invalid hop count properly rejected");
    }
    
    @Test
    @DisplayName("getNeighborhood - Node not found")
    void testGetNeighborhood_NodeNotFound() {
        System.out.println("Testing neighborhood for non-existent node...");
        
        UUID nodeId = UUID.randomUUID();
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.empty());
        
        assertThrows(IllegalArgumentException.class, () -> {
            graphTraversalService.getNeighborhood(nodeId, 1);
        });
        
        System.out.println("✓ Non-existent node properly handled");
    }
    
    @Test
    @DisplayName("findShortestPath - Direct connection")
    void testFindShortestPath_DirectConnection() {
        System.out.println("Testing shortest path with direct connection...");
        
        UUID sourceId = centerNode.getId();
        UUID targetId = neighbor1.getId();
        
        when(edgeRepository.findBySourceId(sourceId)).thenReturn(List.of(edge1));
        when(edgeRepository.findByTargetId(sourceId)).thenReturn(Collections.emptyList());
        
        List<UUID> path = graphTraversalService.findShortestPath(sourceId, targetId, 5);
        
        System.out.println("Path length: " + path.size());
        assertNotNull(path);
        assertEquals(2, path.size());
        assertEquals(sourceId, path.get(0));
        assertEquals(targetId, path.get(1));
        
        System.out.println("✓ Direct connection path found correctly");
    }
    
    @Test
    @DisplayName("findShortestPath - Multi-hop path")
    void testFindShortestPath_MultiHop() {
        System.out.println("Testing shortest path with multiple hops...");
        
        UUID sourceId = centerNode.getId();
        UUID targetId = secondHopNode.getId();
        
        when(edgeRepository.findBySourceId(sourceId)).thenReturn(List.of(edge1, edge2));
        when(edgeRepository.findByTargetId(sourceId)).thenReturn(Collections.emptyList());
        when(edgeRepository.findBySourceId(neighbor1.getId())).thenReturn(List.of(edge3));
        when(edgeRepository.findByTargetId(neighbor1.getId())).thenReturn(Collections.emptyList());
        
        List<UUID> path = graphTraversalService.findShortestPath(sourceId, targetId, 5);
        
        System.out.println("Multi-hop path length: " + path.size());
        assertNotNull(path);
        assertEquals(3, path.size());
        assertEquals(sourceId, path.get(0));
        assertEquals(neighbor1.getId(), path.get(1));
        assertEquals(targetId, path.get(2));
        
        System.out.println("✓ Multi-hop path found correctly");
    }
    
    @Test
    @DisplayName("findShortestPath - No path exists")
    void testFindShortestPath_NoPath() {
        System.out.println("Testing shortest path when no path exists...");
        
        UUID sourceId = centerNode.getId();
        UUID targetId = UUID.randomUUID(); // Disconnected node
        
        when(edgeRepository.findBySourceId(any())).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(any())).thenReturn(Collections.emptyList());
        
        List<UUID> path = graphTraversalService.findShortestPath(sourceId, targetId, 3);
        
        assertTrue(path.isEmpty());
        System.out.println("✓ No path scenario handled correctly");
    }
    
    @Test
    @DisplayName("findShortestPath - Same source and target")
    void testFindShortestPath_SameNode() {
        System.out.println("Testing shortest path with same source and target...");
        
        UUID nodeId = centerNode.getId();
        List<UUID> path = graphTraversalService.findShortestPath(nodeId, nodeId, 5);
        
        assertNotNull(path);
        assertEquals(1, path.size());
        assertEquals(nodeId, path.get(0));
        
        System.out.println("✓ Same node path handled correctly");
    }
    
    @Test
    @DisplayName("extractSubgraph - Success")
    void testExtractSubgraph_Success() {
        System.out.println("Testing subgraph extraction...");
        
        Set<UUID> nodeIds = Set.of(centerNode.getId(), neighbor1.getId(), neighbor2.getId());
        
        when(nodeRepository.findById(centerNode.getId())).thenReturn(Optional.of(centerNode));
        when(nodeRepository.findById(neighbor1.getId())).thenReturn(Optional.of(neighbor1));
        when(nodeRepository.findById(neighbor2.getId())).thenReturn(Optional.of(neighbor2));
        when(edgeRepository.findBySourceId(centerNode.getId())).thenReturn(List.of(edge1, edge2));
        when(edgeRepository.findBySourceId(neighbor1.getId())).thenReturn(Collections.emptyList());
        when(edgeRepository.findBySourceId(neighbor2.getId())).thenReturn(Collections.emptyList());
        
        GraphNeighborhoodDTO subgraph = graphTraversalService.extractSubgraph(nodeIds);
        
        System.out.println("Subgraph nodes: " + subgraph.getTotalNodes());
        System.out.println("Subgraph edges: " + subgraph.getTotalEdges());
        
        assertNotNull(subgraph);
        assertEquals(3, subgraph.getTotalNodes());
        assertEquals(2, subgraph.getTotalEdges());
        
        System.out.println("✓ Subgraph extraction works correctly");
    }
    
    @Test
    @DisplayName("extractSubgraph - Empty node set")
    void testExtractSubgraph_EmptySet() {
        System.out.println("Testing subgraph extraction with empty set...");
        
        GraphNeighborhoodDTO subgraph = graphTraversalService.extractSubgraph(Collections.emptySet());
        
        assertNotNull(subgraph);
        assertEquals(0, subgraph.getTotalNodes());
        assertEquals(0, subgraph.getTotalEdges());
        
        System.out.println("✓ Empty subgraph handled correctly");
    }
    
    @Test
    @DisplayName("getConnectedComponent - Single component")
    void testGetConnectedComponent_SingleComponent() {
        System.out.println("Testing connected component discovery...");
        
        UUID nodeId = centerNode.getId();
        
        // Setup a connected component
        when(edgeRepository.findBySourceId(nodeId)).thenReturn(List.of(edge1, edge2));
        when(edgeRepository.findByTargetId(nodeId)).thenReturn(Collections.emptyList());
        when(edgeRepository.findBySourceId(neighbor1.getId())).thenReturn(List.of(edge3));
        when(edgeRepository.findByTargetId(neighbor1.getId())).thenReturn(List.of(edge1));
        when(edgeRepository.findBySourceId(neighbor2.getId())).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(neighbor2.getId())).thenReturn(List.of(edge2));
        when(edgeRepository.findBySourceId(secondHopNode.getId())).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(secondHopNode.getId())).thenReturn(List.of(edge3));
        
        Set<UUID> component = graphTraversalService.getConnectedComponent(nodeId);
        
        System.out.println("Component size: " + component.size());
        assertNotNull(component);
        assertTrue(component.contains(nodeId));
        assertTrue(component.contains(neighbor1.getId()));
        assertTrue(component.contains(neighbor2.getId()));
        assertTrue(component.contains(secondHopNode.getId()));
        
        System.out.println("✓ Connected component discovered correctly");
    }
    
    @Test
    @DisplayName("getConnectedComponent - Isolated node")
    void testGetConnectedComponent_IsolatedNode() {
        System.out.println("Testing connected component for isolated node...");
        
        UUID nodeId = UUID.randomUUID();
        
        when(edgeRepository.findBySourceId(nodeId)).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(nodeId)).thenReturn(Collections.emptyList());
        
        Set<UUID> component = graphTraversalService.getConnectedComponent(nodeId);
        
        assertNotNull(component);
        assertEquals(1, component.size());
        assertTrue(component.contains(nodeId));
        
        System.out.println("✓ Isolated node component handled correctly");
    }
    
    @Test
    @DisplayName("calculateCentrality - Success")
    void testCalculateCentrality_Success() {
        System.out.println("Testing centrality calculation...");
        
        Set<UUID> nodeIds = Set.of(centerNode.getId(), neighbor1.getId(), neighbor2.getId());
        
        when(edgeRepository.findBySourceId(centerNode.getId())).thenReturn(List.of(edge1, edge2));
        when(edgeRepository.findByTargetId(centerNode.getId())).thenReturn(Collections.emptyList());
        when(edgeRepository.findBySourceId(neighbor1.getId())).thenReturn(List.of(edge3));
        when(edgeRepository.findByTargetId(neighbor1.getId())).thenReturn(List.of(edge1));
        when(edgeRepository.findBySourceId(neighbor2.getId())).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(neighbor2.getId())).thenReturn(List.of(edge2));
        
        Map<UUID, Double> centrality = graphTraversalService.calculateCentrality(nodeIds);
        
        System.out.println("Centrality scores: " + centrality);
        assertNotNull(centrality);
        assertEquals(3, centrality.size());
        assertTrue(centrality.containsKey(centerNode.getId()));
        assertTrue(centrality.containsKey(neighbor1.getId()));
        assertTrue(centrality.containsKey(neighbor2.getId()));
        
        // Center node should have highest centrality (2 connections)
        Double centerCentrality = centrality.get(centerNode.getId());
        assertNotNull(centerCentrality);
        assertTrue(centerCentrality > 0);
        
        System.out.println("✓ Centrality calculation works correctly");
    }
    
    @Test
    @DisplayName("calculateCentrality - Single node")
    void testCalculateCentrality_SingleNode() {
        System.out.println("Testing centrality for single node...");
        
        UUID nodeId = UUID.randomUUID();
        Set<UUID> nodeIds = Set.of(nodeId);
        
        when(edgeRepository.findBySourceId(nodeId)).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(nodeId)).thenReturn(Collections.emptyList());
        
        Map<UUID, Double> centrality = graphTraversalService.calculateCentrality(nodeIds);
        
        assertNotNull(centrality);
        assertEquals(1, centrality.size());
        assertEquals(0.0, centrality.get(nodeId), 0.001);
        
        System.out.println("✓ Single node centrality handled correctly");
    }
    
    @Test
    @DisplayName("getGraphStatistics - Success")
    void testGetGraphStatistics_Success() {
        System.out.println("Testing graph statistics retrieval...");
        
        when(nodeRepository.count()).thenReturn(100L);
        when(edgeRepository.count()).thenReturn(250L);
        
        // Mock node type distribution query
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            // Simulate result set
            return null;
        }).when(jdbcTemplate).query(contains("SELECT type, COUNT(*) as count"), any(RowCallbackHandler.class));
        
        // Mock edge type distribution query
        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            // Simulate result set
            return null;
        }).when(jdbcTemplate).query(contains("FROM kg.edges"), any(RowCallbackHandler.class));
        
        // Mock average connections query
        when(jdbcTemplate.queryForObject(contains("AVG(connection_count)"), eq(Double.class)))
            .thenReturn(2.5);
        
        Map<String, Object> stats = graphTraversalService.getGraphStatistics();
        
        System.out.println("Graph statistics: " + stats);
        assertNotNull(stats);
        assertEquals(100L, stats.get("totalNodes"));
        assertEquals(250L, stats.get("totalEdges"));
        assertEquals(2.5, stats.get("avgConnectionsPerNode"));
        assertNotNull(stats.get("nodeTypes"));
        assertNotNull(stats.get("edgeTypes"));
        
        System.out.println("✓ Graph statistics retrieved correctly");
    }
    
    @Test
    @DisplayName("getNeighborhood - Cyclic graph handling")
    void testGetNeighborhood_CyclicGraph() {
        System.out.println("Testing neighborhood with cycles...");
        
        UUID nodeId = centerNode.getId();
        
        // Create a cycle: center -> neighbor1 -> neighbor2 -> center
        Edge cycleEdge = createEdge(neighbor2, centerNode, EdgeType.RELATED_TO);
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(centerNode));
        when(edgeRepository.findBySourceId(nodeId)).thenReturn(List.of(edge1));
        when(edgeRepository.findByTargetId(nodeId)).thenReturn(List.of(cycleEdge));
        when(edgeRepository.findBySourceId(neighbor1.getId())).thenReturn(List.of(edge3));
        when(edgeRepository.findByTargetId(neighbor1.getId())).thenReturn(List.of(edge1));
        
        GraphNeighborhoodDTO neighborhood = graphTraversalService.getNeighborhood(nodeId, 3);
        
        assertNotNull(neighborhood);
        // Should not get stuck in infinite loop
        assertTrue(neighborhood.getTotalNodes() > 0);
        
        System.out.println("✓ Cyclic graph handled correctly");
    }
    
    @Test
    @DisplayName("Performance test - Large neighborhood")
    void testPerformance_LargeNeighborhood() {
        System.out.println("Testing performance with large neighborhood...");
        
        UUID nodeId = centerNode.getId();
        
        // Create many neighbors
        List<Edge> manyEdges = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Node neighbor = createNode(UUID.randomUUID(), "Neighbor " + i, NodeType.CONCEPT);
            manyEdges.add(createEdge(centerNode, neighbor, EdgeType.RELATED_TO));
        }
        
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(centerNode));
        when(edgeRepository.findBySourceId(nodeId)).thenReturn(manyEdges);
        when(edgeRepository.findByTargetId(nodeId)).thenReturn(Collections.emptyList());
        
        long startTime = System.currentTimeMillis();
        GraphNeighborhoodDTO neighborhood = graphTraversalService.getNeighborhood(nodeId, 1);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Large neighborhood retrieval time: " + duration + "ms");
        assertNotNull(neighborhood);
        assertEquals(101, neighborhood.getTotalNodes()); // center + 100 neighbors
        assertTrue(duration < 1000, "Operation should complete within 1 second");
        
        System.out.println("✓ Large neighborhood handled efficiently");
    }
    
    @Test
    @DisplayName("Concurrent access test")
    void testConcurrentAccess() throws InterruptedException {
        System.out.println("Testing concurrent access to graph traversal...");
        
        UUID nodeId = centerNode.getId();
        when(nodeRepository.findById(nodeId)).thenReturn(Optional.of(centerNode));
        when(edgeRepository.findBySourceId(any())).thenReturn(List.of(edge1));
        when(edgeRepository.findByTargetId(any())).thenReturn(Collections.emptyList());
        
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<GraphNeighborhoodDTO> results = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    GraphNeighborhoodDTO neighborhood = graphTraversalService.getNeighborhood(nodeId, 1);
                    results.add(neighborhood);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        
        assertEquals(threadCount, results.size());
        // All results should be consistent
        long distinctResults = results.stream()
            .map(GraphNeighborhoodDTO::getTotalNodes)
            .distinct()
            .count();
        assertEquals(1, distinctResults);
        
        System.out.println("✓ Concurrent access handled correctly");
    }
    
    @Test
    @DisplayName("Edge cases - Null properties handling")
    void testEdgeCases_NullProperties() {
        System.out.println("Testing null properties handling...");
        
        Node nodeWithNullProps = new Node();
        nodeWithNullProps.setId(UUID.randomUUID());
        nodeWithNullProps.setName("Node with null props");
        nodeWithNullProps.setType(NodeType.CONCEPT);
        nodeWithNullProps.setProperties(null);
        
        when(nodeRepository.findById(nodeWithNullProps.getId())).thenReturn(Optional.of(nodeWithNullProps));
        when(edgeRepository.findBySourceId(any())).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(any())).thenReturn(Collections.emptyList());
        
        GraphNeighborhoodDTO neighborhood = graphTraversalService.getNeighborhood(nodeWithNullProps.getId(), 1);
        
        assertNotNull(neighborhood);
        assertEquals(1, neighborhood.getTotalNodes());
        
        GraphNeighborhoodDTO.GraphNode graphNode = neighborhood.getNodes().get(0);
        assertNull(graphNode.getProperties());
        
        System.out.println("✓ Null properties handled correctly");
    }
    
    // Helper methods
    
    private Node createNode(UUID id, String name, NodeType type) {
        Node node = new Node();
        node.setId(id);
        node.setName(name);
        node.setType(type);
        node.setSourceUri("http://example.com/" + id);
        node.setProperties(Map.of("key", "value"));
        node.setCreatedAt(LocalDateTime.now());
        node.setUpdatedAt(LocalDateTime.now());
        return node;
    }
    
    private Edge createEdge(Node source, Node target, EdgeType type) {
        Edge edge = new Edge();
        edge.setId(UUID.randomUUID());
        edge.setSource(source);
        edge.setTarget(target);
        edge.setType(type);
        edge.setProperties(Map.of("weight", "1.0"));
        edge.setCreatedAt(LocalDateTime.now());
        edge.setUpdatedAt(LocalDateTime.now());
        return edge;
    }
}