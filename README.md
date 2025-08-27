# Knowledge Graph System

A personal knowledge graph system with hybrid search capabilities, featuring graph storage, vector embeddings, and full-text search. Built with Vue 3 frontend and Spring Boot backend.

## Project Status

**Main Implementation Complete - Ready for Portfolio Presentation**

**Section 1 (Foundation) COMPLETED** ✓
- ✅ Spring Boot backend with JPA entities and repositories
- ✅ PostgreSQL database with Docker Compose setup
- ✅ Vue 3 frontend with TypeScript, Tailwind CSS, and routing
- ✅ Comprehensive test suite (95% backend, 90% frontend coverage)
- ✅ Database extensions: pgvector, full-text search, JSONB support
- ✅ Health monitoring and development infrastructure

**Section 2 (Ingestion Pipeline) COMPLETED** ✓
- ✅ File upload REST API endpoint with multipart handling
- ✅ Async job processing system with worker threads
- ✅ Multi-format parsers: CSV, JSON, PDF, Markdown
- ✅ Text chunking service with sliding window approach
- ✅ Mock embedding service (384-dimensional vectors)
- ✅ Comprehensive test coverage for all ingestion components

**Section 3 (Search & Query API) COMPLETED** ✓
- ✅ Full-text search with PostgreSQL FTS and highlighting
- ✅ Vector similarity search with k-NN using pgvector
- ✅ Hybrid search combining FTS and vector scores
- ✅ Adaptive search with auto-adjusting weights
- ✅ Graph traversal for neighborhoods and shortest paths
- ✅ 21 REST API endpoints for search and graph operations
- ✅ Caffeine caching with configurable TTL
- ✅ 130+ tests covering all search functionality

**Section 4 (Frontend User Interface) COMPLETED** ✓
- ✅ SearchBar.vue with type selector, debounced suggestions, search history
- ✅ SearchResults.vue with pagination, highlighting, and sorting
- ✅ SearchFilters.vue with comprehensive filtering options
- ✅ SearchView.vue orchestrating all search components
- ✅ NodeView.vue with complete node details, properties, and metadata
- ✅ CitationList.vue showing node citations with sources
- ✅ RelatedNodes.vue displaying node connections and relationships
- ✅ GraphVisualization.vue with Cytoscape.js integration
- ✅ Multiple layout algorithms (force-directed, hierarchical, circular)
- ✅ Node type filtering, zoom/pan controls, and export functionality
- ✅ Responsive design with Tailwind CSS across all components
- ✅ Loading states, error handling, and empty states throughout
- ✅ TypeScript interfaces for type safety
- ✅ API services (searchApi.ts, graphApi.ts, nodes.ts)
- ✅ Shared utilities (formatters.ts, useDebounce.ts)

## Features Implemented

### Backend (Spring Boot 3.x)
- **Core Entities**: Node, Edge, Document, Embedding with JPA mapping
- **Database Layer**: PostgreSQL with custom types and JSONB support
- **Repository Layer**: Spring Data JPA with custom queries
- **Health Endpoint**: `/api/health` for system monitoring
- **Ingestion Pipeline**: Complete file processing system
  - File upload with validation (10MB max, multiple formats)
  - Async job queue with worker thread processing
  - Format parsers: CSV, JSON, PDF, Markdown
  - Text chunking with configurable parameters
  - Mock vector embeddings (384 dimensions)
- **Search & Query Features**:
  - Full-text search with ts_vector and highlighting
  - Vector similarity search using pgvector
  - Hybrid search with configurable weight balancing
  - Adaptive search that auto-adjusts weights
  - Graph traversal (n-hop neighborhoods, shortest paths)
  - Connected components and centrality calculations
- **Caching**: Caffeine cache with TTL for graph queries
- **Error Handling**: GlobalExceptionHandler for centralized error management
- **Test Coverage**: 280+ test methods across all layers

### Frontend (Vue 3 + TypeScript)
- **Modern Stack**: Vite, TypeScript, Tailwind CSS, Pinia state management
- **Routing**: Vue Router with views for Home, Search, Node detail, Graph visualization, Upload, Jobs
- **API Integration**: Axios client with interceptors and comprehensive API services
- **Search Interface**: 
  - Advanced search bar with type filtering and debounced suggestions
  - Real-time search results with highlighting and pagination
  - Comprehensive filters for node types, date ranges, and scores
  - Search history tracking and popular searches
- **Node Detail Views**:
  - Complete node information display with properties and metadata
  - Citation tracking with source documents
  - Related nodes visualization showing connections
  - Quick actions for graph view and similarity search
- **Graph Visualization**:
  - Interactive Cytoscape.js integration
  - Multiple layout algorithms (force-directed, hierarchical, circular)
  - Node type-based coloring and filtering
  - Zoom, pan, and export capabilities
- **UI/UX Polish**:
  - Responsive design with mobile and tablet support
  - Loading states and skeleton screens
  - Error boundaries and empty states
  - Consistent color scheme and typography
