package com.knowledgegraph.repository;

import com.knowledgegraph.TestConfiguration;
import com.knowledgegraph.model.Edge;
import com.knowledgegraph.model.EdgeType;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration.class)
class EdgeRepositoryTest {

    @Autowired
    private EdgeRepository edgeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Node personNode;
    private Node organizationNode;
    private Node eventNode;
    private Edge affiliationEdge;
    private Edge participationEdge;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up EdgeRepository test data...");
        
        // Create nodes
        personNode = new Node();
        personNode.setType(NodeType.PERSON);
        personNode.setName("John Doe");
        personNode.setSourceUri("test://source/person/1");
        personNode = entityManager.persistAndFlush(personNode);
        
        organizationNode = new Node();
        organizationNode.setType(NodeType.ORGANIZATION);
        organizationNode.setName("ACME Corporation");
        organizationNode.setSourceUri("test://source/org/1");
        organizationNode = entityManager.persistAndFlush(organizationNode);
        
        eventNode = new Node();
        eventNode.setType(NodeType.EVENT);
        eventNode.setName("Tech Conference 2023");
        eventNode.setSourceUri("test://source/event/1");
        eventNode = entityManager.persistAndFlush(eventNode);
        
        // Create edges
        affiliationEdge = new Edge();
        affiliationEdge.setSource(personNode);
        affiliationEdge.setTarget(organizationNode);
        affiliationEdge.setType(EdgeType.AFFILIATED_WITH);
        affiliationEdge.setSourceUri("test://source/edge/1");
        Map<String, Object> affiliationProps = new HashMap<>();
        affiliationProps.put("role", "Software Engineer");
        affiliationProps.put("startDate", "2023-01-01");
        affiliationEdge.setProperties(affiliationProps);
        affiliationEdge = entityManager.persistAndFlush(affiliationEdge);
        
        participationEdge = new Edge();
        participationEdge.setSource(personNode);
        participationEdge.setTarget(eventNode);
        participationEdge.setType(EdgeType.PARTICIPATED_IN);
        participationEdge.setSourceUri("test://source/edge/2");
        Map<String, Object> participationProps = new HashMap<>();
        participationProps.put("role", "Speaker");
        participationEdge.setProperties(participationProps);
        participationEdge = entityManager.persistAndFlush(participationEdge);
        
