package com.knowledgegraph.repository;

import com.knowledgegraph.model.Edge;
import com.knowledgegraph.model.EdgeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EdgeRepository extends JpaRepository<Edge, UUID> {

    // Basic finders - use Pageable consistently
    Page<Edge> findBySource_Id(UUID sourceId, Pageable pageable);
    Page<Edge> findByTarget_Id(UUID targetId, Pageable pageable);
    Page<Edge> findByType(EdgeType type, Pageable pageable);
    
    // Simple utility methods
    List<Edge> findBySource_IdAndTarget_Id(UUID sourceId, UUID targetId);
    List<Edge> findBySourceUri(String sourceUri);
    boolean existsBySource_IdAndTarget_IdAndType(UUID sourceId, UUID targetId, EdgeType type);

    // Simplified connected edges query
    @Query("""
        SELECT e FROM Edge e 
        WHERE e.source.id = :nodeId OR e.target.id = :nodeId
        """)
    Page<Edge> findAllConnectedEdges(@Param("nodeId") UUID nodeId, Pageable pageable);
}