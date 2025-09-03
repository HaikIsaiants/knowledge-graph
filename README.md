# Knowledge Graph System

A knowledge graph application with search capabilities, graph visualization, and document ingestion. Built with Vue 3 and Spring Boot.

## Tech Stack

- **Frontend**: Vue 3, TypeScript, Vite, Tailwind CSS
- **Backend**: Spring Boot 3.x, Java 17, Spring Data JPA
- **Database**: PostgreSQL 15+ with pgvector extension
- **Graph Visualization**: Cytoscape.js

## Features

- Document ingestion (CSV, JSON, PDF, Markdown)
- Full-text and vector similarity search
- Graph traversal and visualization
- Node and relationship management
- Async job processing for file uploads

## Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Maven (included in tools/ directory)

### Database Setup

```bash
# Create database and schema
psql -U postgres -h localhost -f database/init.sql

# Or use the setup script (Windows)
setup-database.bat
```

### Backend

```bash
cd backend

# Run tests
../tools/maven/bin/mvn clean test

# Start server (port 8080)
../tools/maven/bin/mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Run tests
npm test

# Start dev server (port 3000)
npm run dev
```

## Project Structure

```
knowledge-graph/
├── backend/                 # Spring Boot API
│   ├── src/main/java/      # Application code
│   ├── src/test/           # Tests
│   └── pom.xml            
├── frontend/               # Vue 3 SPA
│   ├── src/
│   │   ├── components/    
│   │   ├── views/         
│   │   ├── router/        
│   │   └── api/          
│   └── package.json      
├── database/              # PostgreSQL setup
│   └── init.sql          
└── tools/                # Bundled Maven
```

## API Endpoints

### Health
- `GET /api/health` - System health check

### Ingestion
- `POST /api/ingest/upload` - Upload files (CSV, JSON, PDF, MD)
- `GET /api/ingest/jobs/{id}` - Check job status
- `GET /api/ingest/jobs` - List all jobs

### Search
- `GET /api/search` - Full-text search
- `GET /api/search/vector` - Vector similarity search
- `GET /api/search/hybrid` - Combined search
- `GET /api/search/adaptive` - Auto-adjusting hybrid search
- `GET /api/search/suggestions` - Search suggestions
- `GET /api/search/recent` - Recent searches
- `GET /api/search/popular` - Popular searches

### Nodes
- `GET /api/nodes` - List nodes with pagination
- `GET /api/nodes/{id}` - Get node details
- `POST /api/nodes` - Create node
- `PUT /api/nodes/{id}` - Update node
- `DELETE /api/nodes/{id}` - Delete node
- `GET /api/nodes/{id}/citations` - Get citations
- `GET /api/nodes/{id}/relationships` - Get relationships
- `GET /api/nodes/{id}/similar` - Find similar nodes

### Graph
- `GET /api/graph/neighborhood` - N-hop neighborhood
- `GET /api/graph/path` - Shortest path between nodes
- `GET /api/graph/connected-components` - Find components
- `GET /api/graph/centrality` - Calculate centrality
- `GET /api/graph/statistics` - Graph statistics
- `GET /api/graph/subgraph` - Extract subgraph

## Database Schema

### Tables
- `nodes` - Graph entities (Person, Organization, Document, etc.)
- `edges` - Relationships between nodes
- `documents` - Source documents
- `embeddings` - Vector representations (384 dimensions)

### Extensions
- `pgvector` - Vector similarity search
- `pg_trgm` - Fuzzy text matching
- Full-text search with English configuration

## Configuration

### Backend
- `application.yml` - Main configuration
- `application-local.yml` - Local environment settings
- Environment variables:
  - `DB_URL` - Database connection URL
  - `DB_USER` - Database username
  - `DB_PASSWORD` - Database password
  - `OPENAI_API_KEY` - Optional, for embeddings

### Frontend
- `vite.config.ts` - Build configuration
- API endpoint configured in `src/api/client.ts`

## Testing

### Backend
```bash
cd backend
mvn test
```

### Frontend
```bash
cd frontend
npm test
```

## License

Personal project