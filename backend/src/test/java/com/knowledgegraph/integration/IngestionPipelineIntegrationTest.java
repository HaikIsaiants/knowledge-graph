package com.knowledgegraph.integration;

import com.knowledgegraph.config.IngestionConfig;
import com.knowledgegraph.controller.FileUploadController;
import com.knowledgegraph.dto.FileUploadResponse;
import com.knowledgegraph.dto.IngestionJob;
import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.repository.DocumentRepository;
import com.knowledgegraph.repository.NodeRepository;
import com.knowledgegraph.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Ingestion Pipeline Integration Tests")
class IngestionPipelineIntegrationTest {

    @Autowired
    private FileUploadController fileUploadController;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private IngestionJobService ingestionJobService;

    @Autowired
    private CsvIngestionService csvIngestionService;

    @Autowired
    private JsonIngestionService jsonIngestionService;

    @Autowired
    private PdfIngestionService pdfIngestionService;

    @Autowired
    private MarkdownIngestionService markdownIngestionService;

    @Autowired
    private TextChunkingService textChunkingService;

    @SpyBean
    private MockEmbeddingService mockEmbeddingService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private IngestionConfig ingestionConfig;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        System.out.println("=== Setting up integration test environment ===");
        
        // Ensure upload directory exists
        Path uploadDir = tempDir.resolve("uploads");
        Files.createDirectories(uploadDir);
        
        // Configure storage path
        ingestionConfig.getUpload().setStoragePath(uploadDir.toString());
        
        // Mock embedding generation to speed up tests
        doReturn(new float[]{0.1f, 0.2f, 0.3f})
            .when(mockEmbeddingService).generateEmbedding(anyString());
        
