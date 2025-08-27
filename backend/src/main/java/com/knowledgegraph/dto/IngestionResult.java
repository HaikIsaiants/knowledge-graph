package com.knowledgegraph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResult {
    private UUID jobId;
    private boolean success;
    private String message;
    private LocalDateTime processedAt;
    private Integer totalRecords;
    private Integer successCount;
    private Integer errorCount;
    private Integer skippedCount;
    
    @Builder.Default
    private List<UUID> createdNodeIds = new ArrayList<>();
    
    @Builder.Default
    private List<UUID> createdDocumentIds = new ArrayList<>();
    
    @Builder.Default
    private List<UUID> createdEdgeIds = new ArrayList<>();
    
    @Builder.Default
    private List<ProcessingError> errors = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingError {
        private Integer lineNumber;
        private String field;
        private String value;
        private String errorMessage;
        private String errorType;
    }
}