package com.knowledgegraph.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgegraph.dto.GraphNeighborhoodDTO;
import com.knowledgegraph.service.GraphTraversalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GraphController.class)
@DisplayName("GraphController Tests")
class GraphControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private GraphTraversalService graphTraversalService;
    
    private UUID testNodeId;
    private GraphNeighborhoodDTO mockNeighborhood;
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up GraphController test environment...");
        
        testNodeId = UUID.randomUUID();
        
        // Create mock neighborhood
        List<GraphNeighborhoodDTO.GraphNode> nodes = List.of(
            GraphNeighborhoodDTO.GraphNode.builder()
                .id(testNodeId)
                .type("PERSON")
                .name("Center Node")
                .properties(Map.of("key", "value"))
                .hopLevel(0)
                .build(),
            GraphNeighborhoodDTO.GraphNode.builder()
                .id(UUID.randomUUID())
                .type("ORGANIZATION")
                .name("Neighbor 1")
                .hopLevel(1)
                .build(),
            GraphNeighborhoodDTO.GraphNode.builder()
                .id(UUID.randomUUID())
                .type("DOCUMENT")
                .name("Neighbor 2")
                .hopLevel(1)
                .build()
        );
        
        List<GraphNeighborhoodDTO.GraphEdge> edges = List.of(
            GraphNeighborhoodDTO.GraphEdge.builder()
                .id(UUID.randomUUID())
                .sourceId(testNodeId)
                .targetId(nodes.get(1).getId())
                .type("WORKS_FOR")
                .properties(Map.of("since", "2020"))
                .hopLevel(1)
                .build(),
            GraphNeighborhoodDTO.GraphEdge.builder()
                .id(UUID.randomUUID())
                .sourceId(testNodeId)
                .targetId(nodes.get(2).getId())
                .type("AUTHORED")
                .hopLevel(1)
                .build()
        );
        
        mockNeighborhood = GraphNeighborhoodDTO.builder()
            .centerNodeId(testNodeId)
            .requestedHops(1)
            .actualHops(1)
            .nodes(nodes)
            .edges(edges)
            .nodesPerHop(Map.of(0, 1, 1, 2))
            .totalNodes(3L)
            .totalEdges(2L)
            .build();
    }
    
    @Test
    @DisplayName("GET /graph/neighborhood/{nodeId} - Success with default hops")
    void testGetNeighborhood_DefaultHops() throws Exception {
        System.out.println("Testing get neighborhood with default hops...");
        
        when(graphTraversalService.getNeighborhood(testNodeId, 1))
            .thenReturn(mockNeighborhood);
        
        mockMvc.perform(get("/graph/neighborhood/{nodeId}", testNodeId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.centerNodeId").value(testNodeId.toString()))
            .andExpect(jsonPath("$.requestedHops").value(1))
            .andExpect(jsonPath("$.totalNodes").value(3))
            .andExpect(jsonPath("$.totalEdges").value(2))
            .andExpect(jsonPath("$.nodes", hasSize(3)))
            .andExpect(jsonPath("$.edges", hasSize(2)));
        
        verify(graphTraversalService).getNeighborhood(testNodeId, 1);
        
        System.out.println("✓ Get neighborhood with default hops works correctly");
    }
    
    @Test
    @DisplayName("GET /graph/neighborhood/{nodeId} - Success with custom hops")
    void testGetNeighborhood_CustomHops() throws Exception {
        System.out.println("Testing get neighborhood with custom hops...");
        
        mockNeighborhood.setRequestedHops(3);
        mockNeighborhood.setActualHops(3);
        
        when(graphTraversalService.getNeighborhood(testNodeId, 3))
            .thenReturn(mockNeighborhood);
        
        mockMvc.perform(get("/graph/neighborhood/{nodeId}", testNodeId)
                .param("hops", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requestedHops").value(3))
            .andExpect(jsonPath("$.actualHops").value(3));
        
        verify(graphTraversalService).getNeighborhood(testNodeId, 3);
        
        System.out.println("✓ Get neighborhood with custom hops works correctly");
    }
    
    @Test
    @DisplayName("GET /graph/neighborhood/{nodeId} - Invalid UUID")
    void testGetNeighborhood_InvalidUUID() throws Exception {
        System.out.println("Testing get neighborhood with invalid UUID...");
        
        mockMvc.perform(get("/graph/neighborhood/invalid-uuid"))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Invalid UUID properly rejected");
    }
    
    @Test
    @DisplayName("GET /graph/neighborhood/{nodeId} - Node not found")
    void testGetNeighborhood_NodeNotFound() throws Exception {
        System.out.println("Testing get neighborhood for non-existent node...");
        
        UUID nonExistentId = UUID.randomUUID();
        
        when(graphTraversalService.getNeighborhood(nonExistentId, 1))
            .thenThrow(new IllegalArgumentException("Node not found"));
        
        mockMvc.perform(get("/graph/neighborhood/{nodeId}", nonExistentId))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Non-existent node handled correctly");
    }
    
    @Test
    @DisplayName("GET /graph/path - Find path success")
    void testFindPath_Success() throws Exception {
        System.out.println("Testing find path between nodes...");
        
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        List<UUID> path = List.of(fromId, testNodeId, toId);
        
        when(graphTraversalService.findShortestPath(fromId, toId, 5))
            .thenReturn(path);
        
        mockMvc.perform(get("/graph/path")
                .param("from", fromId.toString())
                .param("to", toId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.from").value(fromId.toString()))
            .andExpect(jsonPath("$.to").value(toId.toString()))
            .andExpect(jsonPath("$.path", hasSize(3)))
            .andExpect(jsonPath("$.distance").value(2))
            .andExpect(jsonPath("$.found").value(true));
        
        verify(graphTraversalService).findShortestPath(fromId, toId, 5);
        
        System.out.println("✓ Path finding works correctly");
    }
    
    @Test
    @DisplayName("GET /graph/path - No path found")
    void testFindPath_NoPath() throws Exception {
        System.out.println("Testing find path with no path...");
        
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        
        when(graphTraversalService.findShortestPath(fromId, toId, 5))
            .thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/graph/path")
                .param("from", fromId.toString())
                .param("to", toId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.path", empty()))
            .andExpect(jsonPath("$.distance").value(-1))
            .andExpect(jsonPath("$.found").value(false));
        
        System.out.println("✓ No path scenario handled correctly");
    }
    
    @Test
    @DisplayName("GET /graph/path - Custom max hops")
    void testFindPath_CustomMaxHops() throws Exception {
        System.out.println("Testing find path with custom max hops...");
        
        UUID fromId = UUID.randomUUID();
        UUID toId = UUID.randomUUID();
        
        when(graphTraversalService.findShortestPath(fromId, toId, 10))
            .thenReturn(List.of(fromId, toId));
        
        mockMvc.perform(get("/graph/path")
                .param("from", fromId.toString())
                .param("to", toId.toString())
                .param("maxHops", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.distance").value(1));
        
        verify(graphTraversalService).findShortestPath(fromId, toId, 10);
        
        System.out.println("✓ Custom max hops works correctly");
    }
    
    @Test
    @DisplayName("GET /graph/path - Missing parameters")
    void testFindPath_MissingParameters() throws Exception {
        System.out.println("Testing find path with missing parameters...");
        
        mockMvc.perform(get("/graph/path")
                .param("from", UUID.randomUUID().toString()))
            .andExpect(status().isBadRequest());
        
        mockMvc.perform(get("/graph/path")
                .param("to", UUID.randomUUID().toString()))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Missing parameters properly rejected");
    }
    
    @Test
    @DisplayName("POST /graph/subgraph - Extract subgraph success")
    void testExtractSubgraph_Success() throws Exception {
        System.out.println("Testing extract subgraph...");
        
        Set<UUID> nodeIds = Set.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        when(graphTraversalService.extractSubgraph(nodeIds))
            .thenReturn(mockNeighborhood);
        
        mockMvc.perform(post("/graph/subgraph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nodeIds)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nodes").isArray())
            .andExpect(jsonPath("$.edges").isArray())
            .andExpect(jsonPath("$.totalNodes").value(3))
            .andExpect(jsonPath("$.totalEdges").value(2));
        
        verify(graphTraversalService).extractSubgraph(nodeIds);
        
        System.out.println("✓ Subgraph extraction works correctly");
    }
    
    @Test
    @DisplayName("POST /graph/subgraph - Empty node set")
    void testExtractSubgraph_EmptySet() throws Exception {
        System.out.println("Testing extract subgraph with empty set...");
        
        Set<UUID> emptySet = Collections.emptySet();
        
        mockMvc.perform(post("/graph/subgraph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptySet)))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Empty node set properly rejected");
    }
    
    @Test
    @DisplayName("POST /graph/subgraph - Too many nodes")
    void testExtractSubgraph_TooManyNodes() throws Exception {
        System.out.println("Testing extract subgraph with too many nodes...");
        
        Set<UUID> tooManyNodes = new HashSet<>();
        for (int i = 0; i < 101; i++) {
            tooManyNodes.add(UUID.randomUUID());
        }
        
        mockMvc.perform(post("/graph/subgraph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tooManyNodes)))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Too many nodes properly rejected");
    }
    
    @Test
    @DisplayName("GET /graph/component/{nodeId} - Get connected component")
    void testGetConnectedComponent_Success() throws Exception {
        System.out.println("Testing get connected component...");
        
        Set<UUID> component = Set.of(
            testNodeId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        when(graphTraversalService.getConnectedComponent(testNodeId))
            .thenReturn(component);
        
        mockMvc.perform(get("/graph/component/{nodeId}", testNodeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nodeId").value(testNodeId.toString()))
            .andExpect(jsonPath("$.componentSize").value(4))
            .andExpect(jsonPath("$.nodeIds", hasSize(4)));
        
        verify(graphTraversalService).getConnectedComponent(testNodeId);
        
        System.out.println("✓ Connected component retrieval works correctly");
    }
    
    @Test
    @DisplayName("POST /graph/centrality - Calculate centrality")
    void testCalculateCentrality_Success() throws Exception {
        System.out.println("Testing centrality calculation...");
        
        Set<UUID> nodeIds = Set.of(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID()
        );
        
        Map<UUID, Double> centrality = new HashMap<>();
        nodeIds.forEach(id -> centrality.put(id, Math.random()));
        
        when(graphTraversalService.calculateCentrality(nodeIds))
            .thenReturn(centrality);
        
        mockMvc.perform(post("/graph/centrality")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nodeIds)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isMap())
            .andExpect(jsonPath("$", aMapWithSize(3)));
        
        verify(graphTraversalService).calculateCentrality(nodeIds);
        
        System.out.println("✓ Centrality calculation works correctly");
    }
    
    @Test
    @DisplayName("POST /graph/centrality - Empty node set")
    void testCalculateCentrality_EmptySet() throws Exception {
        System.out.println("Testing centrality with empty set...");
        
        mockMvc.perform(post("/graph/centrality")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Empty set properly rejected");
    }
    
    @Test
    @DisplayName("POST /graph/centrality - Too many nodes")
    void testCalculateCentrality_TooManyNodes() throws Exception {
        System.out.println("Testing centrality with too many nodes...");
        
        Set<UUID> tooManyNodes = new HashSet<>();
        for (int i = 0; i < 1001; i++) {
            tooManyNodes.add(UUID.randomUUID());
        }
        
        mockMvc.perform(post("/graph/centrality")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tooManyNodes)))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Too many nodes properly rejected");
    }
    
    @Test
    @DisplayName("GET /graph/stats - Get graph statistics")
    void testGetGraphStatistics_Success() throws Exception {
        System.out.println("Testing graph statistics retrieval...");
        
        Map<String, Object> stats = Map.of(
            "totalNodes", 1000L,
            "totalEdges", 5000L,
            "nodeTypes", Map.of("PERSON", 400L, "ORGANIZATION", 200L, "DOCUMENT", 400L),
            "edgeTypes", Map.of("RELATED_TO", 2000L, "WORKS_FOR", 1500L, "AUTHORED", 1500L),
            "avgConnectionsPerNode", 5.0
        );
        
        when(graphTraversalService.getGraphStatistics()).thenReturn(stats);
        
        mockMvc.perform(get("/graph/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalNodes").value(1000))
            .andExpect(jsonPath("$.totalEdges").value(5000))
            .andExpect(jsonPath("$.avgConnectionsPerNode").value(5.0))
            .andExpect(jsonPath("$.nodeTypes.PERSON").value(400))
            .andExpect(jsonPath("$.edgeTypes.RELATED_TO").value(2000));
        
        verify(graphTraversalService).getGraphStatistics();
        
        System.out.println("✓ Graph statistics retrieval works correctly");
    }
    
    @Test
    @DisplayName("Error handling - Service exception")
    void testErrorHandling_ServiceException() throws Exception {
        System.out.println("Testing error handling for service exceptions...");
        
        when(graphTraversalService.getNeighborhood(any(UUID.class), anyInt()))
            .thenThrow(new RuntimeException("Database error"));
        
        mockMvc.perform(get("/graph/neighborhood/{nodeId}", UUID.randomUUID()))
            .andExpect(status().isInternalServerError());
        
        System.out.println("✓ Service exceptions handled correctly");
    }
    
    @Test
    @DisplayName("Invalid request body format")
    void testInvalidRequestBody() throws Exception {
        System.out.println("Testing invalid request body handling...");
        
        mockMvc.perform(post("/graph/subgraph")
                .contentType(MediaType.APPLICATION_JSON)
                .content("not a valid JSON"))
            .andExpect(status().isBadRequest());
        
        mockMvc.perform(post("/graph/centrality")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"format\"}"))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Invalid request bodies properly rejected");
    }
    
    @Test
    @DisplayName("Performance - Large neighborhood response")
    void testPerformance_LargeNeighborhood() throws Exception {
        System.out.println("Testing performance with large neighborhood...");
        
        // Create large neighborhood
        List<GraphNeighborhoodDTO.GraphNode> manyNodes = new ArrayList<>();
        List<GraphNeighborhoodDTO.GraphEdge> manyEdges = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            manyNodes.add(GraphNeighborhoodDTO.GraphNode.builder()
                .id(UUID.randomUUID())
                .type("NODE")
                .name("Node " + i)
                .hopLevel(1)
                .build());
        }
        
        for (int i = 0; i < 200; i++) {
            manyEdges.add(GraphNeighborhoodDTO.GraphEdge.builder()
                .id(UUID.randomUUID())
                .sourceId(testNodeId)
                .targetId(manyNodes.get(i % 100).getId())
                .type("EDGE")
                .hopLevel(1)
                .build());
        }
        
        GraphNeighborhoodDTO largeNeighborhood = GraphNeighborhoodDTO.builder()
            .centerNodeId(testNodeId)
            .nodes(manyNodes)
            .edges(manyEdges)
            .totalNodes((long) manyNodes.size())
            .totalEdges((long) manyEdges.size())
            .build();
        
        when(graphTraversalService.getNeighborhood(testNodeId, 1))
            .thenReturn(largeNeighborhood);
        
        long startTime = System.currentTimeMillis();
        
        MvcResult result = mockMvc.perform(get("/graph/neighborhood/{nodeId}", testNodeId))
            .andExpect(status().isOk())
            .andReturn();
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("Large neighborhood response time: " + duration + "ms");
        assertThat(duration).isLessThan(1000L);
        
        String response = result.getResponse().getContentAsString();
        assertThat(response).contains("\"totalNodes\":100");
        assertThat(response).contains("\"totalEdges\":200");
        
        System.out.println("✓ Large neighborhood handled efficiently");
    }
}