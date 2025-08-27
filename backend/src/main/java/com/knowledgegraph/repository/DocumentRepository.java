package com.knowledgegraph.repository;

import com.knowledgegraph.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Find document by URI
    Optional<Document> findByUri(String uri);

    // Find documents by content type
    List<Document> findByContentType(String contentType);
    Page<Document> findByContentType(String contentType, Pageable pageable);

    // Find documents modified after a certain date
    List<Document> findByLastModifiedAfter(LocalDateTime date);

    // Find documents by ETag
    Optional<Document> findByEtag(String etag);

    // Find documents by content hash
    List<Document> findByContentHash(String contentHash);

    // Unified search method - combines content and URI search
    @Query(value = """
        SELECT d.* FROM kg.documents d 
        WHERE (:query IS NULL 
            OR LOWER(d.uri) LIKE LOWER(CONCAT('%', :query, '%'))
            OR d.content_vector @@ plainto_tsquery('english', :query))
          AND (:contentType IS NULL OR d.content_type = :contentType)
        ORDER BY 
            CASE WHEN LOWER(d.uri) LIKE LOWER(CONCAT('%', :query, '%')) THEN 1 ELSE 2 END,
            ts_rank(d.content_vector, plainto_tsquery('english', :query)) DESC NULLS LAST
        """, nativeQuery = true)
    Page<Document> search(@Param("query") String query, @Param("contentType") String contentType, Pageable pageable);

    // Simple utility methods
    boolean existsByUri(String uri);
}