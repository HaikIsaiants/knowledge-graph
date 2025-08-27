package com.knowledgegraph.repository;

import com.knowledgegraph.TestConfiguration;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Embedding;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration.class)
class EmbeddingRepositoryTest {

    @Autowired
    private EmbeddingRepository embeddingRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Node conceptNode;
    private Node personNode;
    private Document document1;
    private Document document2;
    private Embedding nodeEmbedding;
    private Embedding documentEmbedding;
    private Embedding bothEmbedding;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up EmbeddingRepository test data...");
        
        // Create nodes
        conceptNode = new Node();
        conceptNode.setType(NodeType.CONCEPT);
        conceptNode.setName("Machine Learning");
        conceptNode.setSourceUri("test://source/concept/1");
        conceptNode = entityManager.persistAndFlush(conceptNode);
        
        personNode = new Node();
        personNode.setType(NodeType.PERSON);
        personNode.setName("Dr. AI Smith");
        personNode.setSourceUri("test://source/person/1");
        personNode = entityManager.persistAndFlush(personNode);
        
        // Create documents
        document1 = new Document();
        document1.setUri("https://example.com/ml-guide.pdf");
        document1.setContent("Machine learning algorithms and techniques...");
        document1.setContentType("application/pdf");
        document1 = entityManager.persistAndFlush(document1);
        
        document2 = new Document();
        document2.setUri("https://example.com/ai-research.pdf");
        document2.setContent("Artificial intelligence research paper...");
        document2.setContentType("application/pdf");
        document2 = entityManager.persistAndFlush(document2);
        
