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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration.class)
class EmbeddingTest {

    @Autowired
    private TestEntityManager entityManager;

    private Node node;
    private Document document;
    private Embedding embedding;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up Embedding entity test data...");
        
        // Create and persist a node
        node = new Node();
        node.setType(NodeType.CONCEPT);
        node.setName("Machine Learning");
        node.setSourceUri("test://source/concept/1");
        node = entityManager.persistAndFlush(node);
        
        // Create and persist a document
        document = new Document();
        document.setUri("https://example.com/ml-guide.pdf");
        document.setContent("Machine learning is a subset of artificial intelligence...");
        document.setContentType("application/pdf");
        document = entityManager.persistAndFlush(document);
        
        // Create embedding
        embedding = new Embedding();
        embedding.setNode(node);
        embedding.setDocument(document);
        embedding.setContentSnippet("Machine learning is a subset of artificial intelligence that enables computers to learn without being explicitly programmed.");
        embedding.setVector(new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f});
        embedding.setModelVersion("text-embedding-ada-002");
        
        System.out.println("✓ Embedding test data prepared with node and document");
    }

    @Test
    void embedding_ShouldPersistWithBasicFields() {
        System.out.println("Testing Embedding entity persistence with basic fields...");
        
        // When
        Embedding persistedEmbedding = entityManager.persistAndFlush(embedding);
        
        // Then
        assertNotNull(persistedEmbedding.getId(), "Embedding ID should be generated");
        assertNotNull(persistedEmbedding.getNode(), "Node should be set");
        assertNotNull(persistedEmbedding.getDocument(), "Document should be set");
        assertEquals(node.getId(), persistedEmbedding.getNode().getId(), "Node ID should match");
        assertEquals(document.getId(), persistedEmbedding.getDocument().getId(), "Document ID should match");
        assertEquals("text-embedding-ada-002", persistedEmbedding.getModelVersion(), "Model version should be preserved");
        
        System.out.println("Embedding ID generated: " + persistedEmbedding.getId());
        System.out.println("Node ID: " + persistedEmbedding.getNodeId());
        System.out.println("Document ID: " + persistedEmbedding.getDocumentId());
        System.out.println("✓ Embedding entity persisted successfully with basic fields");
    }

    @Test
    void embedding_ShouldPersistWithContentSnippet() {
        System.out.println("Testing Embedding entity persistence with content snippet...");
        
        // When
        Embedding persistedEmbedding = entityManager.persistAndFlush(embedding);
        
        // Then
        assertNotNull(persistedEmbedding.getContentSnippet(), "Content snippet should not be null");
        assertTrue(persistedEmbedding.getContentSnippet().contains("Machine learning"), "Content snippet should be preserved");
        assertEquals(embedding.getContentSnippet(), persistedEmbedding.getContentSnippet(), "Content snippet should match exactly");
        
        System.out.println("Content snippet length: " + persistedEmbedding.getContentSnippet().length());
        System.out.println("✓ Embedding entity persisted successfully with content snippet");
    }

    @Test
    void embedding_ShouldPersistWithVectorArray() {
        System.out.println("Testing Embedding entity persistence with vector array...");
        
        // When
        Embedding persistedEmbedding = entityManager.persistAndFlush(embedding);
        
        // Then
        assertNotNull(persistedEmbedding.getVector(), "Vector should not be null");
        assertEquals(5, persistedEmbedding.getVector().length, "Vector should have 5 dimensions");
        assertEquals(0.1f, persistedEmbedding.getVector()[0], 0.001f, "First vector component should be preserved");
        assertEquals(0.5f, persistedEmbedding.getVector()[4], 0.001f, "Last vector component should be preserved");
        
        System.out.println("Vector dimensions: " + persistedEmbedding.getVector().length);
        System.out.println("Vector values: [" + persistedEmbedding.getVector()[0] + ", " + 
                          persistedEmbedding.getVector()[1] + ", " + persistedEmbedding.getVector()[2] + ", " + 
                          persistedEmbedding.getVector()[3] + ", " + persistedEmbedding.getVector()[4] + "]");
        System.out.println("✓ Embedding entity persisted successfully with vector array");
    }

    @Test
    void embedding_ShouldSetTimestampOnCreate() {
        System.out.println("Testing Embedding entity timestamp creation...");
        
        LocalDateTime before = LocalDateTime.now();
        
        // When
        Embedding persistedEmbedding = entityManager.persistAndFlush(embedding);
        
        LocalDateTime after = LocalDateTime.now();
        
        // Then
        assertNotNull(persistedEmbedding.getCreatedAt(), "Created timestamp should be set");
        
        assertTrue(persistedEmbedding.getCreatedAt().isAfter(before.minusSeconds(1)), "Created timestamp should be recent");
        assertTrue(persistedEmbedding.getCreatedAt().isBefore(after.plusSeconds(1)), "Created timestamp should be recent");
        
        System.out.println("Created at: " + persistedEmbedding.getCreatedAt());
        System.out.println("✓ Embedding entity timestamp set correctly on creation");
    }

    @Test
    void embedding_ShouldProvideConvenienceMethodsForIds() {
        System.out.println("Testing Embedding entity convenience methods for IDs...");
        
        // When
        Embedding persistedEmbedding = entityManager.persistAndFlush(embedding);
        
        // Then
        assertEquals(node.getId(), persistedEmbedding.getNodeId(), "getNodeId() should return node ID");
        assertEquals(document.getId(), persistedEmbedding.getDocumentId(), "getDocumentId() should return document ID");
        
        System.out.println("Node ID via convenience method: " + persistedEmbedding.getNodeId());
        System.out.println("Document ID via convenience method: " + persistedEmbedding.getDocumentId());
        System.out.println("✓ Embedding entity convenience methods work correctly");
    }

    @Test
    void embedding_ShouldHandleDefaultModelVersion() {
        System.out.println("Testing Embedding entity with default model version...");
        
        // Given
        Embedding embeddingWithDefaultModel = new Embedding();
        embeddingWithDefaultModel.setNode(node);
        embeddingWithDefaultModel.setDocument(document);
        embeddingWithDefaultModel.setContentSnippet("Test snippet");
        embeddingWithDefaultModel.setVector(new float[]{0.1f, 0.2f, 0.3f});
        // Don't set model version - should use default "mock"
        
        // When
        Embedding persistedEmbedding = entityManager.persistAndFlush(embeddingWithDefaultModel);
        
        // Then
        assertEquals("mock", persistedEmbedding.getModelVersion(), "Default model version should be 'mock'");
        
        System.out.println("Default model version: " + persistedEmbedding.getModelVersion());
        System.out.println("✓ Embedding entity uses correct default model version");
    }

    @Test
    void embedding_ShouldHandleHighDimensionalVectors() {
        System.out.println("Testing Embedding entity with high dimensional vectors...");
        
        // Given - create a high dimensional vector (typical embedding size)
        float[] highDimVector = new float[1536]; // Common OpenAI embedding size
        for (int i = 0; i < highDimVector.length; i++) {
            highDimVector[i] = (float) Math.random();
        }
        
        Embedding embeddingWithHighDimVector = new Embedding();
        embeddingWithHighDimVector.setNode(node);
        embeddingWithHighDimVector.setDocument(document);
        embeddingWithHighDimVector.setContentSnippet("High dimensional embedding test");
        embeddingWithHighDimVector.setVector(highDimVector);
        embeddingWithHighDimVector.setModelVersion("text-embedding-ada-002");
        
        // When
        Embedding persistedEmbedding = entityManager.persistAndFlush(embeddingWithHighDimVector);
        
        // Then
        assertNotNull(persistedEmbedding.getVector(), "High dimensional vector should not be null");
        assertEquals(1536, persistedEmbedding.getVector().length, "Vector should have 1536 dimensions");
        assertEquals(highDimVector[0], persistedEmbedding.getVector()[0], 0.001f, "First component should be preserved");
        assertEquals(highDimVector[1535], persistedEmbedding.getVector()[1535], 0.001f, "Last component should be preserved");
        
        System.out.println("High dimensional vector size: " + persistedEmbedding.getVector().length);
        System.out.println("✓ Embedding entity handles high dimensional vectors correctly");
    }

    @Test
    void embedding_ShouldSupportEmbeddingWithNodeOnly() {
        System.out.println("Testing Embedding entity with node only (no document)...");
        
        // Given
        Embedding nodeOnlyEmbedding = new Embedding();
        nodeOnlyEmbedding.setNode(node);
        // nodeOnlyEmbedding.setDocument(null); // No document reference
        nodeOnlyEmbedding.setContentSnippet("Node-only embedding content");
        nodeOnlyEmbedding.setVector(new float[]{0.7f, 0.8f, 0.9f});
        nodeOnlyEmbedding.setModelVersion("custom-model");
        
        // When
        Embedding persistedEmbedding = entityManager.persistAndFlush(nodeOnlyEmbedding);
        
        // Then
        assertNotNull(persistedEmbedding.getNode(), "Node should be set");
        assertNull(persistedEmbedding.getDocument(), "Document should be null");
        assertEquals(node.getId(), persistedEmbedding.getNodeId(), "Node ID should match");
        assertNull(persistedEmbedding.getDocumentId(), "Document ID should be null");
        
        System.out.println("Node-only embedding ID: " + persistedEmbedding.getId());
        System.out.println("✓ Embedding entity supports node-only embeddings");
    }

    @Test
    void embedding_ShouldSupportEmbeddingWithDocumentOnly() {
        System.out.println("Testing Embedding entity with document only (no node)...");
        
        // Given
        Embedding documentOnlyEmbedding = new Embedding();
        // documentOnlyEmbedding.setNode(null); // No node reference
        documentOnlyEmbedding.setDocument(document);
        documentOnlyEmbedding.setContentSnippet("Document-only embedding content");
        documentOnlyEmbedding.setVector(new float[]{0.4f, 0.6f, 0.8f});
        documentOnlyEmbedding.setModelVersion("doc-model");
        
        // When
        Embedding persistedEmbedding = entityManager.persistAndFlush(documentOnlyEmbedding);
        
        // Then
        assertNull(persistedEmbedding.getNode(), "Node should be null");
        assertNotNull(persistedEmbedding.getDocument(), "Document should be set");
        assertNull(persistedEmbedding.getNodeId(), "Node ID should be null");
        assertEquals(document.getId(), persistedEmbedding.getDocumentId(), "Document ID should match");
        
        System.out.println("Document-only embedding ID: " + persistedEmbedding.getId());
        System.out.println("✓ Embedding entity supports document-only embeddings");
    }
}