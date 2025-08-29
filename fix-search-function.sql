-- Drop the old function if it exists
DROP FUNCTION IF EXISTS kg.search_with_highlight(text, text);

-- Create search function that matches what Java expects
CREATE OR REPLACE FUNCTION kg.search_with_highlight(
    search_query text,
    content_to_highlight text
)
RETURNS text AS $$
BEGIN
    RETURN ts_headline('english', 
                       content_to_highlight, 
                       plainto_tsquery('english', search_query), 
                       'StartSel=<mark>, StopSel=</mark>, MaxWords=35, MinWords=15');
END;
$$ LANGUAGE plpgsql;