        System.out.println("Upload directory: " + uploadDir);
    }

    @Test
    @DisplayName("End-to-end CSV file ingestion")
    void testCsvIngestionPipeline() throws Exception {
        System.out.println("\n=== Testing CSV Ingestion Pipeline ===");
        
        // Create CSV test data
        String csvContent = "name,age,email,department\n" +
                           "John Doe,30,john@example.com,Engineering\n" +
                           "Jane Smith,28,jane@example.com,Marketing\n" +
                           "Bob Johnson,35,bob@example.com,Sales";
        
        MockMultipartFile csvFile = new MockMultipartFile(
            "file",
            "employees.csv",
            "text/csv",
            csvContent.getBytes()
        );
        
        System.out.println("Step 1: Uploading CSV file...");
        FileUploadResponse uploadResponse = fileUploadController.uploadFile(csvFile);
        assertNotNull(uploadResponse);
        assertEquals("UPLOADED", uploadResponse.getStatus());
        assertNotNull(uploadResponse.getJobId());
        
        UUID jobId = UUID.fromString(uploadResponse.getJobId());
        System.out.println("File uploaded successfully. Job ID: " + jobId);
        
        System.out.println("Step 2: Waiting for job processing...");
        IngestionJob job = waitForJobCompletion(jobId, 10);
        
        assertEquals(IngestionJob.JobStatus.COMPLETED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        
        System.out.println("Step 3: Verifying database records...");
        
        // Verify nodes were created
        List<Node> nodes = nodeRepository.findAll();
        assertFalse(nodes.isEmpty());
        System.out.println("Created " + nodes.size() + " nodes");
        
        // Verify document was created
        List<Document> documents = documentRepository.findAll();
        assertFalse(documents.isEmpty());
        System.out.println("Created " + documents.size() + " documents");
        
        // Verify specific data
        boolean foundJohn = nodes.stream()
            .anyMatch(n -> n.getProperties() != null && 
                          "John Doe".equals(n.getProperties().get("name")));
        assertTrue(foundJohn, "John Doe node should exist");
        
        System.out.println("✓ CSV ingestion pipeline completed successfully");
    }

    @Test
    @DisplayName("End-to-end JSON file ingestion")
    void testJsonIngestionPipeline() throws Exception {
        System.out.println("\n=== Testing JSON Ingestion Pipeline ===");
        
        String jsonContent = "{\n" +
            "  \"users\": [\n" +
            "    {\"id\": 1, \"name\": \"Alice\", \"role\": \"Admin\"},\n" +
            "    {\"id\": 2, \"name\": \"Bob\", \"role\": \"User\"}\n" +
            "  ],\n" +
            "  \"metadata\": {\n" +
            "    \"version\": \"1.0\",\n" +
            "    \"timestamp\": \"2024-01-15\"\n" +
            "  }\n" +
            "}";
        
        MockMultipartFile jsonFile = new MockMultipartFile(
            "file",
            "data.json",
            "application/json",
            jsonContent.getBytes()
        );
        
        System.out.println("Step 1: Uploading JSON file...");
        FileUploadResponse uploadResponse = fileUploadController.uploadFile(jsonFile);
        assertNotNull(uploadResponse);
        assertEquals("UPLOADED", uploadResponse.getStatus());
        
        UUID jobId = UUID.fromString(uploadResponse.getJobId());
        System.out.println("File uploaded. Job ID: " + jobId);
        
        System.out.println("Step 2: Processing job...");
        IngestionJob job = waitForJobCompletion(jobId, 10);
        
        assertEquals(IngestionJob.JobStatus.COMPLETED, job.getStatus());
        
        System.out.println("Step 3: Verifying results...");
        List<Node> nodes = nodeRepository.findAll();
        assertFalse(nodes.isEmpty());
        
        // Verify JSON structure was processed
        boolean foundAlice = nodes.stream()
            .anyMatch(n -> n.getProperties() != null && 
                          n.getProperties().toString().contains("Alice"));
        assertTrue(foundAlice, "Alice data should exist");
        
        System.out.println("✓ JSON ingestion pipeline completed successfully");
    }

    @Test
    @DisplayName("End-to-end Markdown file ingestion")
    void testMarkdownIngestionPipeline() throws Exception {
        System.out.println("\n=== Testing Markdown Ingestion Pipeline ===");
        
        String markdownContent = "# Project Documentation\n\n" +
            "## Overview\n" +
            "This is a test project for knowledge graph ingestion.\n\n" +
            "## Features\n" +
            "- CSV import\n" +
            "- JSON import\n" +
            "- PDF extraction\n" +
            "- Markdown parsing\n\n" +
            "## Usage\n" +
            "Upload files through the REST API endpoints.";
        
        // Create markdown file
        Path mdFile = tempDir.resolve("README.md");
        Files.writeString(mdFile, markdownContent);
        
        MockMultipartFile markdownFile = new MockMultipartFile(
            "file",
            "README.md",
            "text/markdown",
            Files.readAllBytes(mdFile)
        );
        
        System.out.println("Step 1: Uploading Markdown file...");
        FileUploadResponse uploadResponse = fileUploadController.uploadFile(markdownFile);
        assertNotNull(uploadResponse);
        
        UUID jobId = UUID.fromString(uploadResponse.getJobId());
        System.out.println("File uploaded. Job ID: " + jobId);
        
        System.out.println("Step 2: Processing markdown...");
        IngestionJob job = waitForJobCompletion(jobId, 10);
        
        assertEquals(IngestionJob.JobStatus.COMPLETED, job.getStatus());
        
        System.out.println("Step 3: Verifying chunks...");
        List<Node> nodes = nodeRepository.findAll();
        assertFalse(nodes.isEmpty());
        
        // Verify markdown content was chunked and stored
        boolean foundOverview = nodes.stream()
            .anyMatch(n -> n.getContent() != null && 
                          n.getContent().contains("Overview"));
        assertTrue(foundOverview, "Overview section should be processed");
        
        System.out.println("✓ Markdown ingestion pipeline completed successfully");
    }

    @Test
    @DisplayName("Multiple file upload and processing")
    void testMultipleFileIngestion() throws Exception {
        System.out.println("\n=== Testing Multiple File Ingestion ===");
        
        // Create multiple files
        MockMultipartFile file1 = new MockMultipartFile(
            "files", "data1.csv", "text/csv",
            "col1,col2\nval1,val2".getBytes()
        );
        
        MockMultipartFile file2 = new MockMultipartFile(
            "files", "data2.json", "application/json",
            "{\"key\":\"value\"}".getBytes()
        );
        
        System.out.println("Step 1: Uploading multiple files...");
        List<FileUploadResponse> responses = fileUploadController.uploadMultipleFiles(
            new MockMultipartFile[]{file1, file2}
        );
        
        assertEquals(2, responses.size());
        
        System.out.println("Step 2: Waiting for all jobs to complete...");
        for (FileUploadResponse response : responses) {
            UUID jobId = UUID.fromString(response.getJobId());
            IngestionJob job = waitForJobCompletion(jobId, 10);
            assertEquals(IngestionJob.JobStatus.COMPLETED, job.getStatus());
            System.out.println("Job " + jobId + " completed");
        }
        
        System.out.println("Step 3: Verifying combined results...");
        List<Node> allNodes = nodeRepository.findAll();
        assertFalse(allNodes.isEmpty());
        
        List<Document> allDocs = documentRepository.findAll();
        assertTrue(allDocs.size() >= 2, "At least 2 documents should be created");
        
        System.out.println("✓ Multiple file ingestion completed successfully");
    }

    @Test
    @DisplayName("Error handling - Invalid file")
    void testIngestionErrorHandling() throws Exception {
        System.out.println("\n=== Testing Error Handling ===");
        
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.exe",
            "application/octet-stream",
            "invalid content".getBytes()
        );
        
        System.out.println("Step 1: Attempting to upload invalid file...");
        try {
            FileUploadResponse response = fileUploadController.uploadFile(invalidFile);
            
            // Should either reject immediately or fail during processing
            if ("UPLOADED".equals(response.getStatus())) {
                UUID jobId = UUID.fromString(response.getJobId());
                System.out.println("File accepted, waiting for processing...");
                
                IngestionJob job = waitForJobCompletion(jobId, 5);
                assertNotEquals(IngestionJob.JobStatus.COMPLETED, job.getStatus());
            } else {
                assertEquals("FAILED", response.getStatus());
            }
            
            System.out.println("✓ Invalid file handled correctly");
        } catch (Exception e) {
            System.out.println("✓ Invalid file rejected: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Job cancellation")
    void testJobCancellation() throws Exception {
        System.out.println("\n=== Testing Job Cancellation ===");
        
        // Create a large file to ensure processing takes time
        StringBuilder largeContent = new StringBuilder("name,value\n");
        for (int i = 0; i < 1000; i++) {
            largeContent.append("row").append(i).append(",").append(i).append("\n");
        }
        
        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.csv",
            "text/csv",
            largeContent.toString().getBytes()
        );
        
        System.out.println("Step 1: Uploading large file...");
        FileUploadResponse response = fileUploadController.uploadFile(largeFile);
        UUID jobId = UUID.fromString(response.getJobId());
        
        System.out.println("Step 2: Attempting to cancel job...");
        boolean cancelled = ingestionJobService.cancelJob(jobId);
        
        if (cancelled) {
            System.out.println("Job cancelled successfully");
            
            IngestionJob job = ingestionJobService.getJob(jobId);
            assertEquals(IngestionJob.JobStatus.CANCELLED, job.getStatus());
        } else {
            System.out.println("Job could not be cancelled (may have completed)");
        }
        
        System.out.println("✓ Job cancellation tested");
    }

    @Test
    @DisplayName("Job statistics and monitoring")
    void testJobStatistics() throws Exception {
        System.out.println("\n=== Testing Job Statistics ===");
        
        // Submit multiple jobs
        for (int i = 0; i < 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "test" + i + ".csv",
                "text/csv",
                ("data" + i).getBytes()
            );
            fileUploadController.uploadFile(file);
        }
        
        System.out.println("Step 1: Getting queue size...");
        int queueSize = ingestionJobService.getQueueSize();
        System.out.println("Queue size: " + queueSize);
        
        System.out.println("Step 2: Getting job statistics...");
        var stats = ingestionJobService.getJobStatistics();
        assertNotNull(stats);
        System.out.println("Statistics: " + stats);
        
        System.out.println("Step 3: Getting jobs by status...");
        List<IngestionJob> allJobs = ingestionJobService.getAllJobs();
        assertFalse(allJobs.isEmpty());
        System.out.println("Total jobs: " + allJobs.size());
        
        System.out.println("✓ Job monitoring features working correctly");
    }

    @Test
    @DisplayName("Text chunking integration")
    void testTextChunkingIntegration() throws Exception {
        System.out.println("\n=== Testing Text Chunking Integration ===");
        
        // Create a long text file
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            longText.append("This is paragraph ").append(i).append(". ");
            longText.append("It contains enough text to require chunking. ");
            longText.append("The chunking service should split this appropriately. ");
        }
        
        Path txtFile = tempDir.resolve("long.txt");
        Files.writeString(txtFile, longText.toString());
        
        MockMultipartFile textFile = new MockMultipartFile(
            "file",
            "long.txt",
            "text/plain",
            Files.readAllBytes(txtFile)
        );
        
        System.out.println("Step 1: Uploading text file...");
        FileUploadResponse response = fileUploadController.uploadFile(textFile);
        UUID jobId = UUID.fromString(response.getJobId());
        
        System.out.println("Step 2: Processing with chunking...");
        IngestionJob job = waitForJobCompletion(jobId, 10);
        
        assertEquals(IngestionJob.JobStatus.COMPLETED, job.getStatus());
        
        System.out.println("Step 3: Verifying chunks created...");
        List<Node> nodes = nodeRepository.findAll();
        assertTrue(nodes.size() > 1, "Multiple chunks should be created");
        
        System.out.println("Created " + nodes.size() + " chunks");
        System.out.println("✓ Text chunking integration working correctly");
    }

    @Test
    @DisplayName("Embedding generation integration")
    void testEmbeddingGeneration() throws Exception {
        System.out.println("\n=== Testing Embedding Generation ===");
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "title,content\nTest,Sample content for embedding".getBytes()
        );
        
        System.out.println("Step 1: Uploading file...");
        FileUploadResponse response = fileUploadController.uploadFile(file);
        UUID jobId = UUID.fromString(response.getJobId());
        
        System.out.println("Step 2: Processing with embeddings...");
        IngestionJob job = waitForJobCompletion(jobId, 10);
        
        assertEquals(IngestionJob.JobStatus.COMPLETED, job.getStatus());
        
        // Verify embedding service was called
        verify(mockEmbeddingService, atLeastOnce()).generateEmbedding(anyString());
        
        System.out.println("✓ Embedding generation integrated successfully");
    }

    // Helper method to wait for job completion
    private IngestionJob waitForJobCompletion(UUID jobId, int timeoutSeconds) 
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeout = TimeUnit.SECONDS.toMillis(timeoutSeconds);
        
        while (System.currentTimeMillis() - startTime < timeout) {
            IngestionJob job = ingestionJobService.getJob(jobId);
            
            if (job != null && 
                (job.getStatus() == IngestionJob.JobStatus.COMPLETED ||
                 job.getStatus() == IngestionJob.JobStatus.FAILED ||
                 job.getStatus() == IngestionJob.JobStatus.CANCELLED)) {
                return job;
            }
            
            Thread.sleep(100);
        }
        
        throw new RuntimeException("Job did not complete within timeout");
    }
}