# Knowledge Graph Implementation Plan

## Overview
Make the knowledge graph actually extract entities and relationships from documents using GPT-5 nano.

**Timeline: 1 week**

---

## Section 1: Entity Extraction

### Goal
Extract all entities from documents (people, organizations, locations, concepts, theories, etc.)

### Approach
- Use GPT-5 nano to identify entities
- Multiple passes: extract, classify, get properties
- Chunk long documents (2000 tokens each)

### Tasks
- [ ] Create GPTEntityExtractor service
- [ ] Add completion methods to OpenAI service
- [ ] Update all ingestion services to use GPT
- [ ] Store entity confidence scores
- [ ] Add entity type expansion (CONCEPT, THEORY, etc.)

---

## Section 2: Relationship Extraction

### Goal
Extract relationships between entities during the entity extraction process

### Approach
- Combined extraction: identify entities AND their relationships in a single pass
- As GPT-5 nano reads through text, it extracts:
  - Entities with their types and properties
  - Relationships between those entities with evidence
- More efficient than checking all possible entity pairs
- Natural context preservation

### Tasks
- [x] Modify GPTEntityExtractor to also extract relationships
- [x] Extend EdgeType enum with more types (added 70+ relationship types)
- [x] Store relationship evidence and confidence
- [x] Return both entities and relationships from single API call
- [x] Process relationships only between co-occurring entities

---

## Section 3: UI Updates

### Goal
Show the extracted knowledge graph properly

### Tasks
- [ ] Show entity extraction count on upload
- [ ] Color nodes by type in graph view
- [ ] Display relationship types on edges
- [ ] Add extraction statistics to job view
- [ ] Add "View Graph" button after upload success