        System.out.println("✓ EdgeRepository test data prepared with 2 edges and 3 nodes");
    }

    @Test
    void findAll_ShouldReturnAllEdges() {
        System.out.println("Testing EdgeRepository findAll method...");
        
        // When
        List<Edge> allEdges = edgeRepository.findAll();
        
        // Then
        assertNotNull(allEdges, "Edge list should not be null");
        assertEquals(2, allEdges.size(), "Should find 2 edges");
        
        System.out.println("Found " + allEdges.size() + " edges total");
        System.out.println("✓ EdgeRepository findAll works correctly");
    }

    @Test
    void findById_ShouldReturnCorrectEdge() {
        System.out.println("Testing EdgeRepository findById method...");
        
        // When
        Optional<Edge> foundEdge = edgeRepository.findById(affiliationEdge.getId());
        
        // Then
        assertTrue(foundEdge.isPresent(), "Edge should be found");
        assertEquals(affiliationEdge.getType(), foundEdge.get().getType(), "Edge type should match");
        assertEquals(affiliationEdge.getSourceId(), foundEdge.get().getSourceId(), "Source ID should match");
        assertEquals(affiliationEdge.getTargetId(), foundEdge.get().getTargetId(), "Target ID should match");
        
        System.out.println("Found edge by ID with type: " + foundEdge.get().getType());
        System.out.println("✓ EdgeRepository findById works correctly");
    }

    @Test
    void save_ShouldPersistNewEdge() {
        System.out.println("Testing EdgeRepository save method for new edge...");
        
        // Given
        Edge newEdge = new Edge();
        newEdge.setSource(organizationNode);
        newEdge.setTarget(eventNode);
        newEdge.setType(EdgeType.PRODUCED_BY);
        newEdge.setSourceUri("test://source/edge/3");
        Map<String, Object> props = new HashMap<>();
        props.put("sponsorshipLevel", "Gold");
        newEdge.setProperties(props);
        
        // When
        Edge savedEdge = edgeRepository.save(newEdge);
        
        // Then
        assertNotNull(savedEdge.getId(), "Saved edge should have an ID");
        assertEquals(EdgeType.PRODUCED_BY, savedEdge.getType(), "Edge type should be preserved");
        assertEquals(organizationNode.getId(), savedEdge.getSourceId(), "Source ID should be preserved");
        assertEquals(eventNode.getId(), savedEdge.getTargetId(), "Target ID should be preserved");
        assertEquals("Gold", savedEdge.getProperties().get("sponsorshipLevel"), "Properties should be preserved");
        
        System.out.println("Saved new edge with ID: " + savedEdge.getId());
        System.out.println("✓ EdgeRepository save works correctly for new edge");
    }

    @Test
    void findBySourceId_ShouldReturnEdgesFromSpecificSource() {
        System.out.println("Testing EdgeRepository findBySourceId method...");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Edge> edgesFromPerson = edgeRepository.findBySourceId(personNode.getId(), pageable);
        Page<Edge> edgesFromOrg = edgeRepository.findBySourceId(organizationNode.getId(), pageable);
        
        // Then
        assertEquals(2, edgesFromPerson.getTotalElements(), "Should find 2 edges from person node");
        assertEquals(0, edgesFromOrg.getTotalElements(), "Should find 0 edges from organization node");
        
        // Verify the edges are correct
        List<Edge> personEdges = edgesFromPerson.getContent();
        assertTrue(personEdges.stream().anyMatch(e -> e.getType() == EdgeType.AFFILIATED_WITH), 
                   "Should include affiliation edge");
        assertTrue(personEdges.stream().anyMatch(e -> e.getType() == EdgeType.PARTICIPATED_IN), 
                   "Should include participation edge");
        
        System.out.println("Found " + edgesFromPerson.getTotalElements() + " edges from person node");
        System.out.println("Found " + edgesFromOrg.getTotalElements() + " edges from organization node");
        System.out.println("✓ EdgeRepository findBySourceId works correctly");
    }

    @Test
    void findByTargetId_ShouldReturnEdgesToSpecificTarget() {
        System.out.println("Testing EdgeRepository findByTargetId method...");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Edge> edgesToOrg = edgeRepository.findByTargetId(organizationNode.getId(), pageable);
        Page<Edge> edgesToEvent = edgeRepository.findByTargetId(eventNode.getId(), pageable);
        Page<Edge> edgesToPerson = edgeRepository.findByTargetId(personNode.getId(), pageable);
        
        // Then
        assertEquals(1, edgesToOrg.getTotalElements(), "Should find 1 edge to organization node");
        assertEquals(1, edgesToEvent.getTotalElements(), "Should find 1 edge to event node");
        assertEquals(0, edgesToPerson.getTotalElements(), "Should find 0 edges to person node");
        
        assertEquals(EdgeType.AFFILIATED_WITH, edgesToOrg.getContent().get(0).getType(), "Edge to org should be affiliation");
        assertEquals(EdgeType.PARTICIPATED_IN, edgesToEvent.getContent().get(0).getType(), "Edge to event should be participation");
        
        System.out.println("Found " + edgesToOrg.getTotalElements() + " edges to organization node");
        System.out.println("Found " + edgesToEvent.getTotalElements() + " edges to event node");
        System.out.println("Found " + edgesToPerson.getTotalElements() + " edges to person node");
        System.out.println("✓ EdgeRepository findByTargetId works correctly");
    }

    @Test
    void findByType_ShouldReturnEdgesOfSpecificType() {
        System.out.println("Testing EdgeRepository findByType method...");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Edge> affiliationEdges = edgeRepository.findByType(EdgeType.AFFILIATED_WITH, pageable);
        Page<Edge> participationEdges = edgeRepository.findByType(EdgeType.PARTICIPATED_IN, pageable);
        Page<Edge> referenceEdges = edgeRepository.findByType(EdgeType.REFERENCES, pageable);
        
        // Then
        assertEquals(1, affiliationEdges.getTotalElements(), "Should find 1 affiliation edge");
        assertEquals(1, participationEdges.getTotalElements(), "Should find 1 participation edge");
        assertEquals(0, referenceEdges.getTotalElements(), "Should find 0 reference edges");
        
        System.out.println("Found " + affiliationEdges.getTotalElements() + " affiliation edges");
        System.out.println("Found " + participationEdges.getTotalElements() + " participation edges");
        System.out.println("Found " + referenceEdges.getTotalElements() + " reference edges");
        System.out.println("✓ EdgeRepository findByType works correctly");
    }

    @Test
    void findBySourceIdAndTargetId_ShouldReturnDirectEdgesBetweenNodes() {
        System.out.println("Testing EdgeRepository findBySourceIdAndTargetId method...");
        
        // When
        List<Edge> personToOrgEdges = edgeRepository.findBySourceIdAndTargetId(personNode.getId(), organizationNode.getId());
        List<Edge> orgToPersonEdges = edgeRepository.findBySourceIdAndTargetId(organizationNode.getId(), personNode.getId());
        List<Edge> personToEventEdges = edgeRepository.findBySourceIdAndTargetId(personNode.getId(), eventNode.getId());
        
        // Then
        assertEquals(1, personToOrgEdges.size(), "Should find 1 edge from person to organization");
        assertEquals(0, orgToPersonEdges.size(), "Should find 0 edges from organization to person");
        assertEquals(1, personToEventEdges.size(), "Should find 1 edge from person to event");
        
        assertEquals(EdgeType.AFFILIATED_WITH, personToOrgEdges.get(0).getType(), "Person to org edge should be affiliation");
        assertEquals(EdgeType.PARTICIPATED_IN, personToEventEdges.get(0).getType(), "Person to event edge should be participation");
        
        System.out.println("Found " + personToOrgEdges.size() + " edges from person to organization");
        System.out.println("Found " + personToEventEdges.size() + " edges from person to event");
        System.out.println("✓ EdgeRepository findBySourceIdAndTargetId works correctly");
    }

    @Test
    void findBySourceUri_ShouldReturnEdgesFromSpecificSource() {
        System.out.println("Testing EdgeRepository findBySourceUri method...");
        
        // When
        List<Edge> edgesFromSourceUri = edgeRepository.findBySourceUri("test://source/edge/1");
        
        // Then
        assertEquals(1, edgesFromSourceUri.size(), "Should find 1 edge from specific source URI");
        assertEquals(EdgeType.AFFILIATED_WITH, edgesFromSourceUri.get(0).getType(), "Edge should be affiliation type");
        assertEquals("test://source/edge/1", edgesFromSourceUri.get(0).getSourceUri(), "Source URI should match");
        
        System.out.println("Found edge from source URI: " + edgesFromSourceUri.get(0).getSourceUri());
        System.out.println("✓ EdgeRepository findBySourceUri works correctly");
    }

    @Test
    void existsBySourceIdAndTargetIdAndType_ShouldReturnCorrectExistenceStatus() {
        System.out.println("Testing EdgeRepository existsBySourceIdAndTargetIdAndType method...");
        
        // When
        boolean affiliationExists = edgeRepository.existsBySourceIdAndTargetIdAndType(
                personNode.getId(), organizationNode.getId(), EdgeType.AFFILIATED_WITH);
        boolean participationExists = edgeRepository.existsBySourceIdAndTargetIdAndType(
                personNode.getId(), eventNode.getId(), EdgeType.PARTICIPATED_IN);
        boolean nonExistentEdgeExists = edgeRepository.existsBySourceIdAndTargetIdAndType(
                organizationNode.getId(), personNode.getId(), EdgeType.AFFILIATED_WITH);
        
        // Then
        assertTrue(affiliationExists, "Person to organization affiliation should exist");
        assertTrue(participationExists, "Person to event participation should exist");
        assertFalse(nonExistentEdgeExists, "Organization to person affiliation should not exist");
        
        System.out.println("Person->Organization affiliation exists: " + affiliationExists);
        System.out.println("Person->Event participation exists: " + participationExists);
        System.out.println("Organization->Person affiliation exists: " + nonExistentEdgeExists);
        System.out.println("✓ EdgeRepository existsBySourceIdAndTargetIdAndType works correctly");
    }

    @Test
    void findAllConnectedEdges_ShouldReturnAllEdgesConnectedToNode() {
        System.out.println("Testing EdgeRepository findAllConnectedEdges method...");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Edge> personConnectedEdges = edgeRepository.findAllConnectedEdges(personNode.getId(), pageable);
        Page<Edge> orgConnectedEdges = edgeRepository.findAllConnectedEdges(organizationNode.getId(), pageable);
        Page<Edge> eventConnectedEdges = edgeRepository.findAllConnectedEdges(eventNode.getId(), pageable);
        
        // Then
        assertEquals(2, personConnectedEdges.getTotalElements(), "Person should have 2 connected edges");
        assertEquals(1, orgConnectedEdges.getTotalElements(), "Organization should have 1 connected edge");
        assertEquals(1, eventConnectedEdges.getTotalElements(), "Event should have 1 connected edge");
        
        // Verify person edges include both outgoing edges
        List<Edge> personEdges = personConnectedEdges.getContent();
        assertTrue(personEdges.stream().anyMatch(e -> e.getType() == EdgeType.AFFILIATED_WITH), 
                   "Person should have affiliation edge");
        assertTrue(personEdges.stream().anyMatch(e -> e.getType() == EdgeType.PARTICIPATED_IN), 
                   "Person should have participation edge");
        
        System.out.println("Person connected edges: " + personConnectedEdges.getTotalElements());
        System.out.println("Organization connected edges: " + orgConnectedEdges.getTotalElements());
        System.out.println("Event connected edges: " + eventConnectedEdges.getTotalElements());
        System.out.println("✓ EdgeRepository findAllConnectedEdges works correctly");
    }

    @Test
    void delete_ShouldRemoveEdge() {
        System.out.println("Testing EdgeRepository delete method...");
        
        // Given - verify edge exists
        assertTrue(edgeRepository.existsById(participationEdge.getId()), "Participation edge should exist before deletion");
        
        // When
        edgeRepository.deleteById(participationEdge.getId());
        entityManager.flush();
        
        // Then
        assertFalse(edgeRepository.existsById(participationEdge.getId()), "Participation edge should not exist after deletion");
        
        List<Edge> remainingEdges = edgeRepository.findAll();
        assertEquals(1, remainingEdges.size(), "Should have 1 remaining edge after deletion");
        assertEquals(EdgeType.AFFILIATED_WITH, remainingEdges.get(0).getType(), "Remaining edge should be affiliation");
        
        System.out.println("Edges remaining after deletion: " + remainingEdges.size());
        System.out.println("✓ EdgeRepository delete works correctly");
    }

    @Test
    void update_ShouldModifyExistingEdge() {
        System.out.println("Testing EdgeRepository update operation...");
        
        // Given
        Edge edgeToUpdate = edgeRepository.findById(affiliationEdge.getId()).orElseThrow();
        Map<String, Object> originalProperties = new HashMap<>(edgeToUpdate.getProperties());
        
        // When
        edgeToUpdate.getProperties().put("salary", "$75000");
        edgeToUpdate.getProperties().put("endDate", "2023-12-31");
        Edge updatedEdge = edgeRepository.save(edgeToUpdate);
        
        // Then
        assertEquals("$75000", updatedEdge.getProperties().get("salary"), "New salary property should be added");
        assertEquals("2023-12-31", updatedEdge.getProperties().get("endDate"), "New end date property should be added");
        assertEquals(originalProperties.get("role"), updatedEdge.getProperties().get("role"), "Original role property should be preserved");
        assertEquals(originalProperties.get("startDate"), updatedEdge.getProperties().get("startDate"), "Original start date property should be preserved");
        
        System.out.println("Added salary property: " + updatedEdge.getProperties().get("salary"));
        System.out.println("Added end date property: " + updatedEdge.getProperties().get("endDate"));
        System.out.println("Preserved role property: " + updatedEdge.getProperties().get("role"));
        System.out.println("✓ EdgeRepository update works correctly");
    }

    @Test
    void count_ShouldReturnCorrectEdgeCount() {
        System.out.println("Testing EdgeRepository count method...");
        
        // When
        long edgeCount = edgeRepository.count();
        
        // Then
        assertEquals(2, edgeCount, "Should count 2 edges total");
        
        System.out.println("Total edge count: " + edgeCount);
        System.out.println("✓ EdgeRepository count works correctly");
    }
}