package com.knowledgegraph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private UUID fileId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String status;
    private LocalDateTime uploadedAt;
    private String message;
    private String storagePath;
    private UUID jobId;
}