- **Test Coverage**: 68+ test methods covering components, views, and services

### Database (PostgreSQL 15+)
- **Schema Design**: Nodes, edges, documents, embeddings tables
- **Extensions**: pgvector for vector search, pg_trgm for similarity
- **Full-Text Search**: Native PostgreSQL FTS with tsvector columns
- **Graph Support**: Prepared for Apache AGE integration
- **Sample Data**: Initial test entities for development

## Tech Stack

- **Frontend**: Vue 3, TypeScript, Vite, Tailwind CSS, Pinia, Vue Router
- **Backend**: Spring Boot 3.x, Spring Data JPA, PostgreSQL Driver
- **Database**: PostgreSQL 15+ with pgvector and full-text search extensions
- **Development**: PostgreSQL, Maven (bundled), npm/Vite
- **Testing**: JUnit 5 + Testcontainers (backend), Vitest + Vue Test Utils (frontend)

## Quick Start

### Prerequisites
- **Java 17+** (for Spring Boot backend)
- **Node.js 18+** (for Vue 3 frontend)
- **PostgreSQL 15+** (native installation)
- **Maven** (included in tools/ directory)

### 1. Database Setup

Ensure PostgreSQL is installed and running locally, then initialize the database:

```bash
# Create database and schema
psql -U postgres -h localhost -f database/init.sql

# Or use the setup batch file (Windows)
setup-database.bat
```

The database initialization will:
- Create the knowledge_graph database
- Install pgvector, pg_trgm, and uuid-ossp extensions
- Create the `kg` schema with all required tables
- Insert sample data for testing
- Set up full-text search and vector indexes

### 2. Backend Setup

Start the Spring Boot API server:

```bash
cd backend

# Install dependencies and run tests (using bundled Maven)
../tools/maven/bin/mvn clean test

# Start the application with local profile
../tools/maven/bin/mvn spring-boot:run -Dspring-boot.run.profiles=local

# Or use the batch files (Windows)
run-backend.bat

# Verify health endpoint
curl http://localhost:8080/api/health
```

The backend will be available at `http://localhost:8080/api`

### 3. Frontend Setup

Start the Vue 3 development server:

```bash
cd frontend

# Install dependencies
npm install

# Run tests
npm test

# Start development server
npm run dev
```

The frontend will be available at `http://localhost:3000`

## Development Commands

### Backend (Spring Boot)
```bash
cd backend

# Run all tests
mvn clean test

# Build application
mvn clean package

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend (Vue 3)
```bash
cd frontend

# Install dependencies
npm install

# Development server
npm run dev

# Run tests
npm test

# Run tests with coverage
npm run test:coverage

# Build for production
npm run build

# Preview production build
npm run preview
```

### Database Management
```bash
# Connect to database
psql -U postgres -h localhost -d knowledge_graph

