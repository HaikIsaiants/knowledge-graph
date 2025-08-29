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
        LEFT(COALESCE(n.content, d.raw_content), 200) as content_snippet,
        1 - (e.vector <=> query_vector) as similarity
    FROM kg.embeddings e
    LEFT JOIN kg.nodes n ON e.node_id = n.id
    LEFT JOIN kg.documents d ON e.document_id = d.id
    WHERE 1 - (e.vector <=> query_vector) > threshold
    ORDER BY e.vector <=> query_vector
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;