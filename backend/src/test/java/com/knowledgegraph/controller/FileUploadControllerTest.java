package com.knowledgegraph.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgegraph.TestConfiguration;
import com.knowledgegraph.dto.FileUploadResponse;
import com.knowledgegraph.dto.IngestionJob;
import com.knowledgegraph.service.FileStorageService;
import com.knowledgegraph.service.IngestionJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)
@ActiveProfiles("test")
@Import(TestConfiguration.class)
@DisplayName("FileUploadController Tests")
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private IngestionJobService ingestionJobService;

    @TempDir
    Path tempDir;

    private MockMultipartFile validCsvFile;
    private MockMultipartFile validJsonFile;
    private MockMultipartFile validPdfFile;
    private MockMultipartFile emptyFile;
    private MockMultipartFile invalidFile;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up test files and mocks...");
        
        // Create test files
        validCsvFile = new MockMultipartFile(
            "file",
            "test-data.csv",
            "text/csv",
            "name,age\nJohn,30\nJane,25".getBytes()
        );

        validJsonFile = new MockMultipartFile(
            "file",
            "test-data.json",
            "application/json",
            "{\"test\": \"data\"}".getBytes()
        );

        validPdfFile = new MockMultipartFile(
            "file",
            "test-document.pdf",
            "application/pdf",
            "PDF content simulation".getBytes()
        );

        emptyFile = new MockMultipartFile(
            "file",
            "empty.txt",
            "text/plain",
            new byte[0]
        );

        invalidFile = new MockMultipartFile(
            "file",
            "test.exe",
            "application/octet-stream",
            "executable content".getBytes()
        );
    }

    @Test
    @DisplayName("Upload single CSV file - Success")
    void testUploadCsvFile_Success() throws Exception {
        System.out.println("Testing successful CSV file upload...");
        
        String storedPath = tempDir.resolve("2024-01-15/file123.csv").toString();
        UUID jobId = UUID.randomUUID();
        
        when(fileStorageService.storeFile(any(MockMultipartFile.class)))
            .thenReturn(storedPath);
        
        IngestionJob job = createTestJob(jobId, IngestionJob.JobType.CSV_IMPORT);
        when(ingestionJobService.createJob(
            eq(IngestionJob.JobType.CSV_IMPORT),
            eq("test-data.csv"),
            eq(storedPath),
            eq("text/csv"),
            anyLong()
        )).thenReturn(job);

        System.out.println("Performing file upload request...");
        MvcResult result = mockMvc.perform(multipart("/ingest/upload")
                .file(validCsvFile))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.fileName", is("test-data.csv")))
                .andExpect(jsonPath("$.contentType", is("text/csv")))
                .andExpect(jsonPath("$.status", is("UPLOADED")))
                .andExpect(jsonPath("$.jobId", is(jobId.toString())))
                .andExpect(jsonPath("$.message", containsString("successfully")))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        System.out.println("Response: " + responseBody);
        
        FileUploadResponse response = objectMapper.readValue(responseBody, FileUploadResponse.class);
        assertNotNull(response.getFileId());
        assertEquals("test-data.csv", response.getFileName());
        assertEquals("UPLOADED", response.getStatus());
        
        verify(fileStorageService, times(1)).storeFile(any(MockMultipartFile.class));
        verify(ingestionJobService, times(1)).createJob(any(), any(), any(), any(), anyLong());
        
        System.out.println("✓ CSV file uploaded successfully");
    }

    @Test
    @DisplayName("Upload JSON file - Success")
    void testUploadJsonFile_Success() throws Exception {
        System.out.println("Testing successful JSON file upload...");
        
        String storedPath = tempDir.resolve("2024-01-15/file456.json").toString();
        UUID jobId = UUID.randomUUID();
        
        when(fileStorageService.storeFile(any(MockMultipartFile.class)))
            .thenReturn(storedPath);
        
        IngestionJob job = createTestJob(jobId, IngestionJob.JobType.JSON_IMPORT);
        when(ingestionJobService.createJob(
            eq(IngestionJob.JobType.JSON_IMPORT),
            anyString(),
            anyString(),
            anyString(),
            anyLong()
        )).thenReturn(job);

        mockMvc.perform(multipart("/ingest/upload")
                .file(validJsonFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName", is("test-data.json")))
                .andExpect(jsonPath("$.contentType", is("application/json")))
                .andExpect(jsonPath("$.status", is("UPLOADED")));
        
        System.out.println("✓ JSON file uploaded successfully");
    }

    @Test
    @DisplayName("Upload PDF file - Success")
    void testUploadPdfFile_Success() throws Exception {
        System.out.println("Testing successful PDF file upload...");
        
        String storedPath = tempDir.resolve("2024-01-15/file789.pdf").toString();
        UUID jobId = UUID.randomUUID();
        
        when(fileStorageService.storeFile(any(MockMultipartFile.class)))
            .thenReturn(storedPath);
        
        IngestionJob job = createTestJob(jobId, IngestionJob.JobType.PDF_EXTRACTION);
        when(ingestionJobService.createJob(
            eq(IngestionJob.JobType.PDF_EXTRACTION),
            anyString(),
            anyString(),
            anyString(),
            anyLong()
        )).thenReturn(job);

        mockMvc.perform(multipart("/ingest/upload")
                .file(validPdfFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName", is("test-document.pdf")))
                .andExpect(jsonPath("$.status", is("UPLOADED")));
        
        System.out.println("✓ PDF file uploaded successfully");
    }

    @Test
    @DisplayName("Upload empty file - Bad Request")
    void testUploadEmptyFile_BadRequest() throws Exception {
        System.out.println("Testing empty file upload validation...");
        
        when(fileStorageService.storeFile(any()))
            .thenThrow(new IllegalArgumentException("Cannot upload empty file"));

        mockMvc.perform(multipart("/ingest/upload")
                .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", containsString("empty")));
        
        verify(ingestionJobService, never()).createJob(any(), any(), any(), any(), anyLong());
        
        System.out.println("✓ Empty file upload correctly rejected");
    }

    @Test
    @DisplayName("Upload invalid file type - Bad Request")
    void testUploadInvalidFileType_BadRequest() throws Exception {
        System.out.println("Testing invalid file type validation...");
        
        when(fileStorageService.storeFile(any()))
            .thenThrow(new IllegalArgumentException("File type not allowed"));

        mockMvc.perform(multipart("/ingest/upload")
                .file(invalidFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.message", containsString("not allowed")));
        
        System.out.println("✓ Invalid file type correctly rejected");
    }

    @Test
    @DisplayName("Upload file - Storage Error")
    void testUploadFile_StorageError() throws Exception {
        System.out.println("Testing file storage error handling...");
        
        when(fileStorageService.storeFile(any()))
            .thenThrow(new IOException("Disk full"));

        mockMvc.perform(multipart("/ingest/upload")
                .file(validCsvFile))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is("ERROR")))
                .andExpect(jsonPath("$.message", containsString("Failed to store")));
        
        System.out.println("✓ Storage error handled correctly");
    }

    @Test
    @DisplayName("Upload multiple files - Success")
    void testUploadMultipleFiles_Success() throws Exception {
        System.out.println("Testing multiple file upload...");
        
        MockMultipartFile file1 = new MockMultipartFile(
            "files", "file1.csv", "text/csv", "data1".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
            "files", "file2.json", "application/json", "{}".getBytes()
        );
        
        when(fileStorageService.storeFile(any()))
            .thenReturn("path1.csv", "path2.json");
        when(ingestionJobService.createJob(any(), any(), any(), any(), anyLong()))
            .thenReturn(
                createTestJob(UUID.randomUUID(), IngestionJob.JobType.CSV_IMPORT),
                createTestJob(UUID.randomUUID(), IngestionJob.JobType.JSON_IMPORT)
            );

        MvcResult result = mockMvc.perform(multipart("/ingest/upload-multiple")
                .file(file1)
                .file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].fileName", is("file1.csv")))
                .andExpect(jsonPath("$[1].fileName", is("file2.json")))
                .andReturn();

        System.out.println("Response: " + result.getResponse().getContentAsString());
        System.out.println("✓ Multiple files uploaded successfully");
    }

    @Test
    @DisplayName("Get all jobs - Success")
    void testGetAllJobs_Success() throws Exception {
        System.out.println("Testing get all jobs endpoint...");
        
        List<IngestionJob> jobs = Arrays.asList(
            createTestJob(UUID.randomUUID(), IngestionJob.JobType.CSV_IMPORT),
            createTestJob(UUID.randomUUID(), IngestionJob.JobType.JSON_IMPORT)
        );
        
        when(ingestionJobService.getAllJobs()).thenReturn(jobs);

        mockMvc.perform(get("/ingest/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].status", is("PENDING")))
                .andExpect(jsonPath("$[1].status", is("PENDING")));
        
        System.out.println("✓ Retrieved all jobs successfully");
    }

    @Test
    @DisplayName("Get job by ID - Success")
    void testGetJobById_Success() throws Exception {
        System.out.println("Testing get job by ID endpoint...");
        
        UUID jobId = UUID.randomUUID();
        IngestionJob job = createTestJob(jobId, IngestionJob.JobType.CSV_IMPORT);
        
        when(ingestionJobService.getJob(jobId)).thenReturn(job);

        mockMvc.perform(get("/ingest/jobs/{jobId}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId", is(jobId.toString())))
                .andExpect(jsonPath("$.status", is("PENDING")));
        
        System.out.println("✓ Retrieved job by ID successfully");
    }

    @Test
    @DisplayName("Get job by ID - Not Found")
    void testGetJobById_NotFound() throws Exception {
        System.out.println("Testing get job by ID when not found...");
        
        UUID jobId = UUID.randomUUID();
        when(ingestionJobService.getJob(jobId)).thenReturn(null);

        mockMvc.perform(get("/ingest/jobs/{jobId}", jobId))
                .andExpect(status().isNotFound());
        
        System.out.println("✓ Job not found handled correctly");
    }

    @Test
    @DisplayName("Get jobs by status - Success")
    void testGetJobsByStatus_Success() throws Exception {
        System.out.println("Testing get jobs by status endpoint...");
        
        List<IngestionJob> pendingJobs = Arrays.asList(
            createTestJob(UUID.randomUUID(), IngestionJob.JobType.CSV_IMPORT)
        );
        
        when(ingestionJobService.getJobsByStatus(IngestionJob.JobStatus.PENDING))
            .thenReturn(pendingJobs);

        mockMvc.perform(get("/ingest/jobs/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));
        
        System.out.println("✓ Retrieved jobs by status successfully");
    }

    @Test
    @DisplayName("Cancel job - Success")
    void testCancelJob_Success() throws Exception {
        System.out.println("Testing cancel job endpoint...");
        
        UUID jobId = UUID.randomUUID();
        when(ingestionJobService.cancelJob(jobId)).thenReturn(true);

        mockMvc.perform(post("/ingest/jobs/{jobId}/cancel", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled", is(true)))
                .andExpect(jsonPath("$.message", containsString("successfully")));
        
        System.out.println("✓ Job cancelled successfully");
    }

    @Test
    @DisplayName("Cancel job - Cannot Cancel")
    void testCancelJob_CannotCancel() throws Exception {
        System.out.println("Testing cancel job when cannot cancel...");
        
        UUID jobId = UUID.randomUUID();
        when(ingestionJobService.cancelJob(jobId)).thenReturn(false);

        mockMvc.perform(post("/ingest/jobs/{jobId}/cancel", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled", is(false)))
                .andExpect(jsonPath("$.message", containsString("cannot be cancelled")));
        
        System.out.println("✓ Cannot cancel job handled correctly");
    }

    @Test
    @DisplayName("Retry job - Success")
    void testRetryJob_Success() throws Exception {
        System.out.println("Testing retry job endpoint...");
        
        UUID jobId = UUID.randomUUID();
        doNothing().when(ingestionJobService).resubmitJob(jobId);

        mockMvc.perform(post("/ingest/jobs/{jobId}/retry", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("resubmitted")));
        
        verify(ingestionJobService, times(1)).resubmitJob(jobId);
        
        System.out.println("✓ Job resubmitted successfully");
    }

    @Test
    @DisplayName("Get job statistics - Success")
    void testGetJobStatistics_Success() throws Exception {
        System.out.println("Testing get job statistics endpoint...");
        
        Map<String, Long> stats = new HashMap<>();
        stats.put("PENDING", 5L);
        stats.put("PROCESSING", 2L);
        stats.put("COMPLETED", 10L);
        
        when(ingestionJobService.getQueueSize()).thenReturn(7);
        when(ingestionJobService.getJobStatistics()).thenReturn(stats);

        mockMvc.perform(get("/ingest/jobs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueSize", is(7)))
                .andExpect(jsonPath("$.statusCounts.PENDING", is(5)))
                .andExpect(jsonPath("$.statusCounts.PROCESSING", is(2)))
                .andExpect(jsonPath("$.statusCounts.COMPLETED", is(10)));
        
        System.out.println("✓ Job statistics retrieved successfully");
    }

    @Test
    @DisplayName("Test job type determination for various file extensions")
    void testJobTypeDetermination() throws Exception {
        System.out.println("Testing job type determination for different file extensions...");
        
        // Test CSV
        testFileExtensionJobType("data.csv", IngestionJob.JobType.CSV_IMPORT);
        
        // Test JSON
        testFileExtensionJobType("data.json", IngestionJob.JobType.JSON_IMPORT);
        
        // Test PDF
        testFileExtensionJobType("document.pdf", IngestionJob.JobType.PDF_EXTRACTION);
        
        // Test Markdown
        testFileExtensionJobType("notes.md", IngestionJob.JobType.MARKDOWN_PARSE);
        testFileExtensionJobType("readme.markdown", IngestionJob.JobType.MARKDOWN_PARSE);
        
        // Test HTML
        testFileExtensionJobType("page.html", IngestionJob.JobType.WEB_SCRAPE);
        testFileExtensionJobType("page.htm", IngestionJob.JobType.WEB_SCRAPE);
        
        // Test default
        testFileExtensionJobType("data.txt", IngestionJob.JobType.FILE_UPLOAD);
        testFileExtensionJobType("image.png", IngestionJob.JobType.FILE_UPLOAD);
        
        System.out.println("✓ Job type determination working correctly for all file types");
    }

    private void testFileExtensionJobType(String filename, IngestionJob.JobType expectedType) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file", filename, "application/octet-stream", "content".getBytes()
        );
        
        when(fileStorageService.storeFile(any())).thenReturn("path/" + filename);
        when(ingestionJobService.createJob(eq(expectedType), anyString(), anyString(), anyString(), anyLong()))
            .thenReturn(createTestJob(UUID.randomUUID(), expectedType));
        
        mockMvc.perform(multipart("/ingest/upload").file(file))
                .andExpect(status().isOk());
        
        verify(ingestionJobService).createJob(eq(expectedType), eq(filename), anyString(), anyString(), anyLong());
        reset(ingestionJobService);
    }

    private IngestionJob createTestJob(UUID jobId, IngestionJob.JobType jobType) {
        IngestionJob job = new IngestionJob();
        job.setJobId(jobId);
        job.setJobType(jobType);
        job.setStatus(IngestionJob.JobStatus.PENDING);
        job.setFileName("test-file");
        job.setFilePath("/test/path");
        job.setCreatedAt(LocalDateTime.now());
        return job;
    }
}