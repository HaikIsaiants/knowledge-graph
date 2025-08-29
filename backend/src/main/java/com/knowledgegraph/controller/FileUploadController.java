package com.knowledgegraph.controller;

import com.knowledgegraph.dto.FileUploadResponse;
import com.knowledgegraph.dto.IngestionJob;
import com.knowledgegraph.service.FileStorageService;
import com.knowledgegraph.service.IngestionJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final IngestionJobService ingestionJobService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestPart("file") MultipartFile file) {
        
        log.info("Received file upload request: {}", file.getOriginalFilename());
        
        try {
            // Store the file
            String storedFilePath = fileStorageService.storeFile(file);
            
            // Determine job type based on file extension
            String extension = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
            IngestionJob.JobType jobType = determineJobType(extension);
            
            // Create ingestion job
            IngestionJob job = ingestionJobService.createJob(
                jobType,
                file.getOriginalFilename(),
                storedFilePath,
                file.getContentType(),
                file.getSize()
            );
            
            // Build response
            FileUploadResponse response = FileUploadResponse.builder()
                    .fileId(UUID.randomUUID())
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .status("UPLOADED")
                    .uploadedAt(LocalDateTime.now())
                    .message("File uploaded successfully and queued for processing")
                    .storagePath(storedFilePath)
                    .jobId(job.getJobId())
                    .build();
            
            log.info("File uploaded successfully: {} with job: {}", 
                     file.getOriginalFilename(), job.getJobId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                FileUploadResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .status("FAILED")
                    .message(e.getMessage())
                    .uploadedAt(LocalDateTime.now())
                    .build()
            );
        } catch (IOException e) {
            log.error("Error storing file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                FileUploadResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .status("ERROR")
                    .message("Failed to store file: " + e.getMessage())
                    .uploadedAt(LocalDateTime.now())
                    .build()
            );
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<List<FileUploadResponse>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files) {
        
        log.info("Received multiple file upload request: {} files", files.length);
        
        return ResponseEntity.ok(
            java.util.Arrays.stream(files)
                .map(this::uploadFile)
                .map(ResponseEntity::getBody)
                .toList()
        );
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<IngestionJob>> getAllJobs() {
        return ResponseEntity.ok(ingestionJobService.getAllJobs());
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<IngestionJob> getJob(@PathVariable UUID jobId) {
        IngestionJob job = ingestionJobService.getJob(jobId);
        if (job != null) {
            return ResponseEntity.ok(job);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/jobs/status/{status}")
    public ResponseEntity<List<IngestionJob>> getJobsByStatus(
            @PathVariable IngestionJob.JobStatus status) {
        return ResponseEntity.ok(ingestionJobService.getJobsByStatus(status));
    }

    @PostMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable UUID jobId) {
        boolean cancelled = ingestionJobService.cancelJob(jobId);
        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "cancelled", cancelled,
            "message", cancelled ? "Job cancelled successfully" : "Job cannot be cancelled"
        ));
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ResponseEntity<Map<String, Object>> retryJob(@PathVariable UUID jobId) {
        ingestionJobService.resubmitJob(jobId);
        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "message", "Job resubmitted for processing"
        ));
    }

    @GetMapping("/jobs/stats")
    public ResponseEntity<Map<String, Object>> getJobStatistics() {
        return ResponseEntity.ok(Map.of(
            "queueSize", ingestionJobService.getQueueSize(),
            "statusCounts", ingestionJobService.getJobStatistics()
        ));
    }

    private IngestionJob.JobType determineJobType(String extension) {
        return switch (extension) {
            case "csv" -> IngestionJob.JobType.CSV_IMPORT;
            case "json" -> IngestionJob.JobType.JSON_IMPORT;
            case "pdf" -> IngestionJob.JobType.PDF_EXTRACTION;
            case "md", "markdown" -> IngestionJob.JobType.MARKDOWN_PARSE;
            case "html", "htm" -> IngestionJob.JobType.WEB_SCRAPE;
            default -> IngestionJob.JobType.FILE_UPLOAD;
        };
    }
}