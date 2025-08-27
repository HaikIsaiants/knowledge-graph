-- Migration V3: Enhanced Full-Text Search Support
-- This migration adds search capabilities and indexes for efficient text search

-- Add search_vector to documents table if not exists
ALTER TABLE kg.documents 
ADD COLUMN IF NOT EXISTS search_vector tsvector 
GENERATED ALWAYS AS (
    to_tsvector('english', 
        COALESCE(content, '') || ' ' || 
        COALESCE(metadata::text, '')
    )
) STORED;

-- Create indexes for full-text search if they don't exist
CREATE INDEX IF NOT EXISTS idx_documents_search_vector 
ON kg.documents USING GIN(search_vector);

-- Ensure nodes search_vector index exists
CREATE INDEX IF NOT EXISTS idx_nodes_search_vector 
ON kg.nodes USING GIN(search_vector);

-- Create function to update search_vector for nodes when properties change
CREATE OR REPLACE FUNCTION update_node_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    -- Update search_vector based on name and properties
    NEW.search_vector := to_tsvector('english', 
        COALESCE(NEW.name, '') || ' ' || 
        COALESCE(NEW.properties::text, '')
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger for updating search_vector on node changes
DROP TRIGGER IF EXISTS update_node_search_vector_trigger ON kg.nodes;
CREATE TRIGGER update_node_search_vector_trigger
BEFORE INSERT OR UPDATE OF name, properties ON kg.nodes
FOR EACH ROW
EXECUTE FUNCTION update_node_search_vector();

-- Add indexes for efficient graph traversal
CREATE INDEX IF NOT EXISTS idx_edges_source_type 
ON kg.edges(source_id, type);

CREATE INDEX IF NOT EXISTS idx_edges_target_type 
ON kg.edges(target_id, type);

-- Add composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_nodes_type_created 
ON kg.nodes(type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_nodes_source_uri 
ON kg.nodes(source_uri) WHERE source_uri IS NOT NULL;

-- Add index for embeddings vector similarity (if pgvector is installed)
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        -- Convert float4[] to vector type and add index
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_embeddings_vector 
                 ON kg.embeddings USING ivfflat (vector::vector(384)) 
                 WITH (lists = 100)';
    END IF;
END $$;

-- Create materialized view for node statistics (refresh periodically)
CREATE MATERIALIZED VIEW IF NOT EXISTS kg.node_statistics AS
SELECT 
    type,
    COUNT(*) as count,
    MAX(created_at) as latest_created,
    MIN(created_at) as earliest_created
FROM kg.nodes
GROUP BY type;

CREATE INDEX IF NOT EXISTS idx_node_statistics_type 
ON kg.node_statistics(type);

-- Create function for ts_headline highlighting
CREATE OR REPLACE FUNCTION search_with_highlight(
    query_text TEXT,
    content TEXT,
    max_words INT DEFAULT 35,
    min_words INT DEFAULT 15
)
RETURNS TEXT AS $$
BEGIN
    RETURN ts_headline('english', 
        content, 
        plainto_tsquery('english', query_text),
        'MaxWords=' || max_words || ', MinWords=' || min_words || 
        ', StartSel=<mark>, StopSel=</mark>, HighlightAll=false'
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Refresh node statistics
REFRESH MATERIALIZED VIEW kg.node_statistics;

-- Add comment for documentation
COMMENT ON MATERIALIZED VIEW kg.node_statistics IS 
'Cached statistics for nodes by type. Refresh periodically using: REFRESH MATERIALIZED VIEW kg.node_statistics;';