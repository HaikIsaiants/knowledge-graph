-- Convert enum columns to VARCHAR to fix JPA compatibility issues
-- This allows Spring Boot/Hibernate to properly handle the type mappings

-- Convert node_type enum to VARCHAR
ALTER TABLE kg.nodes 
ALTER COLUMN type TYPE VARCHAR(50) 
USING type::text;

-- Convert edge_type enum to VARCHAR
ALTER TABLE kg.edges 
ALTER COLUMN type TYPE VARCHAR(50) 
USING type::text;

-- Add check constraints to maintain data integrity
ALTER TABLE kg.nodes 
ADD CONSTRAINT check_node_type 
CHECK (type IN ('PERSON', 'ORGANIZATION', 'LOCATION', 'EVENT', 'CONCEPT', 
                'DOCUMENT', 'PROJECT', 'SYSTEM', 'PROCESS', 'POLICY'));

ALTER TABLE kg.edges 
ADD CONSTRAINT check_edge_type 
CHECK (type IN ('RELATED_TO', 'PART_OF', 'CONNECTED_TO', 'DERIVED_FROM', 
                'DEPENDS_ON', 'INFLUENCES', 'CONTAINS', 'REFERENCES', 
                'AUTHORED_BY', 'AFFILIATED_WITH', 'LOCATED_IN', 'OCCURRED_AT', 
                'PARTICIPATES_IN', 'MANAGES', 'OWNS', 'CREATED_BY', 
                'SIMILAR_TO', 'OPPOSITE_OF', 'PRECEDES', 'FOLLOWS'));

-- Drop the old enum types as they're no longer needed
DROP TYPE IF EXISTS kg.node_type CASCADE;
DROP TYPE IF EXISTS kg.edge_type CASCADE;