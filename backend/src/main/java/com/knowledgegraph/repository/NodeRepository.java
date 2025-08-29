package com.knowledgegraph.repository;

import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NodeRepository extends JpaRepository<Node, UUID> {

    // Basic finders - use Pageable for all queries to avoid duplication
    Page<Node> findByType(NodeType type, Pageable pageable);
    Page<Node> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // Simple utility methods
    List<Node> findBySourceUri(String sourceUri);
    
    @Query(value = "SELECT EXISTS(SELECT 1 FROM kg.nodes WHERE name = :name AND type = :type)", nativeQuery = true)
    boolean existsByNameAndType(@Param("name") String name, @Param("type") String type);
    
    default boolean existsByNameAndType(String name, NodeType type) {
        return existsByNameAndType(name, type.name());
    }
    
    @Query(value = "SELECT * FROM kg.nodes WHERE name = :name AND type = :type", nativeQuery = true)
    List<Node> findByNameAndType(@Param("name") String name, @Param("type") String type);
    
    default List<Node> findByNameAndType(String name, NodeType type) {
        return findByNameAndType(name, type.name());
    }
    
    // Simplified search - let the service layer handle pagination logic
    @Query(value = """
        SELECT n.* FROM kg.nodes n 
        WHERE (:query IS NULL OR n.search_vector @@ plainto_tsquery('english', :query))
          AND (:type IS NULL OR n.type = :type)
        ORDER BY ts_rank(n.search_vector, plainto_tsquery('english', :query)) DESC NULLS LAST
        """, nativeQuery = true)
    Page<Node> search(@Param("query") String query, @Param("type") String type, Pageable pageable);

    // Basic connected nodes query - keep complex graph traversal for later when needed
    @Query("""
        SELECT DISTINCT n FROM Node n 
        WHERE n.id IN (
            SELECT e.target.id FROM Edge e WHERE e.source.id = :nodeId
            UNION 
            SELECT e.source.id FROM Edge e WHERE e.target.id = :nodeId
        )
        """)
    List<Node> findConnectedNodes(@Param("nodeId") UUID nodeId);
}