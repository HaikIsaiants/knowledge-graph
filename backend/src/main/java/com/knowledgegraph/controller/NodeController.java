package com.knowledgegraph.controller;

import com.knowledgegraph.dto.NodeDetailDTO;
import com.knowledgegraph.dto.SearchResultDTO;
import com.knowledgegraph.model.Edge;
import com.knowledgegraph.model.Embedding;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.EdgeRepository;
import com.knowledgegraph.repository.EmbeddingRepository;
import com.knowledgegraph.repository.NodeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/nodes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Nodes", description = "Node management and retrieval endpoints")
public class NodeController {
    
    private final NodeRepository nodeRepository;
    private final EdgeRepository edgeRepository;
    private final EmbeddingRepository embeddingRepository;
    
    @GetMapping("/{id}")
    @Operation(summary = "Get node details", 
               description = "Retrieve full details of a node including relationships and citations")
    public ResponseEntity<NodeDetailDTO> getNode(@PathVariable UUID id) {
        log.info("Getting node details for: {}", id);
        
        Node node = nodeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Node not found: " + id
            ));
        
        NodeDetailDTO details = buildNodeDetails(node);
        
        return ResponseEntity.ok(details);
    }
    
    @GetMapping
    @Operation(summary = "List nodes", 
               description = "Get paginated list of nodes with optional filtering")
    public ResponseEntity<Page<Node>> listNodes(
            @Parameter(description = "Filter by node type") 
            @RequestParam(required = false) NodeType type,
            
            @Parameter(description = "Filter by name (partial match)") 
            @RequestParam(required = false) String name,
            
            @Parameter(description = "Page number") 
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "20") int size,
            
            @Parameter(description = "Sort field") 
            @RequestParam(defaultValue = "createdAt") String sortBy,
            
            @Parameter(description = "Sort direction") 
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        log.info("Listing nodes: type={}, name={}, page={}, size={}", type, name, page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Node> nodes;
        if (type != null) {
            nodes = nodeRepository.findByType(type, pageable);
        } else if (name != null && !name.isEmpty()) {
            nodes = nodeRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            nodes = nodeRepository.findAll(pageable);
        }
        
        return ResponseEntity.ok(nodes);
    }
    
    @GetMapping("/{id}/citations")
    @Operation(summary = "Get node citations", 
               description = "Retrieve source citations and references for a node")
    public ResponseEntity<List<NodeDetailDTO.CitationDTO>> getNodeCitations(
            @PathVariable UUID id) {
        
        log.info("Getting citations for node: {}", id);
        
        Node node = nodeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Node not found: " + id
            ));
        
        List<NodeDetailDTO.CitationDTO> citations = buildCitations(node);
        
        return ResponseEntity.ok(citations);
    }
    
    @GetMapping("/{id}/related")
    @Operation(summary = "Get related nodes", 
               description = "Find nodes connected to this node")
    public ResponseEntity<List<SearchResultDTO>> getRelatedNodes(
            @PathVariable UUID id,
            
            @Parameter(description = "Maximum number of related nodes") 
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("Getting related nodes for: {}, limit={}", id, limit);
        
        Node node = nodeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Node not found: " + id
            ));
        
        List<Node> connectedNodes = nodeRepository.findConnectedNodes(id);
        
        List<SearchResultDTO> related = connectedNodes.stream()
            .limit(limit)
            .map(this::convertToSearchResult)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(related);
    }
    
    @GetMapping("/{id}/edges")
    @Operation(summary = "Get node edges", 
               description = "Get all edges connected to this node")
    public ResponseEntity<Map<String, List<NodeDetailDTO.EdgeDTO>>> getNodeEdges(
            @PathVariable UUID id) {
        
        log.info("Getting edges for node: {}", id);
        
        Node node = nodeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Node not found: " + id
            ));
        
        List<Edge> outgoingEdges = edgeRepository.findBySourceId(id);
        List<Edge> incomingEdges = edgeRepository.findByTargetId(id);
        
        Map<String, List<NodeDetailDTO.EdgeDTO>> edges = Map.of(
            "outgoing", convertEdges(outgoingEdges, "outgoing"),
            "incoming", convertEdges(incomingEdges, "incoming")
        );
        
        return ResponseEntity.ok(edges);
    }
    
    @PostMapping
    @Operation(summary = "Create a new node", 
               description = "Create a new node in the knowledge graph")
    public ResponseEntity<Node> createNode(@RequestBody Node node) {
        log.info("Creating new node: type={}, name={}", node.getType(), node.getName());
        
        if (node.getId() != null) {
            node.setId(null); // Let database generate ID
        }
        
        Node saved = nodeRepository.save(node);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update node", 
               description = "Update an existing node's properties")
    public ResponseEntity<Node> updateNode(
            @PathVariable UUID id,
            @RequestBody Node nodeUpdate) {
        
        log.info("Updating node: {}", id);
        
        Node existing = nodeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Node not found: " + id
            ));
        
        // Update fields using Optional to avoid null checks
        Optional.ofNullable(nodeUpdate.getName()).ifPresent(existing::setName);
        Optional.ofNullable(nodeUpdate.getType()).ifPresent(existing::setType);
        Optional.ofNullable(nodeUpdate.getProperties())
            .ifPresent(props -> existing.getProperties().putAll(props));
        
        return ResponseEntity.ok(nodeRepository.save(existing));
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete node", 
               description = "Delete a node and its relationships")
    public ResponseEntity<Void> deleteNode(@PathVariable UUID id) {
        log.info("Deleting node: {}", id);
        
        if (!nodeRepository.existsById(id)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Node not found: " + id
            );
        }
        
        nodeRepository.deleteById(id);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Build complete node details DTO
     */
    private NodeDetailDTO buildNodeDetails(Node node) {
        List<Edge> outgoingEdges = edgeRepository.findBySourceId(node.getId());
        List<Edge> incomingEdges = edgeRepository.findByTargetId(node.getId());
        
        return NodeDetailDTO.builder()
            .id(node.getId())
            .type(node.getType())
            .name(node.getName())
            .properties(node.getProperties())
            .sourceUri(node.getSourceUri())
            .capturedAt(node.getCapturedAt())
            .createdAt(node.getCreatedAt())
            .updatedAt(node.getUpdatedAt())
            .outgoingEdges(convertEdges(outgoingEdges, "outgoing"))
            .incomingEdges(convertEdges(incomingEdges, "incoming"))
            .citations(buildCitations(node))
            .embeddings(convertEmbeddings(embeddingRepository.findByNode_Id(node.getId())))
            .totalConnections(outgoingEdges.size() + incomingEdges.size())
            .connectionsByType(countConnectionsByType(outgoingEdges))
            .build();
    }
    
    /**
     * Count connections by type
     */
    private Map<String, Integer> countConnectionsByType(List<Edge> edges) {
        return edges.stream()
            .collect(Collectors.groupingBy(
                e -> e.getType().name(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }
    
    /**
     * Build citations for a node
     */
    private List<NodeDetailDTO.CitationDTO> buildCitations(Node node) {
        return Optional.ofNullable(node.getSourceUri())
            .map(uri -> List.of(
                NodeDetailDTO.CitationDTO.builder()
                    .documentUri(uri)
                    .extractedAt(node.getCapturedAt())
                    .build()
            ))
            .orElse(Collections.emptyList());
    }
    
    /**
     * Convert edges to DTOs
     */
    private List<NodeDetailDTO.EdgeDTO> convertEdges(List<Edge> edges, String direction) {
        boolean isOutgoing = "outgoing".equals(direction);
        
        return edges.stream()
            .map(edge -> createEdgeDTO(edge, isOutgoing ? edge.getTarget() : edge.getSource(), direction))
            .collect(Collectors.toList());
    }
    
    /**
     * Create edge DTO from edge and connected node
     */
    private NodeDetailDTO.EdgeDTO createEdgeDTO(Edge edge, Node otherNode, String direction) {
        return NodeDetailDTO.EdgeDTO.builder()
            .id(edge.getId())
            .nodeId(otherNode.getId())
            .nodeName(otherNode.getName())
            .nodeType(otherNode.getType())
            .edgeType(edge.getType().name())
            .properties(edge.getProperties())
            .direction(direction)
            .build();
    }
    
    /**
     * Convert embeddings to DTOs
     */
    private List<NodeDetailDTO.EmbeddingDTO> convertEmbeddings(List<Embedding> embeddings) {
        return embeddings.stream()
            .map(emb -> NodeDetailDTO.EmbeddingDTO.builder()
                .id(emb.getId())
                .contentSnippet(emb.getContentSnippet())
                .modelVersion(emb.getModelVersion())
                .createdAt(emb.getCreatedAt())
                .build())
            .collect(Collectors.toList());
    }
    
    /**
     * Convert node to search result
     */
    private SearchResultDTO convertToSearchResult(Node node) {
        return SearchResultDTO.builder()
            .id(node.getId())
            .type(node.getType())
            .title(node.getName())
            .sourceUri(node.getSourceUri())
            .metadata(node.getProperties())
            .createdAt(node.getCreatedAt())
            .updatedAt(node.getUpdatedAt())
            .build();
    }
}