        // Create embeddings
        nodeEmbedding = new Embedding();
        nodeEmbedding.setNode(conceptNode);
        // No document for this embedding
        nodeEmbedding.setContentSnippet("Machine learning is a subset of AI that enables computers to learn patterns from data.");
        nodeEmbedding.setVector(new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f});
        nodeEmbedding.setModelVersion("text-embedding-ada-002");
        nodeEmbedding = entityManager.persistAndFlush(nodeEmbedding);
        
        documentEmbedding = new Embedding();
        documentEmbedding.setDocument(document1);
        // No node for this embedding
        documentEmbedding.setContentSnippet("This document explains various machine learning algorithms including supervised and unsupervised learning.");
        documentEmbedding.setVector(new float[]{0.6f, 0.7f, 0.8f, 0.9f, 1.0f});
        documentEmbedding.setModelVersion("text-embedding-ada-002");
        documentEmbedding = entityManager.persistAndFlush(documentEmbedding);
        
        bothEmbedding = new Embedding();
        bothEmbedding.setNode(personNode);
        bothEmbedding.setDocument(document2);
        bothEmbedding.setContentSnippet("Dr. AI Smith's research focuses on neural networks and deep learning architectures.");
        bothEmbedding.setVector(new float[]{0.2f, 0.4f, 0.6f, 0.8f, 1.0f});
        bothEmbedding.setModelVersion("custom-model-v1");
        bothEmbedding = entityManager.persistAndFlush(bothEmbedding);
        
        System.out.println("✓ EmbeddingRepository test data prepared with 3 embeddings");
    }

    @Test
    void findAll_ShouldReturnAllEmbeddings() {
        System.out.println("Testing EmbeddingRepository findAll method...");
        
        // When
        List<Embedding> allEmbeddings = embeddingRepository.findAll();
        
        // Then
        assertNotNull(allEmbeddings, "Embedding list should not be null");
        assertEquals(3, allEmbeddings.size(), "Should find 3 embeddings");
        
        System.out.println("Found " + allEmbeddings.size() + " embeddings total");
        System.out.println("✓ EmbeddingRepository findAll works correctly");
    }

    @Test
    void findById_ShouldReturnCorrectEmbedding() {
        System.out.println("Testing EmbeddingRepository findById method...");
        
        // When
        Optional<Embedding> foundEmbedding = embeddingRepository.findById(nodeEmbedding.getId());
        
        // Then
        assertTrue(foundEmbedding.isPresent(), "Embedding should be found");
        assertEquals(nodeEmbedding.getContentSnippet(), foundEmbedding.get().getContentSnippet(), "Content snippet should match");
        assertEquals(nodeEmbedding.getModelVersion(), foundEmbedding.get().getModelVersion(), "Model version should match");
        assertEquals(conceptNode.getId(), foundEmbedding.get().getNodeId(), "Node ID should match");
        
        System.out.println("Found embedding by ID with model: " + foundEmbedding.get().getModelVersion());
        System.out.println("✓ EmbeddingRepository findById works correctly");
    }

    @Test
    void findByNodeId_ShouldReturnEmbeddingsForSpecificNode() {
        System.out.println("Testing EmbeddingRepository findByNodeId method...");
        
        // When
        List<Embedding> conceptEmbeddings = embeddingRepository.findByNodeId(conceptNode.getId());
        List<Embedding> personEmbeddings = embeddingRepository.findByNodeId(personNode.getId());
        
        // Then
        assertEquals(1, conceptEmbeddings.size(), "Should find 1 embedding for concept node");
        assertEquals(1, personEmbeddings.size(), "Should find 1 embedding for person node");
        
        assertEquals(nodeEmbedding.getId(), conceptEmbeddings.get(0).getId(), "Should find the node-only embedding");
        assertEquals(bothEmbedding.getId(), personEmbeddings.get(0).getId(), "Should find the both embedding");
        
        System.out.println("Found " + conceptEmbeddings.size() + " embeddings for concept node");
        System.out.println("Found " + personEmbeddings.size() + " embeddings for person node");
        System.out.println("✓ EmbeddingRepository findByNodeId works correctly");
    }

    @Test
    void findByNodeId_WithPageable_ShouldSupportPagination() {
        System.out.println("Testing EmbeddingRepository findByNodeId with pagination...");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Embedding> conceptEmbeddings = embeddingRepository.findByNodeId(conceptNode.getId(), pageable);
        
        // Then
        assertEquals(1, conceptEmbeddings.getTotalElements(), "Should have 1 embedding total for concept node");
        assertEquals(1, conceptEmbeddings.getTotalPages(), "Should have 1 page");
        assertEquals(1, conceptEmbeddings.getContent().size(), "Should have 1 embedding in content");
        
        System.out.println("Concept embeddings with pagination - Total: " + conceptEmbeddings.getTotalElements());
        System.out.println("✓ EmbeddingRepository findByNodeId with pagination works correctly");
    }

    @Test
    void findByDocumentId_ShouldReturnEmbeddingsForSpecificDocument() {
        System.out.println("Testing EmbeddingRepository findByDocumentId method...");
        
        // When
        List<Embedding> doc1Embeddings = embeddingRepository.findByDocumentId(document1.getId());
        List<Embedding> doc2Embeddings = embeddingRepository.findByDocumentId(document2.getId());
        
        // Then
        assertEquals(1, doc1Embeddings.size(), "Should find 1 embedding for document 1");
        assertEquals(1, doc2Embeddings.size(), "Should find 1 embedding for document 2");
        
        assertEquals(documentEmbedding.getId(), doc1Embeddings.get(0).getId(), "Should find the document-only embedding");
        assertEquals(bothEmbedding.getId(), doc2Embeddings.get(0).getId(), "Should find the both embedding");
        
        System.out.println("Found " + doc1Embeddings.size() + " embeddings for document 1");
        System.out.println("Found " + doc2Embeddings.size() + " embeddings for document 2");
        System.out.println("✓ EmbeddingRepository findByDocumentId works correctly");
    }

    @Test
    void findByDocumentId_WithPageable_ShouldSupportPagination() {
        System.out.println("Testing EmbeddingRepository findByDocumentId with pagination...");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Embedding> doc1Embeddings = embeddingRepository.findByDocumentId(document1.getId(), pageable);
        
        // Then
        assertEquals(1, doc1Embeddings.getTotalElements(), "Should have 1 embedding total for document 1");
        assertEquals(1, doc1Embeddings.getTotalPages(), "Should have 1 page");
        assertEquals(1, doc1Embeddings.getContent().size(), "Should have 1 embedding in content");
        
        System.out.println("Document 1 embeddings with pagination - Total: " + doc1Embeddings.getTotalElements());
        System.out.println("✓ EmbeddingRepository findByDocumentId with pagination works correctly");
    }

    @Test
    void findByModelVersion_ShouldReturnEmbeddingsForSpecificModel() {
        System.out.println("Testing EmbeddingRepository findByModelVersion method...");
        
        // When
        List<Embedding> adaEmbeddings = embeddingRepository.findByModelVersion("text-embedding-ada-002");
        List<Embedding> customEmbeddings = embeddingRepository.findByModelVersion("custom-model-v1");
        List<Embedding> nonExistentEmbeddings = embeddingRepository.findByModelVersion("non-existent-model");
        
        // Then
        assertEquals(2, adaEmbeddings.size(), "Should find 2 embeddings for Ada model");
        assertEquals(1, customEmbeddings.size(), "Should find 1 embedding for custom model");
        assertEquals(0, nonExistentEmbeddings.size(), "Should find 0 embeddings for non-existent model");
        
        // Verify specific embeddings
        assertTrue(adaEmbeddings.contains(nodeEmbedding), "Ada embeddings should include node embedding");
        assertTrue(adaEmbeddings.contains(documentEmbedding), "Ada embeddings should include document embedding");
        assertTrue(customEmbeddings.contains(bothEmbedding), "Custom embeddings should include both embedding");
        
        System.out.println("Found " + adaEmbeddings.size() + " embeddings for Ada model");
        System.out.println("Found " + customEmbeddings.size() + " embeddings for custom model");
        System.out.println("✓ EmbeddingRepository findByModelVersion works correctly");
    }

    @Test
    void countByModelVersion_ShouldReturnCorrectCounts() {
        System.out.println("Testing EmbeddingRepository countByModelVersion method...");
        
        // When
        long adaCount = embeddingRepository.countByModelVersion("text-embedding-ada-002");
        long customCount = embeddingRepository.countByModelVersion("custom-model-v1");
        long nonExistentCount = embeddingRepository.countByModelVersion("non-existent-model");
        
        // Then
        assertEquals(2, adaCount, "Should count 2 Ada embeddings");
        assertEquals(1, customCount, "Should count 1 custom embedding");
        assertEquals(0, nonExistentCount, "Should count 0 non-existent model embeddings");
        
        System.out.println("Ada model embedding count: " + adaCount);
        System.out.println("Custom model embedding count: " + customCount);
        System.out.println("✓ EmbeddingRepository countByModelVersion works correctly");
    }

    @Test
    void findEmbeddingsWithoutVectors_ShouldReturnEmbeddingsWithNullVectors() {
        System.out.println("Testing EmbeddingRepository findEmbeddingsWithoutVectors method...");
        
        // Given - create an embedding without a vector
        Embedding embeddingWithoutVector = new Embedding();
        embeddingWithoutVector.setNode(conceptNode);
        embeddingWithoutVector.setContentSnippet("This embedding doesn't have a vector yet.");
        embeddingWithoutVector.setModelVersion("pending-model");
        // Don't set vector - should be null
        embeddingWithoutVector = entityManager.persistAndFlush(embeddingWithoutVector);
        
        // When
        List<Embedding> embeddingsWithoutVectors = embeddingRepository.findEmbeddingsWithoutVectors();
        
        // Then
        assertEquals(1, embeddingsWithoutVectors.size(), "Should find 1 embedding without vector");
        assertEquals(embeddingWithoutVector.getId(), embeddingsWithoutVectors.get(0).getId(), "Should find the embedding without vector");
        assertNull(embeddingsWithoutVectors.get(0).getVector(), "Vector should be null");
        
        System.out.println("Found " + embeddingsWithoutVectors.size() + " embeddings without vectors");
        System.out.println("✓ EmbeddingRepository findEmbeddingsWithoutVectors works correctly");
    }

    @Test
    void findTop20ByOrderByCreatedAtDesc_ShouldReturnRecentEmbeddings() {
        System.out.println("Testing EmbeddingRepository findTop20ByOrderByCreatedAtDesc method...");
        
        // When
        List<Embedding> recentEmbeddings = embeddingRepository.findTop20ByOrderByCreatedAtDesc();
        
        // Then
        assertNotNull(recentEmbeddings, "Recent embeddings list should not be null");
        assertTrue(recentEmbeddings.size() <= 20, "Should return at most 20 embeddings");
        assertEquals(3, recentEmbeddings.size(), "Should return all 3 embeddings since we have less than 20");
        
        // Verify order (most recent first)
        for (int i = 1; i < recentEmbeddings.size(); i++) {
            assertTrue(recentEmbeddings.get(i - 1).getCreatedAt().isAfter(recentEmbeddings.get(i).getCreatedAt()) ||
                      recentEmbeddings.get(i - 1).getCreatedAt().isEqual(recentEmbeddings.get(i).getCreatedAt()),
                      "Embeddings should be ordered by creation date descending");
        }
        
        System.out.println("Found " + recentEmbeddings.size() + " recent embeddings");
        System.out.println("Most recent embedding created at: " + recentEmbeddings.get(0).getCreatedAt());
        System.out.println("✓ EmbeddingRepository findTop20ByOrderByCreatedAtDesc works correctly");
    }

    @Test
    void save_ShouldPersistNewEmbedding() {
        System.out.println("Testing EmbeddingRepository save method for new embedding...");
        
        // Given
        Embedding newEmbedding = new Embedding();
        newEmbedding.setNode(personNode);
        newEmbedding.setDocument(document1);
        newEmbedding.setContentSnippet("New embedding content about person and document relationship.");
        newEmbedding.setVector(new float[]{0.3f, 0.6f, 0.9f, 0.12f, 0.15f});
        newEmbedding.setModelVersion("new-model-v2");
        
        // When
        Embedding savedEmbedding = embeddingRepository.save(newEmbedding);
        
        // Then
        assertNotNull(savedEmbedding.getId(), "Saved embedding should have an ID");
        assertEquals(personNode.getId(), savedEmbedding.getNodeId(), "Node ID should be preserved");
        assertEquals(document1.getId(), savedEmbedding.getDocumentId(), "Document ID should be preserved");
        assertEquals("new-model-v2", savedEmbedding.getModelVersion(), "Model version should be preserved");
        assertArrayEquals(new float[]{0.3f, 0.6f, 0.9f, 0.12f, 0.15f}, savedEmbedding.getVector(), 0.001f, "Vector should be preserved");
        
        System.out.println("Saved new embedding with ID: " + savedEmbedding.getId());
        System.out.println("✓ EmbeddingRepository save works correctly for new embedding");
    }

    @Test
    @Transactional
    void deleteByNodeId_ShouldRemoveEmbeddingsForSpecificNode() {
        System.out.println("Testing EmbeddingRepository deleteByNodeId method...");
        
        // Given - verify embeddings exist for concept node
        List<Embedding> beforeDeletion = embeddingRepository.findByNodeId(conceptNode.getId());
        assertEquals(1, beforeDeletion.size(), "Should have 1 embedding for concept node before deletion");
        
        // When
        embeddingRepository.deleteByNodeId(conceptNode.getId());
        embeddingRepository.flush();
        
        // Then
        List<Embedding> afterDeletion = embeddingRepository.findByNodeId(conceptNode.getId());
        assertEquals(0, afterDeletion.size(), "Should have 0 embeddings for concept node after deletion");
        
        // Verify other embeddings still exist
        List<Embedding> allRemainingEmbeddings = embeddingRepository.findAll();
        assertEquals(2, allRemainingEmbeddings.size(), "Should have 2 remaining embeddings");
        
        System.out.println("Embeddings for concept node after deletion: " + afterDeletion.size());
        System.out.println("Total embeddings remaining: " + allRemainingEmbeddings.size());
        System.out.println("✓ EmbeddingRepository deleteByNodeId works correctly");
    }

    @Test
    @Transactional
    void deleteByDocumentId_ShouldRemoveEmbeddingsForSpecificDocument() {
        System.out.println("Testing EmbeddingRepository deleteByDocumentId method...");
        
        // Given - verify embeddings exist for document1
        List<Embedding> beforeDeletion = embeddingRepository.findByDocumentId(document1.getId());
        assertEquals(1, beforeDeletion.size(), "Should have 1 embedding for document1 before deletion");
        
        // When
        embeddingRepository.deleteByDocumentId(document1.getId());
        embeddingRepository.flush();
        
        // Then
        List<Embedding> afterDeletion = embeddingRepository.findByDocumentId(document1.getId());
        assertEquals(0, afterDeletion.size(), "Should have 0 embeddings for document1 after deletion");
        
        // Verify other embeddings still exist
        List<Embedding> allRemainingEmbeddings = embeddingRepository.findAll();
        assertEquals(2, allRemainingEmbeddings.size(), "Should have 2 remaining embeddings");
        
        System.out.println("Embeddings for document1 after deletion: " + afterDeletion.size());
        System.out.println("Total embeddings remaining: " + allRemainingEmbeddings.size());
        System.out.println("✓ EmbeddingRepository deleteByDocumentId works correctly");
    }

    @Test
    void delete_ShouldRemoveSpecificEmbedding() {
        System.out.println("Testing EmbeddingRepository delete method...");
        
        // Given - verify embedding exists
        assertTrue(embeddingRepository.existsById(bothEmbedding.getId()), "Both embedding should exist before deletion");
        
        // When
        embeddingRepository.deleteById(bothEmbedding.getId());
        entityManager.flush();
        
        // Then
        assertFalse(embeddingRepository.existsById(bothEmbedding.getId()), "Both embedding should not exist after deletion");
        
        List<Embedding> remainingEmbeddings = embeddingRepository.findAll();
        assertEquals(2, remainingEmbeddings.size(), "Should have 2 remaining embeddings after deletion");
        
        System.out.println("Embeddings remaining after deletion: " + remainingEmbeddings.size());
        System.out.println("✓ EmbeddingRepository delete works correctly");
    }

    @Test
    void count_ShouldReturnCorrectEmbeddingCount() {
        System.out.println("Testing EmbeddingRepository count method...");
        
        // When
        long embeddingCount = embeddingRepository.count();
        
        // Then
        assertEquals(3, embeddingCount, "Should count 3 embeddings total");
        
        System.out.println("Total embedding count: " + embeddingCount);
        System.out.println("✓ EmbeddingRepository count works correctly");
    }

    @Test
    void update_ShouldModifyExistingEmbedding() {
        System.out.println("Testing EmbeddingRepository update operation...");
        
        // Given
        Embedding embeddingToUpdate = embeddingRepository.findById(nodeEmbedding.getId()).orElseThrow();
        String originalSnippet = embeddingToUpdate.getContentSnippet();
        float[] originalVector = embeddingToUpdate.getVector().clone();
        
        // When
        embeddingToUpdate.setContentSnippet(originalSnippet + " Updated with additional context about neural networks.");
        embeddingToUpdate.setVector(new float[]{0.2f, 0.4f, 0.6f, 0.8f, 1.0f});
        embeddingToUpdate.setModelVersion("text-embedding-ada-002-updated");
        Embedding updatedEmbedding = embeddingRepository.save(embeddingToUpdate);
        
        // Then
        assertNotEquals(originalSnippet, updatedEmbedding.getContentSnippet(), "Content snippet should be updated");
        assertTrue(updatedEmbedding.getContentSnippet().contains("neural networks"), "Content snippet should contain new information");
        assertFalse(java.util.Arrays.equals(originalVector, updatedEmbedding.getVector()), "Vector should be updated");
        assertEquals("text-embedding-ada-002-updated", updatedEmbedding.getModelVersion(), "Model version should be updated");
        
        System.out.println("Updated content snippet length from " + originalSnippet.length() + 
                          " to " + updatedEmbedding.getContentSnippet().length());
        System.out.println("Updated model version to: " + updatedEmbedding.getModelVersion());
        System.out.println("✓ EmbeddingRepository update works correctly");
    }
}