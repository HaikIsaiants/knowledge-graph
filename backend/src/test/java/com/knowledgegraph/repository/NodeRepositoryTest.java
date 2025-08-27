package com.knowledgegraph.repository;

import com.knowledgegraph.TestConfiguration;
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
class NodeRepositoryTest {

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Node personNode;
    private Node organizationNode;
    private Node conceptNode;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up NodeRepository test data...");
        
        // Create person node
        personNode = new Node();
        personNode.setType(NodeType.PERSON);
        personNode.setName("John Doe");
        personNode.setSourceUri("test://source/person/1");
        Map<String, Object> personProps = new HashMap<>();
        personProps.put("age", 30);
        personNode.setProperties(personProps);
        
        // Create organization node
        organizationNode = new Node();
        organizationNode.setType(NodeType.ORGANIZATION);
        organizationNode.setName("ACME Corporation");
        organizationNode.setSourceUri("test://source/org/1");
        Map<String, Object> orgProps = new HashMap<>();
        orgProps.put("industry", "Technology");
        organizationNode.setProperties(orgProps);
        
        // Create concept node
        conceptNode = new Node();
        conceptNode.setType(NodeType.CONCEPT);
        conceptNode.setName("Machine Learning");
        conceptNode.setSourceUri("test://source/concept/1");
        
        // Persist test data
        personNode = entityManager.persistAndFlush(personNode);
        organizationNode = entityManager.persistAndFlush(organizationNode);
        conceptNode = entityManager.persistAndFlush(conceptNode);
        
