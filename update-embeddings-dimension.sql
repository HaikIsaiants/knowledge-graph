-- Update embeddings table to support OpenAI embedding dimensions
-- text-embedding-3-small produces 1536 dimensions

-- First, drop the old vector column if it exists
ALTER TABLE kg.embeddings DROP COLUMN IF EXISTS vector;

-- Add new vector column with 1536 dimensions
ALTER TABLE kg.embeddings ADD COLUMN vector vector(1536);

-- Update the model_name to indicate we're using OpenAI
UPDATE kg.embeddings SET model_name = 'text-embedding-3-small' WHERE model_name IS NULL;

-- Verify the changes
SELECT 
    column_name, 
    data_type,
    character_maximum_length
FROM information_schema.columns 
WHERE table_schema = 'kg' 
  AND table_name = 'embeddings'
  AND column_name = 'vector';

-- Show table structure
\d kg.embeddings