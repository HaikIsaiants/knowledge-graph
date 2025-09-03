package com.knowledgegraph.service;

import com.knowledgegraph.dto.IngestionJob;
import com.knowledgegraph.dto.IngestionResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class IngestionJobService {

    private final Map<UUID, IngestionJob> jobs = new ConcurrentHashMap<>();
    private final BlockingQueue<IngestionJob> jobQueue = new LinkedBlockingQueue<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private volatile boolean running = true;
    
    // Lazy injection to avoid circular dependency
    @Autowired
    @Lazy
    private CsvIngestionService csvIngestionService;
    
    @Autowired
    @Lazy
    private JsonIngestionService jsonIngestionService;
    
    @Autowired
    @Lazy
    private PdfIngestionService pdfIngestionService;
    
    @Autowired
    @Lazy
    private MarkdownIngestionService markdownIngestionService;

    @PostConstruct
    public void init() {
        // Start job processor threads
        for (int i = 0; i < 10; i++) {
            executorService.submit(this::processJobs);
        }
        log.info("IngestionJobService initialized with 10 worker threads");
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("IngestionJobService shutdown complete");
    }

    public IngestionJob createJob(IngestionJob.JobType jobType, String fileName, String filePath, 
                                   String contentType, Long fileSize) {
        IngestionJob job = IngestionJob.builder()
                .jobId(UUID.randomUUID())
                .jobType(jobType)
                .status(IngestionJob.JobStatus.PENDING)
                .fileName(fileName)
                .filePath(filePath)
                .contentType(contentType)
                .fileSize(fileSize)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .metadata(new HashMap<>())
                .build();
        
        jobs.put(job.getJobId(), job);
        jobQueue.offer(job);
        
        log.info("Created job: {} for file: {}", job.getJobId(), fileName);
        return job;
    }

    public IngestionJob getJob(UUID jobId) {
        return jobs.get(jobId);
    }

    public List<IngestionJob> getAllJobs() {
        return new ArrayList<>(jobs.values());
    }

    public List<IngestionJob> getJobsByStatus(IngestionJob.JobStatus status) {
        return jobs.values().stream()
                .filter(job -> job.getStatus() == status)
                .toList();
    }

    public void updateJobStatus(UUID jobId, IngestionJob.JobStatus status) {
        IngestionJob job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(status);
            if (status == IngestionJob.JobStatus.PROCESSING) {
                job.setStartedAt(LocalDateTime.now());
            } else if (status == IngestionJob.JobStatus.COMPLETED || 
                       status == IngestionJob.JobStatus.FAILED) {
                job.setCompletedAt(LocalDateTime.now());
            }
            log.debug("Job {} status updated to {}", jobId, status);
        }
    }

    public void updateJobProgress(UUID jobId, int processedItems, int totalItems) {
        IngestionJob job = jobs.get(jobId);
        if (job != null) {
            job.setProcessedItems(processedItems);
            job.setTotalItems(totalItems);
            if (totalItems > 0) {
                job.setProgressPercentage((double) processedItems / totalItems * 100);
            }
        }
    }

    public void failJob(UUID jobId, String errorMessage) {
        IngestionJob job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(IngestionJob.JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(LocalDateTime.now());
            log.error("Job {} failed: {}", jobId, errorMessage);
        }
    }

    public boolean cancelJob(UUID jobId) {
        IngestionJob job = jobs.get(jobId);
        if (job != null && job.getStatus() == IngestionJob.JobStatus.PENDING) {
            job.setStatus(IngestionJob.JobStatus.CANCELLED);
            job.setCompletedAt(LocalDateTime.now());
            jobQueue.remove(job);
            log.info("Job {} cancelled", jobId);
            return true;
        }
        return false;
    }

    private void processJobs() {
        while (running) {
            try {
                IngestionJob job = jobQueue.poll(1, TimeUnit.SECONDS);
                if (job != null) {
                    processJob(job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing job", e);
            }
        }
    }

    private void processJob(IngestionJob job) {
        try {
            log.info("Processing job: {} for file: {}", job.getJobId(), job.getFileName());
            updateJobStatus(job.getJobId(), IngestionJob.JobStatus.PROCESSING);
            
            IngestionResult result = switch (job.getJobType()) {
                case FILE_UPLOAD -> {
                    updateJobStatus(job.getJobId(), IngestionJob.JobStatus.COMPLETED);
                    yield null;
                }
                case CSV_IMPORT -> processWithService("CSV import", job,
                    csvIngestionService::processCsvFile);
                case JSON_IMPORT -> processWithService("JSON import", job,
                    jsonIngestionService::processJsonFile);
                case PDF_EXTRACTION -> processWithService("PDF extraction", job,
                    pdfIngestionService::processPdfFile);
                case MARKDOWN_PARSE -> processWithService("Markdown parse", job,
                    markdownIngestionService::processMarkdownFile);
                default -> {
                    log.warn("Unknown job type: {}", job.getJobType());
                    updateJobStatus(job.getJobId(), IngestionJob.JobStatus.COMPLETED);
                    yield null;
                }
            };
            
            // Handle result if present
            if (result != null) {
                if (result.isSuccess()) {
                    updateJobStatus(job.getJobId(), IngestionJob.JobStatus.COMPLETED);
                    job.getMetadata().put("result", result);
                } else {
                    failJob(job.getJobId(), result.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing job: " + job.getJobId(), e);
            failJob(job.getJobId(), e.getMessage());
        }
    }
    
    @FunctionalInterface
    private interface IngestionProcessor {
        IngestionResult process(String filePath, UUID jobId);
    }
    
    private IngestionResult processWithService(String operationName, IngestionJob job, 
                                              IngestionProcessor processor) {
        log.info("Processing {}: {}", operationName, job.getFileName());
        return processor.process(job.getFilePath(), job.getJobId());
    }

    public void resubmitJob(UUID jobId) {
        IngestionJob job = jobs.get(jobId);
        if (job != null && 
            (job.getStatus() == IngestionJob.JobStatus.FAILED || 
             job.getStatus() == IngestionJob.JobStatus.CANCELLED)) {
            
            job.setStatus(IngestionJob.JobStatus.PENDING);
            job.setRetryCount(job.getRetryCount() + 1);
            job.setErrorMessage(null);
            job.setStartedAt(null);
            job.setCompletedAt(null);
            job.setCreatedAt(LocalDateTime.now());
            
            jobQueue.offer(job);
            log.info("Resubmitted job: {}", jobId);
        }
    }

    public int getQueueSize() {
        return jobQueue.size();
    }

    public Map<IngestionJob.JobStatus, Long> getJobStatistics() {
        Map<IngestionJob.JobStatus, Long> stats = new HashMap<>();
        for (IngestionJob.JobStatus status : IngestionJob.JobStatus.values()) {
            stats.put(status, jobs.values().stream()
                    .filter(job -> job.getStatus() == status)
                    .count());
        }
        return stats;
    }
}