package com.knowledgegraph.repository;

import com.knowledgegraph.TestConfiguration;
import com.knowledgegraph.model.Document;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration.class)
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Document pdfDocument;
    private Document htmlDocument;
    private Document textDocument;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up DocumentRepository test data...");
        
        // Create PDF document
        pdfDocument = new Document();
        pdfDocument.setUri("https://example.com/document.pdf");
        pdfDocument.setContent("This is a PDF document about machine learning and artificial intelligence. It covers various algorithms and techniques.");
        pdfDocument.setContentType("application/pdf");
        pdfDocument.setLastModified(LocalDateTime.now().minusDays(5));
        pdfDocument.setEtag("\"pdf-etag-123\"");
        pdfDocument.setContentHash("sha256:pdf123hash");
        
        Map<String, Object> pdfMetadata = new HashMap<>();
        pdfMetadata.put("author", "Dr. AI Smith");
        pdfMetadata.put("title", "ML Guide");
        pdfMetadata.put("pages", 50);
        pdfDocument.setMetadata(pdfMetadata);
        
        // Create HTML document
        htmlDocument = new Document();
        htmlDocument.setUri("https://example.com/webpage.html");
        htmlDocument.setContent("HTML content about knowledge graphs and their applications in modern AI systems.");
        htmlDocument.setContentType("text/html");
        htmlDocument.setLastModified(LocalDateTime.now().minusDays(2));
        htmlDocument.setEtag("\"html-etag-456\"");
        htmlDocument.setContentHash("sha256:html456hash");
        
        Map<String, Object> htmlMetadata = new HashMap<>();
        htmlMetadata.put("title", "Knowledge Graphs Overview");
        htmlMetadata.put("wordCount", 1200);
        htmlDocument.setMetadata(htmlMetadata);
        
        // Create text document
        textDocument = new Document();
        textDocument.setUri("https://example.com/notes.txt");
        textDocument.setContent("Simple text notes about neural networks and deep learning concepts.");
        textDocument.setContentType("text/plain");
        textDocument.setLastModified(LocalDateTime.now().minusHours(6));
        textDocument.setEtag("\"text-etag-789\"");
        textDocument.setContentHash("sha256:text789hash");
        
        // Persist test data
        pdfDocument = entityManager.persistAndFlush(pdfDocument);
        htmlDocument = entityManager.persistAndFlush(htmlDocument);
        textDocument = entityManager.persistAndFlush(textDocument);
        
        System.out.println("✓ DocumentRepository test data prepared with 3 documents");
    }

    @Test
    void findAll_ShouldReturnAllDocuments() {
        System.out.println("Testing DocumentRepository findAll method...");
        
        // When
        List<Document> allDocuments = documentRepository.findAll();
        
        // Then
        assertNotNull(allDocuments, "Document list should not be null");
        assertEquals(3, allDocuments.size(), "Should find 3 documents");
        
        System.out.println("Found " + allDocuments.size() + " documents total");
        System.out.println("✓ DocumentRepository findAll works correctly");
    }

    @Test
    void findByUri_ShouldReturnCorrectDocument() {
        System.out.println("Testing DocumentRepository findByUri method...");
        
        // When
        Optional<Document> foundDocument = documentRepository.findByUri("https://example.com/document.pdf");
        Optional<Document> notFoundDocument = documentRepository.findByUri("https://example.com/nonexistent.pdf");
        
        // Then
        assertTrue(foundDocument.isPresent(), "PDF document should be found");
        assertEquals("application/pdf", foundDocument.get().getContentType(), "Content type should match");
        assertEquals(pdfDocument.getId(), foundDocument.get().getId(), "Document ID should match");
        
        assertFalse(notFoundDocument.isPresent(), "Non-existent document should not be found");
        
        System.out.println("Found document by URI: " + foundDocument.get().getUri());
        System.out.println("✓ DocumentRepository findByUri works correctly");
    }

    @Test
    void findByContentType_ShouldReturnDocumentsOfSpecificType() {
        System.out.println("Testing DocumentRepository findByContentType method...");
        
        // When
        List<Document> pdfDocuments = documentRepository.findByContentType("application/pdf");
        List<Document> htmlDocuments = documentRepository.findByContentType("text/html");
        List<Document> textDocuments = documentRepository.findByContentType("text/plain");
        List<Document> unknownDocuments = documentRepository.findByContentType("application/unknown");
        
        // Then
        assertEquals(1, pdfDocuments.size(), "Should find 1 PDF document");
        assertEquals(1, htmlDocuments.size(), "Should find 1 HTML document");
        assertEquals(1, textDocuments.size(), "Should find 1 text document");
        assertEquals(0, unknownDocuments.size(), "Should find 0 unknown type documents");
        
        assertEquals(pdfDocument.getId(), pdfDocuments.get(0).getId(), "PDF document should match");
        assertEquals(htmlDocument.getId(), htmlDocuments.get(0).getId(), "HTML document should match");
        assertEquals(textDocument.getId(), textDocuments.get(0).getId(), "Text document should match");
        
        System.out.println("Found " + pdfDocuments.size() + " PDF documents");
        System.out.println("Found " + htmlDocuments.size() + " HTML documents");
        System.out.println("Found " + textDocuments.size() + " text documents");
        System.out.println("✓ DocumentRepository findByContentType works correctly");
    }

    @Test
    void findByContentType_WithPageable_ShouldSupportPagination() {
        System.out.println("Testing DocumentRepository findByContentType with pagination...");
        
        // When
        Pageable pageable = PageRequest.of(0, 10);
        Page<Document> pdfDocuments = documentRepository.findByContentType("application/pdf", pageable);
        
        // Then
        assertEquals(1, pdfDocuments.getTotalElements(), "Should have 1 PDF document total");
        assertEquals(1, pdfDocuments.getTotalPages(), "Should have 1 page");
        assertEquals(1, pdfDocuments.getContent().size(), "Should have 1 document in content");
        
        System.out.println("PDF documents with pagination - Total: " + pdfDocuments.getTotalElements());
        System.out.println("✓ DocumentRepository findByContentType with pagination works correctly");
    }

    @Test
    void findByLastModifiedAfter_ShouldReturnRecentDocuments() {
        System.out.println("Testing DocumentRepository findByLastModifiedAfter method...");
        
        // When
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        
        List<Document> documentsAfterThreeDays = documentRepository.findByLastModifiedAfter(threeDaysAgo);
        List<Document> documentsAfterOneDay = documentRepository.findByLastModifiedAfter(oneDayAgo);
        
        // Then
        assertEquals(2, documentsAfterThreeDays.size(), "Should find 2 documents modified after 3 days ago");
        assertEquals(1, documentsAfterOneDay.size(), "Should find 1 document modified after 1 day ago");
        
        // The most recently modified should be the text document
        assertTrue(documentsAfterOneDay.contains(textDocument), "Text document should be in recent results");
        assertTrue(documentsAfterThreeDays.contains(htmlDocument), "HTML document should be in 3-day results");
        assertTrue(documentsAfterThreeDays.contains(textDocument), "Text document should be in 3-day results");
        
        System.out.println("Documents modified after 3 days ago: " + documentsAfterThreeDays.size());
        System.out.println("Documents modified after 1 day ago: " + documentsAfterOneDay.size());
        System.out.println("✓ DocumentRepository findByLastModifiedAfter works correctly");
    }

    @Test
    void findByEtag_ShouldReturnCorrectDocument() {
        System.out.println("Testing DocumentRepository findByEtag method...");
        
        // When
        Optional<Document> foundByEtag = documentRepository.findByEtag("\"pdf-etag-123\"");
        Optional<Document> notFoundByEtag = documentRepository.findByEtag("\"nonexistent-etag\"");
        
        // Then
        assertTrue(foundByEtag.isPresent(), "Document should be found by ETag");
        assertEquals(pdfDocument.getId(), foundByEtag.get().getId(), "Should find the PDF document");
        assertEquals("\"pdf-etag-123\"", foundByEtag.get().getEtag(), "ETag should match");
        
        assertFalse(notFoundByEtag.isPresent(), "Non-existent ETag should not return document");
        
        System.out.println("Found document by ETag: " + foundByEtag.get().getUri());
        System.out.println("✓ DocumentRepository findByEtag works correctly");
    }

    @Test
    void findByContentHash_ShouldReturnDocumentsWithSameHash() {
        System.out.println("Testing DocumentRepository findByContentHash method...");
        
        // When
        List<Document> documentsWithPdfHash = documentRepository.findByContentHash("sha256:pdf123hash");
        List<Document> documentsWithNonExistentHash = documentRepository.findByContentHash("sha256:nonexistent");
        
        // Then
        assertEquals(1, documentsWithPdfHash.size(), "Should find 1 document with PDF hash");
        assertEquals(0, documentsWithNonExistentHash.size(), "Should find 0 documents with non-existent hash");
        
        assertEquals(pdfDocument.getId(), documentsWithPdfHash.get(0).getId(), "Should find the PDF document");
        assertEquals("sha256:pdf123hash", documentsWithPdfHash.get(0).getContentHash(), "Content hash should match");
        
        System.out.println("Found document by content hash: " + documentsWithPdfHash.get(0).getUri());
        System.out.println("✓ DocumentRepository findByContentHash works correctly");
    }

    @Test
    void existsByUri_ShouldReturnCorrectExistenceStatus() {
        System.out.println("Testing DocumentRepository existsByUri method...");
        
        // When
        boolean pdfExists = documentRepository.existsByUri("https://example.com/document.pdf");
        boolean htmlExists = documentRepository.existsByUri("https://example.com/webpage.html");
        boolean nonExistentExists = documentRepository.existsByUri("https://example.com/nonexistent.pdf");
        
        // Then
        assertTrue(pdfExists, "PDF document should exist");
        assertTrue(htmlExists, "HTML document should exist");
        assertFalse(nonExistentExists, "Non-existent document should not exist");
        
        System.out.println("PDF document exists: " + pdfExists);
        System.out.println("HTML document exists: " + htmlExists);
        System.out.println("Non-existent document exists: " + nonExistentExists);
        System.out.println("✓ DocumentRepository existsByUri works correctly");
    }

    @Test
    void save_ShouldPersistNewDocument() {
        System.out.println("Testing DocumentRepository save method for new document...");
        
        // Given
        Document newDocument = new Document();
        newDocument.setUri("https://example.com/new-document.docx");
        newDocument.setContent("New document content about graph databases and their performance characteristics.");
        newDocument.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        newDocument.setLastModified(LocalDateTime.now());
        newDocument.setEtag("\"new-etag-999\"");
        newDocument.setContentHash("sha256:new999hash");
        
        Map<String, Object> newMetadata = new HashMap<>();
        newMetadata.put("author", "Test Author");
        newMetadata.put("version", "1.0");
        newDocument.setMetadata(newMetadata);
        
        // When
        Document savedDocument = documentRepository.save(newDocument);
        
        // Then
        assertNotNull(savedDocument.getId(), "Saved document should have an ID");
        assertEquals("https://example.com/new-document.docx", savedDocument.getUri(), "URI should be preserved");
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
                     savedDocument.getContentType(), "Content type should be preserved");
        assertEquals("Test Author", savedDocument.getMetadata().get("author"), "Metadata should be preserved");
        
        // Verify persistence
        Optional<Document> foundDocument = documentRepository.findById(savedDocument.getId());
        assertTrue(foundDocument.isPresent(), "Saved document should be findable");
        
        System.out.println("Saved new document with ID: " + savedDocument.getId());
        System.out.println("✓ DocumentRepository save works correctly for new document");
    }

    @Test
    void delete_ShouldRemoveDocument() {
        System.out.println("Testing DocumentRepository delete method...");
        
        // Given - verify document exists
        assertTrue(documentRepository.existsById(htmlDocument.getId()), "HTML document should exist before deletion");
        
        // When
        documentRepository.deleteById(htmlDocument.getId());
        entityManager.flush();
        
        // Then
        assertFalse(documentRepository.existsById(htmlDocument.getId()), "HTML document should not exist after deletion");
        
        List<Document> remainingDocuments = documentRepository.findAll();
        assertEquals(2, remainingDocuments.size(), "Should have 2 remaining documents after deletion");
        
        System.out.println("Documents remaining after deletion: " + remainingDocuments.size());
        System.out.println("✓ DocumentRepository delete works correctly");
    }

    @Test
    void update_ShouldModifyExistingDocument() {
        System.out.println("Testing DocumentRepository update operation...");
        
        // Given
        Document documentToUpdate = documentRepository.findById(textDocument.getId()).orElseThrow();
        String originalContent = documentToUpdate.getContent();
        
        // When
        documentToUpdate.setContent(originalContent + " Updated with new information about transformers.");
        documentToUpdate.setLastModified(LocalDateTime.now());
        documentToUpdate.getMetadata().put("lastUpdatedBy", "Test System");
        Document updatedDocument = documentRepository.save(documentToUpdate);
        
        // Then
        assertNotEquals(originalContent, updatedDocument.getContent(), "Content should be updated");
        assertTrue(updatedDocument.getContent().contains("transformers"), "Content should contain new information");
        assertEquals("Test System", updatedDocument.getMetadata().get("lastUpdatedBy"), "New metadata should be added");
        
        System.out.println("Updated document content length from " + originalContent.length() + 
                          " to " + updatedDocument.getContent().length());
        System.out.println("Added metadata: " + updatedDocument.getMetadata().get("lastUpdatedBy"));
        System.out.println("✓ DocumentRepository update works correctly");
    }

    @Test
    void count_ShouldReturnCorrectDocumentCount() {
        System.out.println("Testing DocumentRepository count method...");
        
        // When
        long documentCount = documentRepository.count();
        
        // Then
        assertEquals(3, documentCount, "Should count 3 documents total");
        
        System.out.println("Total document count: " + documentCount);
        System.out.println("✓ DocumentRepository count works correctly");
    }

    @Test
    void findByContentType_ShouldHandleNonExistentType() {
        System.out.println("Testing DocumentRepository findByContentType with non-existent type...");
        
        // When
        List<Document> videoDocuments = documentRepository.findByContentType("video/mp4");
        
        // Then
        assertTrue(videoDocuments.isEmpty(), "Should return empty list for non-existent content type");
        
        System.out.println("Found " + videoDocuments.size() + " video documents (expected 0)");
        System.out.println("✓ DocumentRepository handles non-existent content types correctly");
    }

    @Test
    void findByUri_ShouldHandleCaseSensitivity() {
        System.out.println("Testing DocumentRepository findByUri case sensitivity...");
        
        // When
        Optional<Document> exactMatchDocument = documentRepository.findByUri("https://example.com/document.pdf");
        Optional<Document> upperCaseDocument = documentRepository.findByUri("HTTPS://EXAMPLE.COM/DOCUMENT.PDF");
        
        // Then
        assertTrue(exactMatchDocument.isPresent(), "Exact case match should find document");
        assertFalse(upperCaseDocument.isPresent(), "Different case should not find document (case sensitive)");
        
        System.out.println("Exact case match found: " + exactMatchDocument.isPresent());
        System.out.println("Upper case match found: " + upperCaseDocument.isPresent());
        System.out.println("✓ DocumentRepository URI search is case-sensitive as expected");
    }
}