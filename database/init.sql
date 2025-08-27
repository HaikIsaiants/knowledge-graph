-- Knowledge Graph Database Initialization

-- Create the main database schema
CREATE SCHEMA IF NOT EXISTS kg;

-- Set search path to include our schema
SET search_path TO kg, public;

-- Create custom data types
CREATE TYPE node_type AS ENUM ('PERSON', 'ORGANIZATION', 'EVENT', 'PLACE', 'ITEM', 'CONCEPT', 'DOCUMENT');
CREATE TYPE edge_type AS ENUM ('AFFILIATED_WITH', 'PARTICIPATED_IN', 'LOCATED_IN', 'PART_OF', 'REFERENCES', 'PRODUCED_BY', 'SIMILAR_TO');

-- Nodes table for graph entities
CREATE TABLE IF NOT EXISTS nodes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type node_type NOT NULL,
    name VARCHAR(500) NOT NULL,
    properties JSONB DEFAULT '{}',
    source_uri TEXT,
    captured_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Full-text search
    search_vector tsvector GENERATED ALWAYS AS (
        to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(properties::text, ''))
    ) STORED
);

-- Edges table for relationships
CREATE TABLE IF NOT EXISTS edges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL,
    target_id UUID NOT NULL,
    type edge_type NOT NULL,
    properties JSONB DEFAULT '{}',
    source_uri TEXT,
    captured_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT fk_edge_source FOREIGN KEY (source_id) REFERENCES nodes(id) ON DELETE CASCADE,
    CONSTRAINT fk_edge_target FOREIGN KEY (target_id) REFERENCES nodes(id) ON DELETE CASCADE
);

-- Documents table for source tracking
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uri TEXT UNIQUE NOT NULL,
    content TEXT,
    content_type VARCHAR(100),
    metadata JSONB DEFAULT '{}',
    last_modified TIMESTAMPTZ,
    etag VARCHAR(200),
    content_hash VARCHAR(64),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    
    -- Full-text search for document content
    content_vector tsvector GENERATED ALWAYS AS (
        to_tsvector('english', COALESCE(content, ''))
    ) STORED
);

-- Embeddings table for vector similarity search
CREATE TABLE IF NOT EXISTS embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    node_id UUID,
    document_id UUID,
    content_snippet TEXT,
    vector float4[],  -- Will be converted to vector type after pgvector installation
    model_version VARCHAR(50) DEFAULT 'mock',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    
    CONSTRAINT fk_embedding_node FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE,
    CONSTRAINT fk_embedding_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT chk_embedding_ref CHECK ((node_id IS NOT NULL) OR (document_id IS NOT NULL))
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_nodes_type ON nodes(type);
CREATE INDEX IF NOT EXISTS idx_nodes_name ON nodes(name);
CREATE INDEX IF NOT EXISTS idx_nodes_search_vector ON nodes USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_nodes_properties ON nodes USING GIN(properties);
CREATE INDEX IF NOT EXISTS idx_nodes_created_at ON nodes(created_at);

CREATE INDEX IF NOT EXISTS idx_edges_source_id ON edges(source_id);
CREATE INDEX IF NOT EXISTS idx_edges_target_id ON edges(target_id);
CREATE INDEX IF NOT EXISTS idx_edges_type ON edges(type);
CREATE INDEX IF NOT EXISTS idx_edges_source_target ON edges(source_id, target_id);

CREATE INDEX IF NOT EXISTS idx_documents_uri ON documents(uri);
CREATE INDEX IF NOT EXISTS idx_documents_content_type ON documents(content_type);
CREATE INDEX IF NOT EXISTS idx_documents_content_vector ON documents USING GIN(content_vector);
CREATE INDEX IF NOT EXISTS idx_documents_last_modified ON documents(last_modified);

CREATE INDEX IF NOT EXISTS idx_embeddings_node_id ON embeddings(node_id);
CREATE INDEX IF NOT EXISTS idx_embeddings_document_id ON embeddings(document_id);

-- Create update triggers for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_nodes_updated_at BEFORE UPDATE ON nodes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_edges_updated_at BEFORE UPDATE ON edges
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_documents_updated_at BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert some sample data for testing
INSERT INTO nodes (type, name, properties, source_uri) VALUES
    ('PERSON', 'John Doe', '{"occupation": "Software Engineer", "location": "San Francisco"}', 'sample://person/1'),
    ('ORGANIZATION', 'Tech Corp', '{"industry": "Technology", "founded": 2020}', 'sample://org/1'),
    ('CONCEPT', 'Knowledge Graphs', '{"definition": "Graph databases for knowledge representation"}', 'sample://concept/1');

INSERT INTO edges (source_id, target_id, type, properties) VALUES
    ((SELECT id FROM nodes WHERE name = 'John Doe'), 
     (SELECT id FROM nodes WHERE name = 'Tech Corp'), 
     'AFFILIATED_WITH', 
     '{"role": "Senior Engineer", "start_date": "2021-01-01"}');

COMMIT;