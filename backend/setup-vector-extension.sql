-- Setup script for PostgreSQL vector extension and schema
-- Run this before running tests or the application

-- Create the kg schema if it doesn't exist
CREATE SCHEMA IF NOT EXISTS kg;

-- Install the pgvector extension if not already installed
CREATE EXTENSION IF NOT EXISTS vector;

-- Grant usage on schema to postgres user
GRANT ALL ON SCHEMA kg TO postgres;

-- Note: The application will create tables automatically with Hibernate
-- This script just ensures the schema and extension are ready