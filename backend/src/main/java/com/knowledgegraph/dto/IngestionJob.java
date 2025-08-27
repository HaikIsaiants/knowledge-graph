package com.knowledgegraph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionJob {
    
    public enum JobStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public enum JobType {
        FILE_UPLOAD,
        CSV_IMPORT,
        JSON_IMPORT,
        PDF_EXTRACTION,
        MARKDOWN_PARSE,
        WEB_SCRAPE,
        TEXT_CHUNK,
        ENTITY_EXTRACT,
        EMBEDDING_GENERATE
    }
    
    private UUID jobId;
    private JobType jobType;
    private JobStatus status;
    private String fileName;
    private String filePath;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Map<String, Object> metadata;
    private String errorMessage;
    private Integer retryCount;
    private Integer totalItems;
    private Integer processedItems;
    private Double progressPercentage;
}