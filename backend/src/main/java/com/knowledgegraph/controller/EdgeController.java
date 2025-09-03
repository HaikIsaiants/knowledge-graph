package com.knowledgegraph.controller;

import com.knowledgegraph.model.Edge;
import com.knowledgegraph.repository.EdgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/edges")
@RequiredArgsConstructor
@Slf4j
public class EdgeController {
    
    private final EdgeRepository edgeRepository;
    
    @GetMapping
    public ResponseEntity<Page<Edge>> getAllEdges(Pageable pageable) {
        log.info("Fetching all edges, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Edge> edges = edgeRepository.findAll(pageable);
        return ResponseEntity.ok(edges);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Edge> getEdge(@PathVariable UUID id) {
        return edgeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}