package com.knowledgegraph.repository;

import com.knowledgegraph.model.Embedding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmbeddingRepository extends JpaRepository<Embedding, UUID> {

    // Find embeddings by node
    List<Embedding> findByNode_Id(UUID nodeId);
    Page<Embedding> findByNode_Id(UUID nodeId, Pageable pageable);

    // Find embeddings by document
    List<Embedding> findByDocument_Id(UUID documentId);
    Page<Embedding> findByDocument_Id(UUID documentId, Pageable pageable);

    // Find embeddings by model version
    List<Embedding> findByModelVersion(String modelVersion);

    // Delete embeddings by node ID
    void deleteByNode_Id(UUID nodeId);

    // Delete embeddings by document ID
    void deleteByDocument_Id(UUID documentId);

    // Count embeddings by model version
    long countByModelVersion(String modelVersion);

    // Vector similarity search using native query
    // Note: This will work once pgvector is properly configured
    @Query(value = """
        SELECT e.id, e.node_id, e.document_id, e.content_snippet, 
               e.model_version, e.created_at,
               (1 - (e.vector <=> CAST(:queryVector AS vector))) as similarity
        FROM kg.embeddings e
        WHERE (1 - (e.vector <=> CAST(:queryVector AS vector))) >= :threshold
        ORDER BY e.vector <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarEmbeddings(
        @Param("queryVector") String queryVector,
        @Param("threshold") double threshold,
        @Param("limit") int limit
    );

    // Find embeddings for a specific node with similarity search
    @Query(value = """
        SELECT e.id, e.node_id, e.document_id, e.content_snippet, 
               e.model_version, e.created_at,
               (1 - (e.vector <=> CAST(:queryVector AS vector))) as similarity
        FROM kg.embeddings e
        WHERE e.node_id = :nodeId
          AND (1 - (e.vector <=> CAST(:queryVector AS vector))) >= :threshold
        ORDER BY e.vector <=> CAST(:queryVector AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findSimilarEmbeddingsForNode(
        @Param("nodeId") UUID nodeId,
        @Param("queryVector") String queryVector,
        @Param("threshold") double threshold,
        @Param("limit") int limit
    );

    // Search in content snippets using full-text search
    @Query(value = """
        SELECT e.* FROM kg.embeddings e
        WHERE to_tsvector('english', e.content_snippet) @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(to_tsvector('english', e.content_snippet), plainto_tsquery('english', :query)) DESC
        """, nativeQuery = true)
    List<Embedding> fullTextSearchSnippets(@Param("query") String query);

    // Find embeddings without vectors (for processing)
    @Query("""
        SELECT e FROM Embedding e 
        WHERE e.vector IS NULL
        ORDER BY e.createdAt ASC
        """)
    List<Embedding> findEmbeddingsWithoutVectors();

    // Batch vector similarity search with diverse results
    @Query(value = """
        WITH diverse_results AS (
            SELECT e.*, 
                   (1 - (e.vector <=> CAST(:queryVector AS vector))) as similarity,
                   ROW_NUMBER() OVER (PARTITION BY e.node_id ORDER BY e.vector <=> CAST(:queryVector AS vector)) as rn
            FROM kg.embeddings e
            WHERE (1 - (e.vector <=> CAST(:queryVector AS vector))) >= :threshold
        )
        SELECT id, node_id, document_id, content_snippet, model_version, created_at, similarity
        FROM diverse_results 
        WHERE rn = 1
        ORDER BY similarity DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findDiverseSimilarEmbeddings(
        @Param("queryVector") String queryVector,
        @Param("threshold") double threshold,
        @Param("limit") int limit
    );

    // Get embedding statistics
    @Query(value = """
        SELECT 
            COUNT(*) as total_embeddings,
            COUNT(CASE WHEN node_id IS NOT NULL THEN 1 END) as node_embeddings,
            COUNT(CASE WHEN document_id IS NOT NULL THEN 1 END) as document_embeddings,
            COUNT(CASE WHEN vector IS NOT NULL THEN 1 END) as embeddings_with_vectors,
            COUNT(DISTINCT model_version) as unique_models
        FROM kg.embeddings
        """, nativeQuery = true)
    Object getEmbeddingStatistics();

    // Find recent embeddings
    List<Embedding> findTop20ByOrderByCreatedAtDesc();
}