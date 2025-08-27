package com.knowledgegraph.service;

import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractIngestionService Tests")
class AbstractIngestionServiceTest {

    private TestIngestionService testService;

    @Mock
    private DocumentRepository documentRepository;

    // Concrete implementation for testing
    private class TestIngestionService extends AbstractIngestionService {
        // Test implementation
    }

    @BeforeEach
    void setUp() {
        System.out.println("Setting up AbstractIngestionService test environment...");
        testService = new TestIngestionService();
        testService.documentRepository = documentRepository;
    }

    @Test
    @DisplayName("Create document record - Success")
    void testCreateDocumentRecord_Success() {
        System.out.println("Testing document record creation...");
        
        String filePath = "/test/path/file.txt";
        String contentType = "text/plain";
        String ingestionType = "TEXT";
        
        Document savedDocument = new Document();
        savedDocument.setId(UUID.randomUUID());
        savedDocument.setUri("file://" + filePath);
        savedDocument.setContentType(contentType);
        savedDocument.setLastModified(LocalDateTime.now());
        
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        
        Document result = testService.createDocumentRecord(filePath, contentType, ingestionType);
        
        assertNotNull(result);
        assertEquals("file://" + filePath, result.getUri());
        assertEquals(contentType, result.getContentType());
        assertNotNull(result.getLastModified());
        
        // Verify metadata was set correctly
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        
        Document capturedDoc = docCaptor.getValue();
        assertNotNull(capturedDoc.getMetadata());
        assertEquals(filePath, capturedDoc.getMetadata().get("filePath"));
        assertEquals(ingestionType, capturedDoc.getMetadata().get("ingestionType"));
        assertTrue(capturedDoc.getMetadata().containsKey("ingestedAt"));
        
        System.out.println("✓ Document record created successfully");
    }

    @Test
    @DisplayName("Create document with special characters in path")
    void testCreateDocumentRecord_SpecialCharactersPath() {
        System.out.println("Testing document creation with special characters in path...");
        
        String filePath = "/test/path with spaces/file (1).txt";
        String contentType = "text/plain";
        String ingestionType = "TEXT";
        
        Document savedDocument = new Document();
        savedDocument.setId(UUID.randomUUID());
        savedDocument.setUri("file://" + filePath);
        
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        
        Document result = testService.createDocumentRecord(filePath, contentType, ingestionType);
        
        assertNotNull(result);
        assertEquals("file://" + filePath, result.getUri());
        
        System.out.println("✓ Document with special characters handled correctly");
    }

    @Test
    @DisplayName("Build error result - Single error")
    void testBuildErrorResult_SingleError() {
        System.out.println("Testing error result builder...");
        
        UUID jobId = UUID.randomUUID();
        String errorMessage = "File not found";
        String errorType = "FILE_ERROR";
        
        IngestionResult result = testService.buildErrorResult(jobId, errorMessage, errorType);
        
        assertNotNull(result);
        assertEquals(jobId, result.getJobId());
        assertFalse(result.isSuccess());
        assertEquals(errorMessage, result.getMessage());
        assertEquals(0, result.getTotalRecords());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());
        assertNotNull(result.getProcessedAt());
        
        // Check error details
        assertNotNull(result.getErrors());
        assertEquals(1, result.getErrors().size());
        
        IngestionResult.ProcessingError error = result.getErrors().get(0);
        assertEquals(errorMessage, error.getErrorMessage());
        assertEquals(errorType, error.getErrorType());
        
