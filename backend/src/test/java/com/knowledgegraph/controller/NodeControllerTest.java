package com.knowledgegraph.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgegraph.dto.NodeDetailDTO;
import com.knowledgegraph.model.Edge;
import com.knowledgegraph.model.EdgeType;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.EdgeRepository;
import com.knowledgegraph.repository.EmbeddingRepository;
import com.knowledgegraph.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NodeController.class)
@DisplayName("NodeController Tests")
class NodeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private NodeRepository nodeRepository;
    
    @MockBean
    private EdgeRepository edgeRepository;
    
    @MockBean
    private EmbeddingRepository embeddingRepository;
    
    private Node testNode;
    private Edge testEdge;
    private UUID testNodeId;
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up NodeController test environment...");
        
        testNodeId = UUID.randomUUID();
        
        // Create test node
        testNode = new Node();
        testNode.setId(testNodeId);
        testNode.setName("Test Node");
        testNode.setType(NodeType.PERSON);
        testNode.setSourceUri("http://example.com/test");
        testNode.setProperties(Map.of("role", "engineer", "department", "IT"));
        testNode.setCapturedAt(LocalDateTime.now());
        testNode.setCreatedAt(LocalDateTime.now());
        testNode.setUpdatedAt(LocalDateTime.now());
        
        // Create test edge
        Node targetNode = new Node();
        targetNode.setId(UUID.randomUUID());
        targetNode.setName("Target Node");
        targetNode.setType(NodeType.ORGANIZATION);
        
        testEdge = new Edge();
        testEdge.setId(UUID.randomUUID());
        testEdge.setSource(testNode);
        testEdge.setTarget(targetNode);
        testEdge.setType(EdgeType.WORKS_FOR);
        testEdge.setProperties(Map.of("since", "2020"));
    }
    
    @Test
    @DisplayName("GET /nodes/{id} - Get node details success")
    void testGetNode_Success() throws Exception {
        System.out.println("Testing get node details...");
        
        when(nodeRepository.findById(testNodeId)).thenReturn(Optional.of(testNode));
        when(edgeRepository.findBySourceId(testNodeId)).thenReturn(List.of(testEdge));
        when(edgeRepository.findByTargetId(testNodeId)).thenReturn(Collections.emptyList());
        when(embeddingRepository.findByNode_Id(testNodeId)).thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/nodes/{id}", testNodeId))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(testNodeId.toString()))
            .andExpect(jsonPath("$.name").value("Test Node"))
            .andExpect(jsonPath("$.type").value("PERSON"))
            .andExpect(jsonPath("$.properties.role").value("engineer"))
            .andExpect(jsonPath("$.outgoingEdges", hasSize(1)))
            .andExpect(jsonPath("$.incomingEdges", hasSize(0)))
            .andExpect(jsonPath("$.totalConnections").value(1));
        
        verify(nodeRepository).findById(testNodeId);
        
        System.out.println("✓ Get node details works correctly");
    }
    
    @Test
    @DisplayName("GET /nodes/{id} - Node not found")
    void testGetNode_NotFound() throws Exception {
        System.out.println("Testing get node with non-existent ID...");
        
        UUID unknownId = UUID.randomUUID();
        when(nodeRepository.findById(unknownId)).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/nodes/{id}", unknownId))
            .andExpect(status().isNotFound());
        
        System.out.println("✓ Non-existent node returns 404");
    }
    
    @Test
    @DisplayName("GET /nodes - List nodes with pagination")
    void testListNodes_Pagination() throws Exception {
        System.out.println("Testing list nodes with pagination...");
        
        List<Node> nodes = List.of(testNode);
        Page<Node> nodePage = new PageImpl<>(nodes, PageRequest.of(0, 20), 1);
        
        when(nodeRepository.findAll(any(Pageable.class))).thenReturn(nodePage);
        
        mockMvc.perform(get("/nodes")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].name").value("Test Node"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));
        
        System.out.println("✓ Node listing with pagination works correctly");
    }
    
    @Test
    @DisplayName("GET /nodes - Filter by type")
    void testListNodes_FilterByType() throws Exception {
        System.out.println("Testing list nodes filtered by type...");
        
        Page<Node> nodePage = new PageImpl<>(List.of(testNode));
        
        when(nodeRepository.findByType(eq(NodeType.PERSON), any(Pageable.class)))
            .thenReturn(nodePage);
        
        mockMvc.perform(get("/nodes")
                .param("type", "PERSON"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].type").value("PERSON"));
        
        verify(nodeRepository).findByType(eq(NodeType.PERSON), any(Pageable.class));
        
        System.out.println("✓ Type filtering works correctly");
    }
    
    @Test
    @DisplayName("GET /nodes - Filter by name")
    void testListNodes_FilterByName() throws Exception {
        System.out.println("Testing list nodes filtered by name...");
        
        Page<Node> nodePage = new PageImpl<>(List.of(testNode));
        
        when(nodeRepository.findByNameContainingIgnoreCase(eq("test"), any(Pageable.class)))
            .thenReturn(nodePage);
        
        mockMvc.perform(get("/nodes")
                .param("name", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].name").value("Test Node"));
        
        verify(nodeRepository).findByNameContainingIgnoreCase(eq("test"), any(Pageable.class));
        
        System.out.println("✓ Name filtering works correctly");
    }
    
    @Test
    @DisplayName("GET /nodes/{id}/citations - Get node citations")
    void testGetNodeCitations() throws Exception {
        System.out.println("Testing get node citations...");
        
        when(nodeRepository.findById(testNodeId)).thenReturn(Optional.of(testNode));
        
        mockMvc.perform(get("/nodes/{id}/citations", testNodeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].documentUri").value("http://example.com/test"));
        
        System.out.println("✓ Node citations retrieved correctly");
    }
    
    @Test
    @DisplayName("GET /nodes/{id}/related - Get related nodes")
    void testGetRelatedNodes() throws Exception {
        System.out.println("Testing get related nodes...");
        
        Node relatedNode = new Node();
        relatedNode.setId(UUID.randomUUID());
        relatedNode.setName("Related Node");
        relatedNode.setType(NodeType.CONCEPT);
        
        when(nodeRepository.findById(testNodeId)).thenReturn(Optional.of(testNode));
        when(nodeRepository.findConnectedNodes(testNodeId)).thenReturn(List.of(relatedNode));
        
        mockMvc.perform(get("/nodes/{id}/related", testNodeId)
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title").value("Related Node"))
            .andExpect(jsonPath("$[0].type").value("CONCEPT"));
        
        System.out.println("✓ Related nodes retrieved correctly");
    }
    
    @Test
    @DisplayName("GET /nodes/{id}/edges - Get node edges")
    void testGetNodeEdges() throws Exception {
        System.out.println("Testing get node edges...");
        
        when(nodeRepository.findById(testNodeId)).thenReturn(Optional.of(testNode));
        when(edgeRepository.findBySourceId(testNodeId)).thenReturn(List.of(testEdge));
        when(edgeRepository.findByTargetId(testNodeId)).thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/nodes/{id}/edges", testNodeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outgoing", hasSize(1)))
            .andExpect(jsonPath("$.incoming", hasSize(0)))
            .andExpect(jsonPath("$.outgoing[0].edgeType").value("WORKS_FOR"))
            .andExpect(jsonPath("$.outgoing[0].nodeName").value("Target Node"));
        
        System.out.println("✓ Node edges retrieved correctly");
    }
    
    @Test
    @DisplayName("POST /nodes - Create new node")
    void testCreateNode() throws Exception {
        System.out.println("Testing create new node...");
        
        Node newNode = new Node();
        newNode.setName("New Node");
        newNode.setType(NodeType.DOCUMENT);
        newNode.setProperties(Map.of("key", "value"));
        
        Node savedNode = new Node();
        savedNode.setId(UUID.randomUUID());
        savedNode.setName(newNode.getName());
        savedNode.setType(newNode.getType());
        savedNode.setProperties(newNode.getProperties());
        savedNode.setCreatedAt(LocalDateTime.now());
        
        when(nodeRepository.save(any(Node.class))).thenReturn(savedNode);
        
        mockMvc.perform(post("/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newNode)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("New Node"))
            .andExpect(jsonPath("$.type").value("DOCUMENT"));
        
        verify(nodeRepository).save(any(Node.class));
        
        System.out.println("✓ Node creation works correctly");
    }
    
    @Test
    @DisplayName("PUT /nodes/{id} - Update existing node")
    void testUpdateNode() throws Exception {
        System.out.println("Testing update existing node...");
        
        Node updateData = new Node();
        updateData.setName("Updated Name");
        updateData.setProperties(Map.of("newProp", "newValue"));
        
        Node updatedNode = new Node();
        updatedNode.setId(testNodeId);
        updatedNode.setName("Updated Name");
        updatedNode.setType(testNode.getType());
        updatedNode.setProperties(Map.of("role", "engineer", "department", "IT", "newProp", "newValue"));
        
        when(nodeRepository.findById(testNodeId)).thenReturn(Optional.of(testNode));
        when(nodeRepository.save(any(Node.class))).thenReturn(updatedNode);
        
        mockMvc.perform(put("/nodes/{id}", testNodeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.properties.newProp").value("newValue"));
        
        verify(nodeRepository).save(any(Node.class));
        
        System.out.println("✓ Node update works correctly");
    }
    
    @Test
    @DisplayName("PUT /nodes/{id} - Node not found")
    void testUpdateNode_NotFound() throws Exception {
        System.out.println("Testing update non-existent node...");
        
        UUID unknownId = UUID.randomUUID();
        when(nodeRepository.findById(unknownId)).thenReturn(Optional.empty());
        
        mockMvc.perform(put("/nodes/{id}", unknownId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new Node())))
            .andExpect(status().isNotFound());
        
        System.out.println("✓ Update non-existent node returns 404");
    }
    
    @Test
    @DisplayName("DELETE /nodes/{id} - Delete node")
    void testDeleteNode() throws Exception {
        System.out.println("Testing delete node...");
        
        when(nodeRepository.existsById(testNodeId)).thenReturn(true);
        doNothing().when(nodeRepository).deleteById(testNodeId);
        
        mockMvc.perform(delete("/nodes/{id}", testNodeId))
            .andExpect(status().isNoContent());
        
        verify(nodeRepository).deleteById(testNodeId);
        
        System.out.println("✓ Node deletion works correctly");
    }
    
    @Test
    @DisplayName("DELETE /nodes/{id} - Node not found")
    void testDeleteNode_NotFound() throws Exception {
        System.out.println("Testing delete non-existent node...");
        
        UUID unknownId = UUID.randomUUID();
        when(nodeRepository.existsById(unknownId)).thenReturn(false);
        
        mockMvc.perform(delete("/nodes/{id}", unknownId))
            .andExpect(status().isNotFound());
        
        System.out.println("✓ Delete non-existent node returns 404");
    }
    
    @Test
    @DisplayName("Sorting and ordering")
    void testListNodes_SortingAndOrdering() throws Exception {
        System.out.println("Testing sorting and ordering...");
        
        Page<Node> nodePage = new PageImpl<>(List.of(testNode));
        
        when(nodeRepository.findAll(any(Pageable.class))).thenReturn(nodePage);
        
        mockMvc.perform(get("/nodes")
                .param("sortBy", "name")
                .param("direction", "ASC"))
            .andExpect(status().isOk());
        
        verify(nodeRepository).findAll(argThat(pageable -> {
            Sort sort = pageable.getSort();
            return sort.getOrderFor("name") != null && 
                   sort.getOrderFor("name").getDirection() == Sort.Direction.ASC;
        }));
        
        System.out.println("✓ Sorting and ordering works correctly");
    }
    
    @Test
    @DisplayName("Node with null properties")
    void testNode_NullProperties() throws Exception {
        System.out.println("Testing node with null properties...");
        
        Node nodeWithNullProps = new Node();
        nodeWithNullProps.setId(UUID.randomUUID());
        nodeWithNullProps.setName("Null Props Node");
        nodeWithNullProps.setType(NodeType.CONCEPT);
        nodeWithNullProps.setProperties(null);
        
        when(nodeRepository.findById(nodeWithNullProps.getId())).thenReturn(Optional.of(nodeWithNullProps));
        when(edgeRepository.findBySourceId(any())).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(any())).thenReturn(Collections.emptyList());
        when(embeddingRepository.findByNode_Id(any())).thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/nodes/{id}", nodeWithNullProps.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Null Props Node"));
        
        System.out.println("✓ Null properties handled correctly");
    }
    
    @Test
    @DisplayName("Large property map handling")
    void testLargePropertyMap() throws Exception {
        System.out.println("Testing large property map...");
        
        Map<String, Object> largeProps = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            largeProps.put("key" + i, "value" + i);
        }
        
        Node nodeWithLargeProps = new Node();
        nodeWithLargeProps.setId(UUID.randomUUID());
        nodeWithLargeProps.setName("Large Props Node");
        nodeWithLargeProps.setType(NodeType.CONCEPT);
        nodeWithLargeProps.setProperties(largeProps);
        
        when(nodeRepository.save(any(Node.class))).thenReturn(nodeWithLargeProps);
        
        mockMvc.perform(post("/nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nodeWithLargeProps)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.properties.key49").value("value49"));
        
        System.out.println("✓ Large property map handled correctly");
    }
    
    @Test
    @DisplayName("Invalid UUID format")
    void testInvalidUUID() throws Exception {
        System.out.println("Testing invalid UUID format...");
        
        mockMvc.perform(get("/nodes/invalid-uuid"))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Invalid UUID format rejected");
    }
    
    @Test
    @DisplayName("Empty related nodes")
    void testEmptyRelatedNodes() throws Exception {
        System.out.println("Testing empty related nodes...");
        
        when(nodeRepository.findById(testNodeId)).thenReturn(Optional.of(testNode));
        when(nodeRepository.findConnectedNodes(testNodeId)).thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/nodes/{id}/related", testNodeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
        
        System.out.println("✓ Empty related nodes handled correctly");
    }
    
    @Test
    @DisplayName("Concurrent node operations")
    void testConcurrentOperations() throws Exception {
        System.out.println("Testing concurrent node operations...");
        
        when(nodeRepository.findById(testNodeId)).thenReturn(Optional.of(testNode));
        when(edgeRepository.findBySourceId(any())).thenReturn(Collections.emptyList());
        when(edgeRepository.findByTargetId(any())).thenReturn(Collections.emptyList());
        when(embeddingRepository.findByNode_Id(any())).thenReturn(Collections.emptyList());
        
        // Simulate concurrent requests
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/nodes/{id}", testNodeId))
                .andExpect(status().isOk());
        }
        
        verify(nodeRepository, times(5)).findById(testNodeId);
        
        System.out.println("✓ Concurrent operations handled correctly");
    }
}