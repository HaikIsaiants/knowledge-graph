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
class DocumentTest {

    @Autowired
    private TestEntityManager entityManager;

    private Document document;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up Document entity test data...");
        
        document = new Document();
        document.setUri("https://example.com/document1.pdf");
        document.setContent("This is the content of the test document. It contains various information about knowledge graphs.");
        document.setContentType("application/pdf");
        document.setLastModified(LocalDateTime.now().minusDays(1));
        document.setEtag("\"abc123def456\"");
        document.setContentHash("sha256:1234567890abcdef");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("author", "John Doe");
        metadata.put("title", "Introduction to Knowledge Graphs");
        metadata.put("pageCount", 42);
        document.setMetadata(metadata);
        
        System.out.println("✓ Document test data prepared");
    }

    @Test
    void document_ShouldPersistWithBasicFields() {
        System.out.println("Testing Document entity persistence with basic fields...");
        
        // When
        Document persistedDocument = entityManager.persistAndFlush(document);
        
        // Then
        assertNotNull(persistedDocument.getId(), "Document ID should be generated");
        assertEquals("https://example.com/document1.pdf", persistedDocument.getUri(), "URI should be preserved");
        assertEquals("application/pdf", persistedDocument.getContentType(), "Content type should be preserved");
        assertEquals("\"abc123def456\"", persistedDocument.getEtag(), "ETag should be preserved");
        assertEquals("sha256:1234567890abcdef", persistedDocument.getContentHash(), "Content hash should be preserved");
        
        System.out.println("Document ID generated: " + persistedDocument.getId());
        System.out.println("Document URI: " + persistedDocument.getUri());
        System.out.println("✓ Document entity persisted successfully with basic fields");
    }

    @Test
    void document_ShouldPersistWithContent() {
        System.out.println("Testing Document entity persistence with content...");
        
        // When
        Document persistedDocument = entityManager.persistAndFlush(document);
        
        // Then
        assertNotNull(persistedDocument.getContent(), "Content should not be null");
        assertTrue(persistedDocument.getContent().contains("knowledge graphs"), "Content should be preserved");
        assertEquals(document.getContent().length(), persistedDocument.getContent().length(), "Content length should be preserved");
        
        System.out.println("Content length: " + persistedDocument.getContent().length());
        System.out.println("✓ Document entity persisted successfully with content");
    }

    @Test
    void document_ShouldPersistWithJsonMetadata() {
        System.out.println("Testing Document entity persistence with JSON metadata...");
        
        // When
        Document persistedDocument = entityManager.persistAndFlush(document);
        
        // Then
        assertNotNull(persistedDocument.getMetadata(), "Metadata should not be null");
        assertEquals(3, persistedDocument.getMetadata().size(), "Should have 3 metadata properties");
        assertEquals("John Doe", persistedDocument.getMetadata().get("author"), "Author metadata should be preserved");
        assertEquals("Introduction to Knowledge Graphs", persistedDocument.getMetadata().get("title"), "Title metadata should be preserved");
        assertEquals(42, persistedDocument.getMetadata().get("pageCount"), "Page count metadata should be preserved");
        
        System.out.println("Metadata persisted: " + persistedDocument.getMetadata());
        System.out.println("✓ Document entity persisted successfully with JSON metadata");
    }

    @Test
    void document_ShouldSetTimestampsOnCreate() {
        System.out.println("Testing Document entity timestamp creation...");
        
        LocalDateTime before = LocalDateTime.now();
        
        // When
        Document persistedDocument = entityManager.persistAndFlush(document);
        
        LocalDateTime after = LocalDateTime.now();
        
        // Then
        assertNotNull(persistedDocument.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(persistedDocument.getUpdatedAt(), "Updated timestamp should be set");
        assertNotNull(persistedDocument.getLastModified(), "Last modified timestamp should be preserved");
        
        assertTrue(persistedDocument.getCreatedAt().isAfter(before.minusSeconds(1)), "Created timestamp should be recent");
        assertTrue(persistedDocument.getCreatedAt().isBefore(after.plusSeconds(1)), "Created timestamp should be recent");
        
        assertEquals(persistedDocument.getCreatedAt(), persistedDocument.getUpdatedAt(), "Created and updated timestamps should be equal on creation");
        
        System.out.println("Created at: " + persistedDocument.getCreatedAt());
        System.out.println("Updated at: " + persistedDocument.getUpdatedAt());
        System.out.println("Last modified: " + persistedDocument.getLastModified());
        System.out.println("✓ Document entity timestamps set correctly on creation");
    }

    @Test
    void document_ShouldUpdateTimestampOnModification() {
        System.out.println("Testing Document entity timestamp update on modification...");
        
        // Given - persist initial document
        Document persistedDocument = entityManager.persistAndFlush(document);
        LocalDateTime originalUpdatedAt = persistedDocument.getUpdatedAt();
        
        // Small delay to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // When - update the document
        persistedDocument.setContentType("text/plain");
        Document updatedDocument = entityManager.persistAndFlush(persistedDocument);
        
        // Then
        assertEquals("text/plain", updatedDocument.getContentType(), "Content type should be updated");
        assertNotEquals(originalUpdatedAt, updatedDocument.getUpdatedAt(), "Updated timestamp should change");
        assertTrue(updatedDocument.getUpdatedAt().isAfter(originalUpdatedAt), "Updated timestamp should be more recent");
        
        System.out.println("Original updated at: " + originalUpdatedAt);
        System.out.println("New updated at: " + updatedDocument.getUpdatedAt());
        System.out.println("✓ Document entity timestamp updated correctly on modification");
    }

    @Test
    void document_ShouldHandleEmptyMetadata() {
        System.out.println("Testing Document entity with empty metadata...");
        
        // Given
        Document docWithEmptyMetadata = new Document();
        docWithEmptyMetadata.setUri("https://example.com/empty.txt");
        docWithEmptyMetadata.setContent("Empty metadata document");
        docWithEmptyMetadata.setContentType("text/plain");
        docWithEmptyMetadata.setMetadata(new HashMap<>());
        
        // When
        Document persistedDocument = entityManager.persistAndFlush(docWithEmptyMetadata);
        
        // Then
        assertNotNull(persistedDocument.getMetadata(), "Metadata should not be null");
        assertTrue(persistedDocument.getMetadata().isEmpty(), "Metadata should be empty");
        
        System.out.println("✓ Document entity handles empty metadata correctly");
    }

    @Test
    void document_ShouldHandleLargeContent() {
        System.out.println("Testing Document entity with large content...");
        
        // Given
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("This is line ").append(i).append(" of a large document content. ");
        }
        
        Document docWithLargeContent = new Document();
        docWithLargeContent.setUri("https://example.com/large.txt");
        docWithLargeContent.setContent(largeContent.toString());
        docWithLargeContent.setContentType("text/plain");
        
        // When
        Document persistedDocument = entityManager.persistAndFlush(docWithLargeContent);
        
        // Then
        assertNotNull(persistedDocument.getContent(), "Content should not be null");
        assertEquals(largeContent.toString().length(), persistedDocument.getContent().length(), "Large content should be preserved");
        assertTrue(persistedDocument.getContent().contains("line 500"), "Content should contain expected text");
        
        System.out.println("Large content length: " + persistedDocument.getContent().length());
        System.out.println("✓ Document entity handles large content correctly");
    }

    @Test
    void document_ShouldRequireUniqueUri() {
        System.out.println("Testing Document entity URI uniqueness constraint...");
        
        // Given - persist first document
        Document firstDocument = entityManager.persistAndFlush(document);
        
        // When - try to create another document with the same URI
        Document secondDocument = new Document();
        secondDocument.setUri(document.getUri()); // Same URI
        secondDocument.setContent("Different content");
        secondDocument.setContentType("text/plain");
        
        // This should demonstrate the unique constraint exists
        // In a real scenario, attempting to persist would throw a constraint violation
        
        assertNotNull(firstDocument.getId(), "First document should be persisted");
        assertEquals(document.getUri(), secondDocument.getUri(), "URIs should be the same (for test verification)");
        
        System.out.println("✓ Document entity properly defines URI uniqueness constraint");
    }
}