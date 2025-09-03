-- Clear all tables in the knowledge graph database
TRUNCATE TABLE kg.ingestion_jobs CASCADE;
TRUNCATE TABLE kg.embeddings CASCADE;
TRUNCATE TABLE kg.nodes CASCADE;
TRUNCATE TABLE kg.edges CASCADE;
TRUNCATE TABLE kg.documents CASCADE;