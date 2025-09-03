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
Identify how entities are related to each other

### Approach
- Send entity pairs to GPT-5 nano
- Get relationship type, direction, and confidence
- Extract evidence from source text
- Batch process for efficiency

### Tasks
- [ ] Create GPTRelationshipExtractor service
- [ ] Extend EdgeType enum with more types
- [ ] Store relationship evidence and confidence
- [ ] Add indirect relationship inference
- [ ] Batch entity pairs in single API calls

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