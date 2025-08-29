package com.knowledgegraph.controller;

import com.knowledgegraph.dto.SearchResponseDTO;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.service.HybridSearchService;
import com.knowledgegraph.service.SearchService;
import com.knowledgegraph.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Search", description = "Search and discovery endpoints")
public class SearchController {
    
    private final SearchService searchService;
    private final VectorSearchService vectorSearchService;
    private final HybridSearchService hybridSearchService;
    
    @GetMapping
    @Operation(summary = "Full-text search", 
               description = "Search nodes and documents using full-text search")
    public ResponseEntity<SearchResponseDTO> search(
            @RequestParam @NotBlank(message = "Search query cannot be empty") String q,
            @RequestParam(required = false) NodeType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "true") boolean highlight) {
        
        log.info("Search request: query='{}', type={}, page={}, size={}", q, type, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "score"));
        
        return ResponseEntity.ok(
            highlight 
                ? searchService.searchWithHighlight(q, pageable)
                : searchService.searchNodes(q, type, pageable)
        );
    }
    
    @GetMapping("/vector")
    @Operation(summary = "Vector similarity search", 
               description = "Find similar content using vector embeddings")
    public ResponseEntity<SearchResponseDTO> vectorSearch(
            @RequestParam @NotBlank(message = "Search query cannot be empty") String q,
            @RequestParam(required = false) Double threshold,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Vector search: query='{}', threshold={}, limit={}", q, threshold, limit);
        return ResponseEntity.ok(vectorSearchService.findSimilar(q, threshold, limit));
    }
    
    @GetMapping("/hybrid")
    @Operation(summary = "Hybrid search", 
               description = "Combine full-text and vector search with configurable weights")
    public ResponseEntity<SearchResponseDTO> hybridSearch(
            @Parameter(description = "Search query") 
            @RequestParam @NotBlank(message = "Search query cannot be empty") String q,
            
            @Parameter(description = "FTS weight (0.0-1.0)") 
            @RequestParam(required = false) Double ftsWeight,
            
            @Parameter(description = "Vector weight (0.0-1.0)") 
            @RequestParam(required = false) Double vectorWeight,
            
            @Parameter(description = "Page number") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Hybrid search: query='{}', weights=[fts={}, vector={}]", 
                 q, ftsWeight, vectorWeight);
        
        Pageable pageable = PageRequest.of(page, size);
        
        SearchResponseDTO response = hybridSearchService.hybridSearch(
            q, ftsWeight, vectorWeight, pageable
        );
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/adaptive")
    @Operation(summary = "Adaptive hybrid search", 
               description = "Automatically adjust search weights based on result quality")
    public ResponseEntity<SearchResponseDTO> adaptiveSearch(
            @Parameter(description = "Search query") 
            @RequestParam @NotBlank(message = "Search query cannot be empty") String q,
            
            @Parameter(description = "Page number") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Adaptive search: query='{}'", q);
        
        Pageable pageable = PageRequest.of(page, size);
        
        SearchResponseDTO response = hybridSearchService.adaptiveHybridSearch(q, pageable);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/similar/{nodeId}")
    @Operation(summary = "Find similar nodes", 
               description = "Find nodes similar to a given node using vector similarity")
    public ResponseEntity<SearchResponseDTO> findSimilar(
            @PathVariable UUID nodeId,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Find similar to node: {}, limit={}", nodeId, limit);
        return ResponseEntity.ok(vectorSearchService.findSimilarNodes(nodeId, limit));
    }
    
    @GetMapping("/suggest")
    @Operation(summary = "Get search suggestions", 
               description = "Get suggested queries based on the input")
    public ResponseEntity<String[]> getSuggestions(@RequestParam @NotBlank(message = "Search query cannot be empty") String q) {
        log.debug("Getting suggestions for: '{}'", q);
        return ResponseEntity.ok(searchService.getSuggestedQueries(q));
    }
    
    @GetMapping("/documents")
    @Operation(summary = "Search documents", 
               description = "Search within document content")
    public ResponseEntity<SearchResponseDTO> searchDocuments(
            @Parameter(description = "Search query") 
            @RequestParam @NotBlank(message = "Search query cannot be empty") String q,
            
            @Parameter(description = "Page number") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Document search: query='{}', page={}, size={}", q, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        SearchResponseDTO response = searchService.searchDocuments(q, pageable);
        
        return ResponseEntity.ok(response);
    }
}