-- Install PostgreSQL extensions for Knowledge Graph

-- Install pgvector for vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- Install Apache AGE for graph queries (if available)
-- Note: AGE requires compilation and may not be available in standard PostgreSQL Docker images
-- For now, we'll use native PostgreSQL with planned AGE integration later
-- CREATE EXTENSION IF NOT EXISTS age;

-- Install other useful extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Convert float4[] to vector type for embeddings table
-- This will be done after pgvector is installed
ALTER TABLE kg.embeddings ALTER COLUMN vector TYPE vector(768);

-- Create vector similarity index
CREATE INDEX IF NOT EXISTS idx_embeddings_vector ON kg.embeddings 
    USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);

-- Create additional full-text search configurations
CREATE TEXT SEARCH CONFIGURATION IF NOT EXISTS kg_english (COPY = english);

-- Optional: Create functions for vector similarity search
CREATE OR REPLACE FUNCTION kg.vector_similarity_search(
    query_vector vector(768),
    limit_count integer DEFAULT 10,
    similarity_threshold float DEFAULT 0.7
) RETURNS TABLE(
    id UUID,
    similarity float,
    content_snippet TEXT,
    node_id UUID,
    document_id UUID
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        e.id,
        1 - (e.vector <=> query_vector) as similarity,
        e.content_snippet,
        e.node_id,
        e.document_id
    FROM kg.embeddings e
    WHERE (1 - (e.vector <=> query_vector)) >= similarity_threshold
    ORDER BY e.vector <=> query_vector
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;