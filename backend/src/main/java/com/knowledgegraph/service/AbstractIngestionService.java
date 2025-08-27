package com.knowledgegraph.service;

import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.repository.DocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Base class for all ingestion services to reduce duplicate code
 */
@Slf4j
public abstract class AbstractIngestionService {
    
    @Autowired
    protected DocumentRepository documentRepository;
    
    /**
     * Creates a standard document record for any file type
     */
    protected Document createDocumentRecord(String filePath, String contentType, String ingestionType) {
        Document document = new Document();
        document.setUri("file://" + filePath);
        document.setContentType(contentType);
        document.setLastModified(LocalDateTime.now());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filePath", filePath);
        metadata.put("ingestionType", ingestionType);
        metadata.put("ingestedAt", LocalDateTime.now().toString());
        document.setMetadata(metadata);
        
        return documentRepository.save(document);
    }
    
    /**
     * Builds a standard error result
     */
    protected IngestionResult buildErrorResult(UUID jobId, String message, String errorType) {
        return IngestionResult.builder()
                .jobId(jobId)
                .processedAt(LocalDateTime.now())
                .success(false)
                .message(message)
                .totalRecords(0)
                .successCount(0)
                .errorCount(1)
                .errors(List.of(IngestionResult.ProcessingError.builder()
                        .errorMessage(message)
                        .errorType(errorType)
                        .build()))
                .build();
    }
    
    /**
     * Builds a success result
     */
    protected IngestionResult buildSuccessResult(UUID jobId, String message, int totalRecords, 
                                                 int successCount, int errorCount,
                                                 List<UUID> createdNodeIds, 
                                                 List<UUID> createdEdgeIds,
                                                 List<UUID> createdDocumentIds,
                                                 List<IngestionResult.ProcessingError> errors) {
        return IngestionResult.builder()
                .jobId(jobId)
                .processedAt(LocalDateTime.now())
                .success(errorCount == 0)
                .message(message)
                .totalRecords(totalRecords)
                .successCount(successCount)
                .errorCount(errorCount)
                .createdNodeIds(createdNodeIds)
                .createdEdgeIds(createdEdgeIds != null ? createdEdgeIds : new ArrayList<>())
                .createdDocumentIds(createdDocumentIds)
                .errors(errors)
                .build();
    }
}