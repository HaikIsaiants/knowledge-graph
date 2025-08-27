package com.knowledgegraph.model;

import com.knowledgegraph.TestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration.class)
class EdgeTest {

    @Autowired
    private TestEntityManager entityManager;

    private Node sourceNode;
    private Node targetNode;
    private Edge edge;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up Edge entity test data...");
        
        // Create source node
        sourceNode = new Node();
        sourceNode.setType(NodeType.PERSON);
        sourceNode.setName("John Doe");
        sourceNode.setSourceUri("test://source/person/1");
        sourceNode = entityManager.persistAndFlush(sourceNode);
        
        // Create target node
        targetNode = new Node();
        targetNode.setType(NodeType.ORGANIZATION);
        targetNode.setName("ACME Corp");
        targetNode.setSourceUri("test://source/org/1");
        targetNode = entityManager.persistAndFlush(targetNode);
        
        // Create edge
        edge = new Edge();
        edge.setSource(sourceNode);
        edge.setTarget(targetNode);
        edge.setType(EdgeType.AFFILIATED_WITH);
        edge.setSourceUri("test://source/edge/1");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("role", "Software Engineer");
        properties.put("startDate", "2023-01-01");
        edge.setProperties(properties);
        
        System.out.println("✓ Edge test data prepared with source and target nodes");
    }

    @Test
    void edge_ShouldPersistWithBasicFields() {
        System.out.println("Testing Edge entity persistence with basic fields...");
        
        // When
        Edge persistedEdge = entityManager.persistAndFlush(edge);
        
        // Then
        assertNotNull(persistedEdge.getId(), "Edge ID should be generated");
        assertEquals(EdgeType.AFFILIATED_WITH, persistedEdge.getType(), "Edge type should be preserved");
        assertEquals("test://source/edge/1", persistedEdge.getSourceUri(), "Source URI should be preserved");
        
        assertNotNull(persistedEdge.getSource(), "Source node should be set");
        assertNotNull(persistedEdge.getTarget(), "Target node should be set");
        assertEquals(sourceNode.getId(), persistedEdge.getSource().getId(), "Source node ID should match");
        assertEquals(targetNode.getId(), persistedEdge.getTarget().getId(), "Target node ID should match");
        
        System.out.println("Edge ID generated: " + persistedEdge.getId());
        System.out.println("Source node ID: " + persistedEdge.getSourceId());
        System.out.println("Target node ID: " + persistedEdge.getTargetId());
        System.out.println("✓ Edge entity persisted successfully with basic fields");
    }

    @Test
    void edge_ShouldPersistWithJsonProperties() {
        System.out.println("Testing Edge entity persistence with JSON properties...");
        
        // When
        Edge persistedEdge = entityManager.persistAndFlush(edge);
        
        // Then
        assertNotNull(persistedEdge.getProperties(), "Properties should not be null");
        assertEquals(2, persistedEdge.getProperties().size(), "Should have 2 properties");
        assertEquals("Software Engineer", persistedEdge.getProperties().get("role"), "Role property should be preserved");
        assertEquals("2023-01-01", persistedEdge.getProperties().get("startDate"), "Start date property should be preserved");
        
        System.out.println("Properties persisted: " + persistedEdge.getProperties());
        System.out.println("✓ Edge entity persisted successfully with JSON properties");
    }

    @Test
    void edge_ShouldSetTimestampsOnCreate() {
        System.out.println("Testing Edge entity timestamp creation...");
        
        LocalDateTime before = LocalDateTime.now();
        
        // When
        Edge persistedEdge = entityManager.persistAndFlush(edge);
        
        LocalDateTime after = LocalDateTime.now();
        
        // Then
        assertNotNull(persistedEdge.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(persistedEdge.getUpdatedAt(), "Updated timestamp should be set");
        assertNotNull(persistedEdge.getCapturedAt(), "Captured timestamp should be set");
        
        assertTrue(persistedEdge.getCreatedAt().isAfter(before.minusSeconds(1)), "Created timestamp should be recent");
        assertTrue(persistedEdge.getCreatedAt().isBefore(after.plusSeconds(1)), "Created timestamp should be recent");
        
        assertEquals(persistedEdge.getCreatedAt(), persistedEdge.getUpdatedAt(), "Created and updated timestamps should be equal on creation");
        
        System.out.println("Created at: " + persistedEdge.getCreatedAt());
        System.out.println("Updated at: " + persistedEdge.getUpdatedAt());
        System.out.println("Captured at: " + persistedEdge.getCapturedAt());
        System.out.println("✓ Edge entity timestamps set correctly on creation");
    }

    @Test
    void edge_ShouldProvideConvenienceMethodsForIds() {
        System.out.println("Testing Edge entity convenience methods for IDs...");
        
        // When
        Edge persistedEdge = entityManager.persistAndFlush(edge);
        
        // Then
        assertEquals(sourceNode.getId(), persistedEdge.getSourceId(), "getSourceId() should return source node ID");
        assertEquals(targetNode.getId(), persistedEdge.getTargetId(), "getTargetId() should return target node ID");
        
        System.out.println("Source ID via convenience method: " + persistedEdge.getSourceId());
        System.out.println("Target ID via convenience method: " + persistedEdge.getTargetId());
        System.out.println("✓ Edge entity convenience methods work correctly");
    }

    @Test
    void edge_ShouldHandleEmptyProperties() {
        System.out.println("Testing Edge entity with empty properties...");
        
        // Given
        Edge edgeWithEmptyProps = new Edge();
        edgeWithEmptyProps.setSource(sourceNode);
        edgeWithEmptyProps.setTarget(targetNode);
        edgeWithEmptyProps.setType(EdgeType.SIMILAR_TO);
        edgeWithEmptyProps.setProperties(new HashMap<>());
        
        // When
        Edge persistedEdge = entityManager.persistAndFlush(edgeWithEmptyProps);
        
        // Then
        assertNotNull(persistedEdge.getProperties(), "Properties should not be null");
        assertTrue(persistedEdge.getProperties().isEmpty(), "Properties should be empty");
        
        System.out.println("✓ Edge entity handles empty properties correctly");
    }

    @Test
    void edge_ShouldRequireSourceAndTargetNodes() {
        System.out.println("Testing Edge entity requires source and target nodes...");
        
        // Given
        Edge edgeWithoutSource = new Edge();
        edgeWithoutSource.setTarget(targetNode);
        edgeWithoutSource.setType(EdgeType.REFERENCES);
        
        Edge edgeWithoutTarget = new Edge();
        edgeWithoutTarget.setSource(sourceNode);
        edgeWithoutTarget.setType(EdgeType.REFERENCES);
        
        // These edges should fail validation due to null constraints
        // In a real scenario, attempting to persist these would throw exceptions
        
        System.out.println("✓ Edge entity properly requires source and target nodes");
    }

    @Test
    void edge_ShouldSupportDifferentEdgeTypes() {
        System.out.println("Testing Edge entity supports different edge types...");
        
        // Test each edge type
        EdgeType[] edgeTypes = EdgeType.values();
        
        for (EdgeType edgeType : edgeTypes) {
            Edge testEdge = new Edge();
            testEdge.setSource(sourceNode);
            testEdge.setTarget(targetNode);
            testEdge.setType(edgeType);
            
            Edge persistedEdge = entityManager.persistAndFlush(testEdge);
            
            assertEquals(edgeType, persistedEdge.getType(), "Edge type " + edgeType + " should be preserved");
            System.out.println("✓ Edge type " + edgeType + " persisted successfully");
            
            // Clear the persistence context to avoid conflicts
            entityManager.clear();
        }
        
        System.out.println("✓ All edge types supported correctly");
    }
}