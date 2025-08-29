-- Complete database setup script for Knowledge Graph
-- Run with: psql -h 127.0.0.1 -p 5433 -U postgres -f setup-database.sql

-- Create database if not exists
SELECT 'CREATE DATABASE knowledge_graph'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'knowledge_graph')\gexec

-- Connect to the database
\c knowledge_graph

-- Create schema
CREATE SCHEMA IF NOT EXISTS kg;

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- Drop old ENUM types if they exist and convert to VARCHAR
DO $$ BEGIN
    -- Convert node_type enum to VARCHAR if it exists
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'node_type' AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'kg')) THEN
        ALTER TABLE IF EXISTS kg.nodes ALTER COLUMN type TYPE VARCHAR(50);
        DROP TYPE IF EXISTS kg.node_type CASCADE;
    END IF;
    
    -- Keep edge_type as enum for now (not causing issues)
    CREATE TYPE kg.edge_type AS ENUM ('AFFILIATED_WITH', 'PARTICIPATED_IN', 'LOCATED_IN', 'PART_OF', 'REFERENCES', 'PRODUCED_BY', 'SIMILAR_TO');
EXCEPTION
    WHEN duplicate_object THEN null;
END $$;

-- Create documents table
CREATE TABLE IF NOT EXISTS kg.documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(500) NOT NULL,
    source_uri TEXT,
    content_hash VARCHAR(64),
    file_type VARCHAR(50),
    metadata JSONB,
    raw_content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create nodes table (with VARCHAR type instead of enum)
CREATE TABLE IF NOT EXISTS kg.nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type VARCHAR(50) NOT NULL,  -- Using VARCHAR instead of enum for compatibility
    title VARCHAR(500) NOT NULL,
    content TEXT,
    metadata JSONB,
    document_id UUID REFERENCES kg.documents(id) ON DELETE CASCADE,
    parent_node_id UUID REFERENCES kg.nodes(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create edges table
CREATE TABLE IF NOT EXISTS kg.edges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_id UUID NOT NULL REFERENCES kg.nodes(id) ON DELETE CASCADE,
    target_id UUID NOT NULL REFERENCES kg.nodes(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    weight FLOAT DEFAULT 1.0,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(source_id, target_id, type)
);

-- Create embeddings table with pgvector
CREATE TABLE IF NOT EXISTS kg.embeddings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    node_id UUID REFERENCES kg.nodes(id) ON DELETE CASCADE,
    document_id UUID REFERENCES kg.documents(id) ON DELETE CASCADE,
    content_snippet TEXT,
    vector vector(1536),
    model_version VARCHAR(50) DEFAULT 'text-embedding-ada-002',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON kg.documents(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_documents_file_type ON kg.documents(file_type);

CREATE INDEX IF NOT EXISTS idx_nodes_document_id ON kg.nodes(document_id);
CREATE INDEX IF NOT EXISTS idx_nodes_parent_id ON kg.nodes(parent_node_id);
CREATE INDEX IF NOT EXISTS idx_nodes_type ON kg.nodes(type);
CREATE INDEX IF NOT EXISTS idx_nodes_created_at ON kg.nodes(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_edges_source_id ON kg.edges(source_id);
CREATE INDEX IF NOT EXISTS idx_edges_target_id ON kg.edges(target_id);
CREATE INDEX IF NOT EXISTS idx_edges_type ON kg.edges(type);

CREATE INDEX IF NOT EXISTS idx_embeddings_node_id ON kg.embeddings(node_id);
CREATE INDEX IF NOT EXISTS idx_embeddings_document_id ON kg.embeddings(document_id);

-- Full text search indexes
CREATE INDEX IF NOT EXISTS idx_nodes_title_gin ON kg.nodes USING gin(to_tsvector('english', title));
CREATE INDEX IF NOT EXISTS idx_nodes_content_gin ON kg.nodes USING gin(to_tsvector('english', content));
CREATE INDEX IF NOT EXISTS idx_documents_title_gin ON kg.documents USING gin(to_tsvector('english', title));

-- Vector similarity search index
CREATE INDEX IF NOT EXISTS idx_embeddings_vector_cosine ON kg.embeddings 
    USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);

-- Create search function with highlighting
CREATE OR REPLACE FUNCTION kg.search_with_highlight(
    search_query text,
    highlight_tag text DEFAULT 'mark'
)
RETURNS TABLE (
    node_id uuid,
    snippet text,
    highlighted_snippet text,
    rank real
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        n.id as node_id,
        LEFT(n.content, 200) as snippet,
        ts_headline('english', n.content, websearch_to_tsquery('english', search_query), 
            'StartSel=<' || highlight_tag || '>, StopSel=</' || highlight_tag || '>, MaxWords=35, MinWords=15') as highlighted_snippet,
        ts_rank(to_tsvector('english', n.content), websearch_to_tsquery('english', search_query)) as rank
    FROM kg.nodes n
    WHERE to_tsvector('english', n.content) @@ websearch_to_tsquery('english', search_query)
    ORDER BY rank DESC;
END;
$$ LANGUAGE plpgsql;

-- Create vector similarity search function
CREATE OR REPLACE FUNCTION kg.find_similar_vectors(
    query_vector vector,
    limit_count int DEFAULT 10,
    threshold float DEFAULT 0.0
)
RETURNS TABLE (
    embedding_id uuid,
    node_id uuid,
    document_id uuid,
    content_snippet text,
    similarity float
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        e.id as embedding_id,
        e.node_id,
        e.document_id,
        e.content_snippet,
        1 - (e.vector <=> query_vector) as similarity
    FROM kg.embeddings e
    WHERE 1 - (e.vector <=> query_vector) > threshold
    ORDER BY e.vector <=> query_vector
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- Grant permissions
GRANT ALL ON SCHEMA kg TO postgres;
GRANT ALL ON ALL TABLES IN SCHEMA kg TO postgres;
GRANT ALL ON ALL SEQUENCES IN SCHEMA kg TO postgres;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA kg TO postgres;

-- Add comments for documentation
COMMENT ON SCHEMA kg IS 'Knowledge Graph schema containing documents, nodes, edges, and embeddings';
COMMENT ON TABLE kg.documents IS 'Source documents ingested into the knowledge graph';
COMMENT ON TABLE kg.nodes IS 'Knowledge nodes extracted from documents';
COMMENT ON TABLE kg.edges IS 'Relationships between nodes';
COMMENT ON TABLE kg.embeddings IS 'Vector embeddings for semantic search';
COMMENT ON FUNCTION kg.search_with_highlight IS 'Full-text search with result highlighting';
COMMENT ON FUNCTION kg.find_similar_vectors IS 'Vector similarity search using cosine distance';

-- Verify setup
SELECT 'Database setup complete!' as status;
SELECT table_name FROM information_schema.tables WHERE table_schema = 'kg' ORDER BY table_name;