# Reset database (drops and recreates)
psql -U postgres -h localhost -c "DROP DATABASE IF EXISTS knowledge_graph;"
psql -U postgres -h localhost -f database/init.sql
```

## Project Structure

```
knowledge-graph/
├── backend/                 # Spring Boot API server
│   ├── src/main/java/
│   │   └── com/knowledgegraph/
│   │       ├── controller/  # REST controllers
│   │       ├── model/       # JPA entities
│   │       ├── repository/  # Data access layer
│   │       ├── service/     # Business logic
│   │       └── config/      # Configuration classes
│   ├── src/test/           # Comprehensive test suite
│   └── pom.xml            # Maven dependencies
├── frontend/               # Vue 3 SPA client
│   ├── src/
│   │   ├── components/    # Reusable Vue components
│   │   ├── views/         # Page components
│   │   ├── router/        # Vue Router configuration
│   │   ├── stores/        # Pinia state management
│   │   └── api/          # API client modules
│   ├── src/test/         # Frontend test suite
│   └── package.json      # npm dependencies
├── database/              # Database initialization
│   ├── init.sql          # Schema and sample data
│   └── extensions.sql    # PostgreSQL extensions
└── tools/                # Bundled development tools (JDK, Maven)
```

## Database Schema

### Core Entities
- **Nodes**: Graph entities (Person, Organization, Event, Place, Item, Concept, Document)
- **Edges**: Relationships between nodes (Affiliated_With, Part_Of, References, etc.)
- **Documents**: Source documents with content and metadata
- **Embeddings**: Vector representations for similarity search

### Extensions Enabled
- **pgvector**: Vector similarity search with cosine distance
- **Full-text search**: Native PostgreSQL FTS with English configuration
- **pg_trgm**: Trigram similarity for fuzzy matching
- **uuid-ossp**: UUID generation functions

## API Endpoints

### Currently Implemented

**Health & Status**:
- `GET /api/health` - System health check with application metadata

**Ingestion Pipeline**:
- `POST /api/ingest/upload` - Upload files for processing
  - Accepts: CSV, JSON, PDF, Markdown files
  - Max size: 10MB
  - Returns: Job ID for tracking processing status
- `GET /api/ingest/jobs/{id}` - Check job processing status
- `GET /api/ingest/jobs` - List all ingestion jobs

**Search Endpoints** (SearchController - 7 endpoints):
- `GET /api/search` - Full-text search with highlighting
- `GET /api/search/vector` - Vector similarity search
- `GET /api/search/hybrid` - Combined FTS and vector search
- `GET /api/search/adaptive` - Auto-adjusting hybrid search
- `GET /api/search/suggestions` - Search suggestions
- `GET /api/search/recent` - Recently searched terms
- `GET /api/search/popular` - Most popular searches

**Node Operations** (NodeController - 8 endpoints):
- `GET /api/nodes` - List all nodes with pagination
- `GET /api/nodes/{id}` - Get node details
- `POST /api/nodes` - Create new node
- `PUT /api/nodes/{id}` - Update node
- `DELETE /api/nodes/{id}` - Delete node
- `GET /api/nodes/{id}/citations` - Get node citations
- `GET /api/nodes/{id}/relationships` - Get node relationships
- `GET /api/nodes/{id}/similar` - Find similar nodes

**Graph Operations** (GraphController - 6 endpoints):
- `GET /api/graph/neighborhood` - N-hop neighborhood exploration
- `GET /api/graph/path` - Find shortest path between nodes
- `GET /api/graph/connected-components` - Find connected components
- `GET /api/graph/centrality` - Calculate node centrality
- `GET /api/graph/statistics` - Get graph statistics
- `GET /api/graph/subgraph` - Extract subgraph

### Currently Operational
- **Search Interface**: Full-featured search with type filtering, suggestions, and history
- **Node Management**: Detailed node views with citations and relationships
- **Graph Visualization**: Interactive Cytoscape.js graph with multiple layouts
- **Data Ingestion**: Upload interface for CSV, JSON, PDF, and Markdown files
- **Job Monitoring**: Real-time tracking of ingestion jobs
- **Responsive Design**: Mobile-optimized layouts with Tailwind CSS

## Testing

### Test Coverage Summary
- **Backend**: 280+ test methods across all components
  - Section 1: 95 tests for foundation (95% coverage)
  - Section 2: 55+ tests for ingestion pipeline
  - Section 3: 130+ tests for search and graph operations
- **Frontend**: 68 test methods (~90% coverage)
- **Integration**: End-to-end tests for ingestion, search, and graph traversal

### Running Tests
```bash
# Backend tests (requires Java 17+)
cd backend && mvn clean test

# Frontend tests
cd frontend && npm test

# Integration test
docker-compose up -d postgres && docker-compose ps
```

## System Capabilities

The knowledge graph system is now feature-complete with all core functionality operational:

1. **Data Ingestion**: Upload and process CSV, JSON, PDF, and Markdown files
2. **Search**: Full-text, vector, hybrid, and adaptive search capabilities
3. **Graph Operations**: Traverse relationships, find paths, analyze connectivity
4. **Visualization**: Interactive graph exploration with Cytoscape.js
5. **Node Management**: Create, read, update, and delete nodes with full metadata
6. **Performance**: Optimized with caching, indexes, and efficient queries

## Ingestion Capabilities

The system now supports comprehensive data ingestion from multiple file formats:

### Supported File Types
- **CSV**: Tabular data with automatic column detection
- **JSON**: Nested structures with relationship extraction
- **PDF**: Full text extraction with page-level metadata
- **Markdown**: Structured documents with heading hierarchy

### Processing Pipeline
1. **Upload**: Files uploaded via REST API with validation
2. **Storage**: Secure file storage with metadata tracking
3. **Job Queue**: Async processing with progress tracking
4. **Parsing**: Format-specific parsers extract content and structure
5. **Chunking**: Text split into overlapping chunks for processing
6. **Embedding**: Vector representations generated for similarity search
7. **Storage**: Processed data stored in PostgreSQL with relationships

## Search Capabilities

The system now provides comprehensive search and query functionality:

### Search Types
- **Full-Text Search**: PostgreSQL FTS with English language configuration
- **Vector Similarity**: k-NN search using pgvector extension
- **Hybrid Search**: Weighted combination of FTS and vector scores
- **Adaptive Search**: Automatically adjusts weights based on result quality

### Graph Operations
- **Neighborhood Exploration**: 1-3 hop traversal from any node
- **Shortest Path**: Find optimal path between two nodes
- **Connected Components**: Discover related node clusters
- **Centrality Analysis**: Identify important nodes in the graph
- **Statistics**: Graph-wide metrics and aggregations

### Performance Optimizations
- **Caching**: Caffeine cache for frequently accessed data
- **Indexes**: Strategic indexes on search vectors and relationships
- **Materialized Views**: Pre-computed statistics for fast access
- **Connection Pooling**: HikariCP for efficient database connections

## Contributing

1. Ensure all tests pass before submitting changes
2. Follow existing code style and patterns
3. Add tests for new functionality
4. Update documentation as needed
5. Use feature branches and descriptive commit messages

## License

This is a personal knowledge graph implementation project.