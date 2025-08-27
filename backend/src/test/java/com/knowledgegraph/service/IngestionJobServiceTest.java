package com.knowledgegraph.service;

import com.knowledgegraph.dto.IngestionJob;
import com.knowledgegraph.dto.IngestionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IngestionJobService Tests")
class IngestionJobServiceTest {

    @InjectMocks
    private IngestionJobService ingestionJobService;

    @Mock
    private CsvIngestionService csvIngestionService;

    @Mock
    private JsonIngestionService jsonIngestionService;

    @Mock
    private PdfIngestionService pdfIngestionService;

    @Mock
    private MarkdownIngestionService markdownIngestionService;

    private ExecutorService testExecutor;
    private BlockingQueue<IngestionJob> testQueue;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up IngestionJobService for testing...");
        
        // Create test executor and queue
        testExecutor = Executors.newFixedThreadPool(2);
        testQueue = new LinkedBlockingQueue<>();
        
        // Set fields using reflection
        ReflectionTestUtils.setField(ingestionJobService, "executor", testExecutor);
        ReflectionTestUtils.setField(ingestionJobService, "jobQueue", testQueue);
        ReflectionTestUtils.setField(ingestionJobService, "jobStore", new ConcurrentHashMap<>());
        ReflectionTestUtils.setField(ingestionJobService, "maxRetries", 3);
        ReflectionTestUtils.setField(ingestionJobService, "maxConcurrentJobs", 5);
    }

    @Test
    @DisplayName("Create job - CSV import")
    void testCreateJob_CsvImport_Success() {
        System.out.println("Testing CSV import job creation...");
        
        IngestionJob job = ingestionJobService.createJob(
            IngestionJob.JobType.CSV_IMPORT,
            "test.csv",
            "/path/to/test.csv",
            "text/csv",
            1024L
        );
        
        assertNotNull(job);
        assertNotNull(job.getJobId());
        assertEquals(IngestionJob.JobType.CSV_IMPORT, job.getJobType());
        assertEquals("test.csv", job.getFileName());
        assertEquals("/path/to/test.csv", job.getFilePath());
        assertEquals("text/csv", job.getContentType());
        assertEquals(1024L, job.getFileSize());
        assertEquals(IngestionJob.JobStatus.PENDING, job.getStatus());
        assertNotNull(job.getCreatedAt());
        
        System.out.println("Created job: " + job.getJobId());
        System.out.println("✓ CSV import job created successfully");
    }

    @Test
    @DisplayName("Create job - JSON import")
    void testCreateJob_JsonImport_Success() {
        System.out.println("Testing JSON import job creation...");
        
        IngestionJob job = ingestionJobService.createJob(
            IngestionJob.JobType.JSON_IMPORT,
            "data.json",
            "/path/to/data.json",
            "application/json",
            2048L
        );
        
        assertNotNull(job);
        assertEquals(IngestionJob.JobType.JSON_IMPORT, job.getJobType());
        assertEquals("data.json", job.getFileName());
        
        System.out.println("✓ JSON import job created successfully");
    }

    @Test
    @DisplayName("Process job - CSV success")
    void testProcessJob_CsvSuccess() throws Exception {
        System.out.println("Testing successful CSV job processing...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        
        IngestionResult successResult = IngestionResult.builder()
            .jobId(job.getJobId())
            .success(true)
            .message("CSV processed successfully")
            .totalRecords(10)
            .successCount(10)
            .errorCount(0)
            .build();
        
        when(csvIngestionService.ingestCsv(anyString(), any(UUID.class)))
            .thenReturn(successResult);
        
        // Process the job
        ingestionJobService.submitJob(job);
        
        // Wait for processing
        Thread.sleep(100);
        
        // Verify the job was processed
        verify(csvIngestionService, timeout(1000)).ingestCsv(eq(job.getFilePath()), eq(job.getJobId()));
        
        IngestionJob updatedJob = ingestionJobService.getJob(job.getJobId());
        assertNotNull(updatedJob);
        
        System.out.println("✓ CSV job processed successfully");
    }

    @Test
    @DisplayName("Process job - JSON success")
    void testProcessJob_JsonSuccess() throws Exception {
        System.out.println("Testing successful JSON job processing...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.JSON_IMPORT);
        
        IngestionResult successResult = IngestionResult.builder()
            .jobId(job.getJobId())
            .success(true)
            .message("JSON processed successfully")
            .build();
        
        when(jsonIngestionService.ingestJson(anyString(), any(UUID.class)))
            .thenReturn(successResult);
        
        ingestionJobService.submitJob(job);
        Thread.sleep(100);
        
        verify(jsonIngestionService, timeout(1000)).ingestJson(eq(job.getFilePath()), eq(job.getJobId()));
        
        System.out.println("✓ JSON job processed successfully");
    }

    @Test
    @DisplayName("Process job - PDF success")
    void testProcessJob_PdfSuccess() throws Exception {
        System.out.println("Testing successful PDF job processing...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.PDF_EXTRACTION);
        
        IngestionResult successResult = IngestionResult.builder()
            .jobId(job.getJobId())
            .success(true)
            .message("PDF processed successfully")
            .build();
        
        when(pdfIngestionService.ingestPdf(anyString(), any(UUID.class)))
            .thenReturn(successResult);
        
        ingestionJobService.submitJob(job);
        Thread.sleep(100);
        
        verify(pdfIngestionService, timeout(1000)).ingestPdf(eq(job.getFilePath()), eq(job.getJobId()));
        
        System.out.println("✓ PDF job processed successfully");
    }

    @Test
    @DisplayName("Process job - Markdown success")
    void testProcessJob_MarkdownSuccess() throws Exception {
        System.out.println("Testing successful Markdown job processing...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.MARKDOWN_PARSE);
        
        IngestionResult successResult = IngestionResult.builder()
            .jobId(job.getJobId())
            .success(true)
            .message("Markdown processed successfully")
            .build();
        
        when(markdownIngestionService.ingestMarkdown(anyString(), any(UUID.class)))
            .thenReturn(successResult);
        
        ingestionJobService.submitJob(job);
        Thread.sleep(100);
        
        verify(markdownIngestionService, timeout(1000)).ingestMarkdown(eq(job.getFilePath()), eq(job.getJobId()));
        
        System.out.println("✓ Markdown job processed successfully");
    }

    @Test
    @DisplayName("Process job - Processing failure with retry")
    void testProcessJob_FailureWithRetry() throws Exception {
        System.out.println("Testing job processing failure with retry...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        
        IngestionResult failureResult = IngestionResult.builder()
            .jobId(job.getJobId())
            .success(false)
            .message("Processing failed")
            .errorCount(1)
            .build();
        
        when(csvIngestionService.ingestCsv(anyString(), any(UUID.class)))
            .thenReturn(failureResult);
        
        ingestionJobService.submitJob(job);
        Thread.sleep(200);
        
        IngestionJob updatedJob = ingestionJobService.getJob(job.getJobId());
        assertNotNull(updatedJob);
        assertEquals(1, updatedJob.getRetryCount());
        
        System.out.println("✓ Job failure with retry handled correctly");
    }

    @Test
    @DisplayName("Cancel pending job - Success")
    void testCancelJob_PendingJob_Success() {
        System.out.println("Testing cancelling pending job...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        job.setStatus(IngestionJob.JobStatus.PENDING);
        
        // Add job to store
        Map<UUID, IngestionJob> jobStore = new ConcurrentHashMap<>();
        jobStore.put(job.getJobId(), job);
        ReflectionTestUtils.setField(ingestionJobService, "jobStore", jobStore);
        
        boolean cancelled = ingestionJobService.cancelJob(job.getJobId());
        
        assertTrue(cancelled);
        assertEquals(IngestionJob.JobStatus.CANCELLED, job.getStatus());
        assertNotNull(job.getCompletedAt());
        
        System.out.println("✓ Pending job cancelled successfully");
    }

    @Test
    @DisplayName("Cancel completed job - Failure")
    void testCancelJob_CompletedJob_Failure() {
        System.out.println("Testing cancelling completed job...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        job.setStatus(IngestionJob.JobStatus.COMPLETED);
        
        Map<UUID, IngestionJob> jobStore = new ConcurrentHashMap<>();
        jobStore.put(job.getJobId(), job);
        ReflectionTestUtils.setField(ingestionJobService, "jobStore", jobStore);
        
        boolean cancelled = ingestionJobService.cancelJob(job.getJobId());
        
        assertFalse(cancelled);
        assertEquals(IngestionJob.JobStatus.COMPLETED, job.getStatus());
        
        System.out.println("✓ Cannot cancel completed job");
    }

    @Test
    @DisplayName("Resubmit failed job - Success")
    void testResubmitJob_FailedJob_Success() {
        System.out.println("Testing resubmitting failed job...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        job.setStatus(IngestionJob.JobStatus.FAILED);
        job.setRetryCount(2);
        
        Map<UUID, IngestionJob> jobStore = new ConcurrentHashMap<>();
        jobStore.put(job.getJobId(), job);
        ReflectionTestUtils.setField(ingestionJobService, "jobStore", jobStore);
        
        ingestionJobService.resubmitJob(job.getJobId());
        
        assertEquals(IngestionJob.JobStatus.PENDING, job.getStatus());
        assertEquals(0, job.getRetryCount()); // Reset retry count
        
        System.out.println("✓ Failed job resubmitted successfully");
    }

    @Test
    @DisplayName("Get all jobs - Success")
    void testGetAllJobs_Success() {
        System.out.println("Testing getting all jobs...");
        
        Map<UUID, IngestionJob> jobStore = new ConcurrentHashMap<>();
        IngestionJob job1 = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        IngestionJob job2 = createTestJob(IngestionJob.JobType.JSON_IMPORT);
        jobStore.put(job1.getJobId(), job1);
        jobStore.put(job2.getJobId(), job2);
        
        ReflectionTestUtils.setField(ingestionJobService, "jobStore", jobStore);
        
        List<IngestionJob> jobs = ingestionJobService.getAllJobs();
        
        assertEquals(2, jobs.size());
        
        System.out.println("✓ Retrieved all jobs successfully");
    }

    @Test
    @DisplayName("Get jobs by status - Success")
    void testGetJobsByStatus_Success() {
        System.out.println("Testing getting jobs by status...");
        
        Map<UUID, IngestionJob> jobStore = new ConcurrentHashMap<>();
        
        IngestionJob pendingJob = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        pendingJob.setStatus(IngestionJob.JobStatus.PENDING);
        
        IngestionJob processingJob = createTestJob(IngestionJob.JobType.JSON_IMPORT);
        processingJob.setStatus(IngestionJob.JobStatus.PROCESSING);
        
        IngestionJob completedJob = createTestJob(IngestionJob.JobType.PDF_EXTRACTION);
        completedJob.setStatus(IngestionJob.JobStatus.COMPLETED);
        
        jobStore.put(pendingJob.getJobId(), pendingJob);
        jobStore.put(processingJob.getJobId(), processingJob);
        jobStore.put(completedJob.getJobId(), completedJob);
        
        ReflectionTestUtils.setField(ingestionJobService, "jobStore", jobStore);
        
        List<IngestionJob> pendingJobs = ingestionJobService.getJobsByStatus(IngestionJob.JobStatus.PENDING);
        List<IngestionJob> processingJobs = ingestionJobService.getJobsByStatus(IngestionJob.JobStatus.PROCESSING);
        List<IngestionJob> completedJobs = ingestionJobService.getJobsByStatus(IngestionJob.JobStatus.COMPLETED);
        
        assertEquals(1, pendingJobs.size());
        assertEquals(1, processingJobs.size());
        assertEquals(1, completedJobs.size());
        
        System.out.println("✓ Retrieved jobs by status successfully");
    }

    @Test
    @DisplayName("Get job statistics - Success")
    void testGetJobStatistics_Success() {
        System.out.println("Testing job statistics calculation...");
        
        Map<UUID, IngestionJob> jobStore = new ConcurrentHashMap<>();
        
        // Add jobs with different statuses
        for (int i = 0; i < 3; i++) {
            IngestionJob job = createTestJob(IngestionJob.JobType.CSV_IMPORT);
            job.setStatus(IngestionJob.JobStatus.PENDING);
            jobStore.put(job.getJobId(), job);
        }
        
        for (int i = 0; i < 2; i++) {
            IngestionJob job = createTestJob(IngestionJob.JobType.JSON_IMPORT);
            job.setStatus(IngestionJob.JobStatus.PROCESSING);
            jobStore.put(job.getJobId(), job);
        }
        
        for (int i = 0; i < 5; i++) {
            IngestionJob job = createTestJob(IngestionJob.JobType.PDF_EXTRACTION);
            job.setStatus(IngestionJob.JobStatus.COMPLETED);
            jobStore.put(job.getJobId(), job);
        }
        
        IngestionJob failedJob = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        failedJob.setStatus(IngestionJob.JobStatus.FAILED);
        jobStore.put(failedJob.getJobId(), failedJob);
        
        ReflectionTestUtils.setField(ingestionJobService, "jobStore", jobStore);
        
        Map<String, Long> stats = ingestionJobService.getJobStatistics();
        
        assertEquals(3L, stats.get("PENDING"));
        assertEquals(2L, stats.get("PROCESSING"));
        assertEquals(5L, stats.get("COMPLETED"));
        assertEquals(1L, stats.get("FAILED"));
        assertNull(stats.get("CANCELLED"));
        
        System.out.println("Statistics: " + stats);
        System.out.println("✓ Job statistics calculated correctly");
    }

    @Test
    @DisplayName("Get queue size - Success")
    void testGetQueueSize_Success() throws InterruptedException {
        System.out.println("Testing queue size retrieval...");
        
        BlockingQueue<IngestionJob> queue = new LinkedBlockingQueue<>();
        queue.add(createTestJob(IngestionJob.JobType.CSV_IMPORT));
        queue.add(createTestJob(IngestionJob.JobType.JSON_IMPORT));
        queue.add(createTestJob(IngestionJob.JobType.PDF_EXTRACTION));
        
        ReflectionTestUtils.setField(ingestionJobService, "jobQueue", queue);
        
        int queueSize = ingestionJobService.getQueueSize();
        
        assertEquals(3, queueSize);
        
        System.out.println("✓ Queue size retrieved correctly");
    }

    @Test
    @DisplayName("Submit multiple jobs concurrently - Success")
    void testSubmitJobs_Concurrent_Success() throws Exception {
        System.out.println("Testing concurrent job submission...");
        
        int numberOfJobs = 10;
        CountDownLatch latch = new CountDownLatch(numberOfJobs);
        List<IngestionJob> jobs = new ArrayList<>();
        
        for (int i = 0; i < numberOfJobs; i++) {
            IngestionJob job = createTestJob(
                i % 2 == 0 ? IngestionJob.JobType.CSV_IMPORT : IngestionJob.JobType.JSON_IMPORT
            );
            jobs.add(job);
        }
        
        // Submit jobs concurrently
        ExecutorService submitExecutor = Executors.newFixedThreadPool(5);
        for (IngestionJob job : jobs) {
            submitExecutor.submit(() -> {
                ingestionJobService.submitJob(job);
                latch.countDown();
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // Verify all jobs are in the queue or being processed
        for (IngestionJob job : jobs) {
            IngestionJob storedJob = ingestionJobService.getJob(job.getJobId());
            assertNotNull(storedJob);
        }
        
        submitExecutor.shutdown();
        
        System.out.println("✓ Concurrent job submission handled correctly");
    }

    @Test
    @DisplayName("Job retry mechanism - Max retries exceeded")
    void testJobRetry_MaxRetriesExceeded() throws Exception {
        System.out.println("Testing job retry with max retries exceeded...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        job.setRetryCount(3); // Already at max retries
        
        Map<UUID, IngestionJob> jobStore = new ConcurrentHashMap<>();
        jobStore.put(job.getJobId(), job);
        ReflectionTestUtils.setField(ingestionJobService, "jobStore", jobStore);
        
        IngestionResult failureResult = IngestionResult.builder()
            .jobId(job.getJobId())
            .success(false)
            .message("Processing failed")
            .build();
        
        when(csvIngestionService.ingestCsv(anyString(), any(UUID.class)))
            .thenReturn(failureResult);
        
        ingestionJobService.submitJob(job);
        Thread.sleep(200);
        
        IngestionJob updatedJob = ingestionJobService.getJob(job.getJobId());
        assertEquals(IngestionJob.JobStatus.FAILED, updatedJob.getStatus());
        
        System.out.println("✓ Max retries exceeded handled correctly");
    }

    @Test
    @DisplayName("Process job with exception - Error handling")
    void testProcessJob_Exception_ErrorHandling() throws Exception {
        System.out.println("Testing job processing with exception...");
        
        IngestionJob job = createTestJob(IngestionJob.JobType.CSV_IMPORT);
        
        when(csvIngestionService.ingestCsv(anyString(), any(UUID.class)))
            .thenThrow(new RuntimeException("Unexpected error"));
        
        ingestionJobService.submitJob(job);
        Thread.sleep(200);
        
        IngestionJob updatedJob = ingestionJobService.getJob(job.getJobId());
        assertNotNull(updatedJob.getErrorMessage());
        assertTrue(updatedJob.getErrorMessage().contains("Unexpected error"));
        
        System.out.println("✓ Exception during processing handled correctly");
    }

    private IngestionJob createTestJob(IngestionJob.JobType jobType) {
        IngestionJob job = new IngestionJob();
        job.setJobId(UUID.randomUUID());
        job.setJobType(jobType);
        job.setStatus(IngestionJob.JobStatus.PENDING);
        job.setFileName("test-file");
        job.setFilePath("/test/path");
        job.setContentType("text/plain");
        job.setFileSize(1024L);
        job.setCreatedAt(LocalDateTime.now());
        job.setRetryCount(0);
        return job;
    }
}