        System.out.println("✓ Error result built correctly");
    }

    @Test
    @DisplayName("Build success result - No errors")
    void testBuildSuccessResult_NoErrors() {
        System.out.println("Testing success result builder with no errors...");
        
        UUID jobId = UUID.randomUUID();
        String message = "Processing completed successfully";
        int totalRecords = 10;
        int successCount = 10;
        int errorCount = 0;
        
        List<UUID> nodeIds = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());
        List<UUID> edgeIds = Arrays.asList(UUID.randomUUID());
        List<UUID> docIds = Arrays.asList(UUID.randomUUID());
        
        IngestionResult result = testService.buildSuccessResult(
            jobId, message, totalRecords, successCount, errorCount,
            nodeIds, edgeIds, docIds, null
        );
        
        assertNotNull(result);
        assertEquals(jobId, result.getJobId());
        assertTrue(result.isSuccess());
        assertEquals(message, result.getMessage());
        assertEquals(totalRecords, result.getTotalRecords());
        assertEquals(successCount, result.getSuccessCount());
        assertEquals(errorCount, result.getErrorCount());
        assertNotNull(result.getProcessedAt());
        
        // Check created IDs
        assertEquals(nodeIds, result.getCreatedNodeIds());
        assertEquals(edgeIds, result.getCreatedEdgeIds());
        assertEquals(docIds, result.getCreatedDocumentIds());
        
        System.out.println("✓ Success result built correctly");
    }

    @Test
    @DisplayName("Build partial success result - With errors")
    void testBuildSuccessResult_PartialSuccess() {
        System.out.println("Testing partial success result builder...");
        
        UUID jobId = UUID.randomUUID();
        String message = "Processing completed with errors";
        int totalRecords = 10;
        int successCount = 8;
        int errorCount = 2;
        
        List<UUID> nodeIds = Arrays.asList(UUID.randomUUID());
        List<IngestionResult.ProcessingError> errors = Arrays.asList(
            IngestionResult.ProcessingError.builder()
                .errorMessage("Record 3 failed")
                .errorType("VALIDATION_ERROR")
                .build(),
            IngestionResult.ProcessingError.builder()
                .errorMessage("Record 7 failed")
                .errorType("PROCESSING_ERROR")
                .build()
        );
        
        IngestionResult result = testService.buildSuccessResult(
            jobId, message, totalRecords, successCount, errorCount,
            nodeIds, null, null, errors
        );
        
        assertNotNull(result);
        assertFalse(result.isSuccess()); // Has errors, so not fully successful
        assertEquals(totalRecords, result.getTotalRecords());
        assertEquals(successCount, result.getSuccessCount());
        assertEquals(errorCount, result.getErrorCount());
        assertEquals(errors, result.getErrors());
        
        System.out.println("✓ Partial success result built correctly");
    }

    @Test
    @DisplayName("Build success result - Null edge IDs handled")
    void testBuildSuccessResult_NullEdgeIds() {
        System.out.println("Testing success result with null edge IDs...");
        
        UUID jobId = UUID.randomUUID();
        
        IngestionResult result = testService.buildSuccessResult(
            jobId, "Success", 1, 1, 0,
            Arrays.asList(UUID.randomUUID()),
            null, // null edge IDs
            Arrays.asList(UUID.randomUUID()),
            null
        );
        
        assertNotNull(result.getCreatedEdgeIds());
        assertTrue(result.getCreatedEdgeIds().isEmpty());
        
        System.out.println("✓ Null edge IDs handled correctly");
    }

    @Test
    @DisplayName("Create document with empty metadata")
    void testCreateDocumentRecord_EmptyMetadata() {
        System.out.println("Testing document creation with minimal data...");
        
        String filePath = "";
        String contentType = "";
        String ingestionType = "";
        
        Document savedDocument = new Document();
        savedDocument.setId(UUID.randomUUID());
        
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        
        Document result = testService.createDocumentRecord(filePath, contentType, ingestionType);
        
        assertNotNull(result);
        assertEquals("file://", result.getUri());
        
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        
        Document capturedDoc = docCaptor.getValue();
        Map<String, Object> metadata = capturedDoc.getMetadata();
        assertEquals("", metadata.get("filePath"));
        assertEquals("", metadata.get("ingestionType"));
        
        System.out.println("✓ Empty metadata handled correctly");
    }

    @Test
    @DisplayName("Build error result with null message")
    void testBuildErrorResult_NullMessage() {
        System.out.println("Testing error result with null message...");
        
        UUID jobId = UUID.randomUUID();
        
        IngestionResult result = testService.buildErrorResult(jobId, null, "ERROR_TYPE");
        
        assertNotNull(result);
        assertNull(result.getMessage());
        assertEquals(1, result.getErrorCount());
        assertNull(result.getErrors().get(0).getErrorMessage());
        
        System.out.println("✓ Null message in error result handled correctly");
    }

    @Test
    @DisplayName("Timestamp consistency in results")
    void testTimestampConsistency() {
        System.out.println("Testing timestamp consistency...");
        
        UUID jobId = UUID.randomUUID();
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);
        
        IngestionResult errorResult = testService.buildErrorResult(
            jobId, "Error", "ERROR"
        );
        
        IngestionResult successResult = testService.buildSuccessResult(
            jobId, "Success", 1, 1, 0, null, null, null, null
        );
        
        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);
        
        // Check that timestamps are within expected range
        assertTrue(errorResult.getProcessedAt().isAfter(beforeCall));
        assertTrue(errorResult.getProcessedAt().isBefore(afterCall));
        assertTrue(successResult.getProcessedAt().isAfter(beforeCall));
        assertTrue(successResult.getProcessedAt().isBefore(afterCall));
        
        System.out.println("✓ Timestamps are consistent and correct");
    }

    @Test
    @DisplayName("Document record with large metadata")
    void testCreateDocumentRecord_LargeMetadata() {
        System.out.println("Testing document creation with large metadata...");
        
        StringBuilder largePath = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largePath.append("/folder").append(i);
        }
        largePath.append("/file.txt");
        
        String filePath = largePath.toString();
        String contentType = "application/octet-stream";
        String ingestionType = "LARGE_FILE_INGESTION_TYPE_WITH_LONG_NAME";
        
        Document savedDocument = new Document();
        savedDocument.setId(UUID.randomUUID());
        
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);
        
        Document result = testService.createDocumentRecord(filePath, contentType, ingestionType);
        
        assertNotNull(result);
        
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        
        Document capturedDoc = docCaptor.getValue();
        assertEquals(filePath, capturedDoc.getMetadata().get("filePath"));
        assertEquals(ingestionType, capturedDoc.getMetadata().get("ingestionType"));
        
        System.out.println("✓ Large metadata handled correctly");
    }

    @Test
    @DisplayName("Success result with maximum records")
    void testBuildSuccessResult_MaxRecords() {
        System.out.println("Testing success result with maximum records...");
        
        UUID jobId = UUID.randomUUID();
        int totalRecords = Integer.MAX_VALUE;
        int successCount = Integer.MAX_VALUE;
        
        IngestionResult result = testService.buildSuccessResult(
            jobId, "Max records processed", totalRecords, successCount, 0,
            null, null, null, null
        );
        
        assertEquals(totalRecords, result.getTotalRecords());
        assertEquals(successCount, result.getSuccessCount());
        assertTrue(result.isSuccess());
        
        System.out.println("✓ Maximum records handled correctly");
    }
}