        System.out.println("✓ NodeRepository test data prepared with 3 nodes");
    }

    @Test
    void findAll_ShouldReturnAllNodes() {
        System.out.println("Testing NodeRepository findAll method...");
        
        // When
        List<Node> allNodes = nodeRepository.findAll();
        
        // Then
        assertNotNull(allNodes, "Node list should not be null");
        assertEquals(3, allNodes.size(), "Should find 3 nodes");
        
        System.out.println("Found " + allNodes.size() + " nodes total");
        System.out.println("✓ NodeRepository findAll works correctly");
    }

    @Test
    void findById_ShouldReturnCorrectNode() {
        System.out.println("Testing NodeRepository findById method...");
        
        // When
        Optional<Node> foundNode = nodeRepository.findById(personNode.getId());
        
        // Then
        assertTrue(foundNode.isPresent(), "Node should be found");
        assertEquals(personNode.getName(), foundNode.get().getName(), "Node name should match");
        assertEquals(personNode.getType(), foundNode.get().getType(), "Node type should match");
        
        System.out.println("Found node by ID: " + foundNode.get().getName());
        System.out.println("✓ NodeRepository findById works correctly");
    }

    @Test
    void save_ShouldPersistNewNode() {
        System.out.println("Testing NodeRepository save method for new node...");
        
        // Given
        Node newNode = new Node();
        newNode.setType(NodeType.EVENT);
        newNode.setName("Knowledge Graph Conference 2023");
        newNode.setSourceUri("test://source/event/1");
        
        // When
        Node savedNode = nodeRepository.save(newNode);
        
        // Then
        assertNotNull(savedNode.getId(), "Saved node should have an ID");
        assertEquals("Knowledge Graph Conference 2023", savedNode.getName(), "Node name should be preserved");
        assertEquals(NodeType.EVENT, savedNode.getType(), "Node type should be preserved");
        
        // Verify persistence
        Optional<Node> foundNode = nodeRepository.findById(savedNode.getId());
        assertTrue(foundNode.isPresent(), "Saved node should be findable");
        
        System.out.println("Saved new node with ID: " + savedNode.getId());
        System.out.println("✓ NodeRepository save works correctly for new node");
    }

    @Test
    void findByType_ShouldReturnNodesOfSpecificType() {
        System.out.println("Testing NodeRepository findByType method...");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Node> personNodes = nodeRepository.findByType(NodeType.PERSON, pageable);
        Page<Node> organizationNodes = nodeRepository.findByType(NodeType.ORGANIZATION, pageable);
        
        // Then
        assertEquals(1, personNodes.getTotalElements(), "Should find 1 person node");
        assertEquals(1, organizationNodes.getTotalElements(), "Should find 1 organization node");
        
        assertEquals(NodeType.PERSON, personNodes.getContent().get(0).getType(), "Person node should have correct type");
        assertEquals(NodeType.ORGANIZATION, organizationNodes.getContent().get(0).getType(), "Organization node should have correct type");
        
        System.out.println("Found " + personNodes.getTotalElements() + " person nodes");
        System.out.println("Found " + organizationNodes.getTotalElements() + " organization nodes");
        System.out.println("✓ NodeRepository findByType works correctly");
    }

    @Test
    void findByNameContainingIgnoreCase_ShouldReturnMatchingNodes() {
        System.out.println("Testing NodeRepository findByNameContainingIgnoreCase method...");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Node> nodesWithJohn = nodeRepository.findByNameContainingIgnoreCase("john", pageable);
        Page<Node> nodesWithAcme = nodeRepository.findByNameContainingIgnoreCase("acme", pageable);
        Page<Node> nodesWithMachine = nodeRepository.findByNameContainingIgnoreCase("MACHINE", pageable);
        
        // Then
        assertEquals(1, nodesWithJohn.getTotalElements(), "Should find 1 node containing 'john'");
        assertEquals(1, nodesWithAcme.getTotalElements(), "Should find 1 node containing 'acme'");
        assertEquals(1, nodesWithMachine.getTotalElements(), "Should find 1 node containing 'machine'");
        
        assertEquals("John Doe", nodesWithJohn.getContent().get(0).getName(), "Should find John Doe");
        assertEquals("ACME Corporation", nodesWithAcme.getContent().get(0).getName(), "Should find ACME Corporation");
        assertEquals("Machine Learning", nodesWithMachine.getContent().get(0).getName(), "Should find Machine Learning");
        
        System.out.println("Found nodes with case-insensitive search:");
        System.out.println("- 'john': " + nodesWithJohn.getContent().get(0).getName());
        System.out.println("- 'acme': " + nodesWithAcme.getContent().get(0).getName());
        System.out.println("- 'MACHINE': " + nodesWithMachine.getContent().get(0).getName());
        System.out.println("✓ NodeRepository findByNameContainingIgnoreCase works correctly");
    }

    @Test
    void findBySourceUri_ShouldReturnNodesFromSpecificSource() {
        System.out.println("Testing NodeRepository findBySourceUri method...");
        
        // When
        List<Node> nodesFromSource = nodeRepository.findBySourceUri("test://source/person/1");
        
        // Then
        assertEquals(1, nodesFromSource.size(), "Should find 1 node from specific source");
        assertEquals("John Doe", nodesFromSource.get(0).getName(), "Should find the correct node");
        assertEquals("test://source/person/1", nodesFromSource.get(0).getSourceUri(), "Source URI should match");
        
        System.out.println("Found node from source: " + nodesFromSource.get(0).getName());
        System.out.println("✓ NodeRepository findBySourceUri works correctly");
    }

    @Test
    void existsByNameAndType_ShouldReturnCorrectExistenceStatus() {
        System.out.println("Testing NodeRepository existsByNameAndType method...");
        
        // When
        boolean johnDoeExists = nodeRepository.existsByNameAndType("John Doe", NodeType.PERSON);
        boolean johnDoeAsOrgExists = nodeRepository.existsByNameAndType("John Doe", NodeType.ORGANIZATION);
        boolean nonExistentPersonExists = nodeRepository.existsByNameAndType("Jane Smith", NodeType.PERSON);
        
        // Then
        assertTrue(johnDoeExists, "John Doe as PERSON should exist");
        assertFalse(johnDoeAsOrgExists, "John Doe as ORGANIZATION should not exist");
        assertFalse(nonExistentPersonExists, "Jane Smith should not exist");
        
        System.out.println("John Doe as PERSON exists: " + johnDoeExists);
        System.out.println("John Doe as ORGANIZATION exists: " + johnDoeAsOrgExists);
        System.out.println("Jane Smith as PERSON exists: " + nonExistentPersonExists);
        System.out.println("✓ NodeRepository existsByNameAndType works correctly");
    }

    @Test
    void delete_ShouldRemoveNode() {
        System.out.println("Testing NodeRepository delete method...");
        
        // Given - verify node exists
        assertTrue(nodeRepository.existsById(conceptNode.getId()), "Concept node should exist before deletion");
        
        // When
        nodeRepository.deleteById(conceptNode.getId());
        entityManager.flush();
        
        // Then
        assertFalse(nodeRepository.existsById(conceptNode.getId()), "Concept node should not exist after deletion");
        
        List<Node> remainingNodes = nodeRepository.findAll();
        assertEquals(2, remainingNodes.size(), "Should have 2 remaining nodes after deletion");
        
        System.out.println("Nodes remaining after deletion: " + remainingNodes.size());
        System.out.println("✓ NodeRepository delete works correctly");
    }

    @Test
    void update_ShouldModifyExistingNode() {
        System.out.println("Testing NodeRepository update operation...");
        
        // Given
        Node nodeToUpdate = nodeRepository.findById(personNode.getId()).orElseThrow();
        String originalName = nodeToUpdate.getName();
        
        // When
        nodeToUpdate.setName("John Smith");
        nodeToUpdate.getProperties().put("title", "Senior Developer");
        Node updatedNode = nodeRepository.save(nodeToUpdate);
        
        // Then
        assertNotEquals(originalName, updatedNode.getName(), "Name should be updated");
        assertEquals("John Smith", updatedNode.getName(), "Name should be updated to John Smith");
        assertEquals("Senior Developer", updatedNode.getProperties().get("title"), "New property should be added");
        assertEquals(30, updatedNode.getProperties().get("age"), "Original property should be preserved");
        
        System.out.println("Updated node name from '" + originalName + "' to '" + updatedNode.getName() + "'");
        System.out.println("Added title property: " + updatedNode.getProperties().get("title"));
        System.out.println("✓ NodeRepository update works correctly");
    }

    @Test
    void count_ShouldReturnCorrectNodeCount() {
        System.out.println("Testing NodeRepository count method...");
        
        // When
        long nodeCount = nodeRepository.count();
        
        // Then
        assertEquals(3, nodeCount, "Should count 3 nodes total");
        
        System.out.println("Total node count: " + nodeCount);
        System.out.println("✓ NodeRepository count works correctly");
    }

    @Test
    void findByType_ShouldHandlePagination() {
        System.out.println("Testing NodeRepository pagination with findByType...");
        
        // Given - add more nodes of the same type
        Node extraPerson1 = new Node();
        extraPerson1.setType(NodeType.PERSON);
        extraPerson1.setName("Jane Smith");
        extraPerson1.setSourceUri("test://source/person/2");
        
        Node extraPerson2 = new Node();
        extraPerson2.setType(NodeType.PERSON);
        extraPerson2.setName("Bob Johnson");
        extraPerson2.setSourceUri("test://source/person/3");
        
        nodeRepository.save(extraPerson1);
        nodeRepository.save(extraPerson2);
        
        // When - test pagination
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);
        
        Page<Node> firstPageResults = nodeRepository.findByType(NodeType.PERSON, firstPage);
        Page<Node> secondPageResults = nodeRepository.findByType(NodeType.PERSON, secondPage);
        
        // Then
        assertEquals(3, firstPageResults.getTotalElements(), "Total should be 3 person nodes");
        assertEquals(2, firstPageResults.getTotalPages(), "Should have 2 pages with page size 2");
        assertEquals(2, firstPageResults.getContent().size(), "First page should have 2 nodes");
        assertEquals(1, secondPageResults.getContent().size(), "Second page should have 1 node");
        
        System.out.println("Total person nodes: " + firstPageResults.getTotalElements());
        System.out.println("Total pages: " + firstPageResults.getTotalPages());
        System.out.println("First page size: " + firstPageResults.getContent().size());
        System.out.println("Second page size: " + secondPageResults.getContent().size());
        System.out.println("✓ NodeRepository pagination works correctly");
    }
}