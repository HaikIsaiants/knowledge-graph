-- Initialize test database with basic schema
-- This mirrors the main database initialization for testing

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS kg;

-- Create necessary extensions for testing
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Note: In tests we'll use simplified vector operations
-- Production vector extension setup would be different