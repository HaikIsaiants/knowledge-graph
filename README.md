# Knowledge Graph

A personal knowledge graph that ingests documents (PDF, CSV, JSON, Markdown), lets you search with hybrid full‑text + vector similarity, and explore relationships in an interactive graph UI.

## Stack

- Frontend: Vue 3, TypeScript, Vite, Tailwind, Cytoscape.js
- Backend: Spring Boot 3 (Java 17), Spring Data JPA
- Database: PostgreSQL 15+ with pgvector

## Run Locally (bash)

### Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 15+
- Maven (on your PATH)

### 1) Database

Use the provided script to create the database, schema, extensions, and indexes:

```bash
psql -U postgres -h localhost -f setup-database.sql
```

If the database already exists, you can ensure the schema and `vector` extension only:

```bash
psql -U postgres -h localhost -d knowledge_graph -f backend/setup-vector-extension.sql
```

### 2) Environment

```bash
cp .env.example .env
```

### 3) Backend

```bash
cd backend && mvn spring-boot:run
```

### 4) Frontend

```bash
cd frontend
npm install
npm run dev
```

## Notes

- The dev frontend is configured to proxy `/api` to the backend.
