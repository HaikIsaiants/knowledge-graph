package com.knowledgegraph;

import com.knowledgegraph.model.EdgeType;
import com.knowledgegraph.model.NodeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration.class)
class DatabaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseSchema_ShouldBeCreatedCorrectly() {
        System.out.println("Testing database schema creation...");
        
        // Test that the kg schema exists
        Integer schemaCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = 'kg'", 
                Integer.class);
        assertEquals(1, schemaCount, "kg schema should exist");
        
        System.out.println("✓ kg schema exists");
        
        // Test that all required tables exist
        String[] requiredTables = {"nodes", "edges", "documents", "embeddings"};
        
        for (String tableName : requiredTables) {
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'kg' AND table_name = ?", 
                    Integer.class, tableName);
            assertEquals(1, tableCount, tableName + " table should exist");
            System.out.println("✓ " + tableName + " table exists");
        }
        
        System.out.println("✓ Database schema created correctly");
    }

    @Test
    void customTypes_ShouldBeCreatedCorrectly() {
        System.out.println("Testing custom PostgreSQL types...");
        
        // Test node_type enum
        List<Map<String, Object>> nodeTypes = jdbcTemplate.queryForList(
                "SELECT enumlabel FROM pg_enum WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'node_type') ORDER BY enumsortorder");
        
        String[] expectedNodeTypes = Arrays.stream(NodeType.values())
                .map(Enum::name)
                .toArray(String[]::new);
        
        assertEquals(expectedNodeTypes.length, nodeTypes.size(), "Should have all node types");
        
        for (int i = 0; i < expectedNodeTypes.length; i++) {
            assertEquals(expectedNodeTypes[i], nodeTypes.get(i).get("enumlabel"), 
                    "Node type " + expectedNodeTypes[i] + " should exist");
        }
        
        System.out.println("✓ node_type enum created with " + nodeTypes.size() + " values");
        
        // Test edge_type enum
        List<Map<String, Object>> edgeTypes = jdbcTemplate.queryForList(
                "SELECT enumlabel FROM pg_enum WHERE enumtypid = (SELECT oid FROM pg_type WHERE typname = 'edge_type') ORDER BY enumsortorder");
        
        String[] expectedEdgeTypes = Arrays.stream(EdgeType.values())
                .map(Enum::name)
                .toArray(String[]::new);
        
        assertEquals(expectedEdgeTypes.length, edgeTypes.size(), "Should have all edge types");
        
        for (int i = 0; i < expectedEdgeTypes.length; i++) {
            assertEquals(expectedEdgeTypes[i], edgeTypes.get(i).get("enumlabel"), 
                    "Edge type " + expectedEdgeTypes[i] + " should exist");
        }
        
        System.out.println("✓ edge_type enum created with " + edgeTypes.size() + " values");
        System.out.println("✓ Custom types created correctly");
    }

    @Test
    void indexes_ShouldBeCreatedCorrectly() {
        System.out.println("Testing database indexes...");
        
        // Test some critical indexes exist
        String[] expectedIndexes = {
                "idx_nodes_type",
                "idx_nodes_name", 
                "idx_edges_source_id",
                "idx_edges_target_id",
                "idx_documents_uri",
                "idx_embeddings_node_id"
        };
        
        for (String indexName : expectedIndexes) {
            Integer indexCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'kg' AND indexname = ?", 
                    Integer.class, indexName);
            assertEquals(1, indexCount, indexName + " index should exist");
            System.out.println("✓ " + indexName + " index exists");
        }
        
        System.out.println("✓ Database indexes created correctly");
    }

    @Test
    void constraints_ShouldBeCreatedCorrectly() {
        System.out.println("Testing database constraints...");
        
        // Test foreign key constraints exist
        List<Map<String, Object>> foreignKeys = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                "WHERE table_schema = 'kg' AND constraint_type = 'FOREIGN KEY'");
        
        assertTrue(foreignKeys.size() >= 4, "Should have at least 4 foreign key constraints");
        
        // Look for specific constraint names
        List<String> constraintNames = foreignKeys.stream()
                .map(row -> (String) row.get("constraint_name"))
                .toList();
        
        assertTrue(constraintNames.stream().anyMatch(name -> name.contains("edge_source")), 
                "Should have edge source foreign key constraint");
        assertTrue(constraintNames.stream().anyMatch(name -> name.contains("edge_target")), 
                "Should have edge target foreign key constraint");
        assertTrue(constraintNames.stream().anyMatch(name -> name.contains("embedding_node")), 
                "Should have embedding node foreign key constraint");
        assertTrue(constraintNames.stream().anyMatch(name -> name.contains("embedding_document")), 
                "Should have embedding document foreign key constraint");
        
        System.out.println("Found " + foreignKeys.size() + " foreign key constraints:");
        constraintNames.forEach(name -> System.out.println("  - " + name));
        
        // Test unique constraints
        List<Map<String, Object>> uniqueConstraints = jdbcTemplate.queryForList(
                "SELECT constraint_name, table_name FROM information_schema.table_constraints " +
                "WHERE table_schema = 'kg' AND constraint_type = 'UNIQUE'");
        
        boolean hasDocumentUriConstraint = uniqueConstraints.stream()
                .anyMatch(row -> "documents".equals(row.get("table_name")));
        assertTrue(hasDocumentUriConstraint, "Should have unique constraint on documents URI");
        
        System.out.println("✓ Database constraints created correctly");
    }

    @Test
    void triggers_ShouldBeCreatedCorrectly() {
        System.out.println("Testing database triggers...");
        
        // Test that update triggers exist
        List<Map<String, Object>> triggers = jdbcTemplate.queryForList(
                "SELECT trigger_name, event_object_table FROM information_schema.triggers " +
                "WHERE trigger_schema = 'kg' AND trigger_name LIKE '%updated_at%'");
        
        assertTrue(triggers.size() >= 3, "Should have at least 3 updated_at triggers");
        
        List<String> triggerTables = triggers.stream()
                .map(row -> (String) row.get("event_object_table"))
                .toList();
        
        assertTrue(triggerTables.contains("nodes"), "Should have updated_at trigger on nodes table");
        assertTrue(triggerTables.contains("edges"), "Should have updated_at trigger on edges table");
        assertTrue(triggerTables.contains("documents"), "Should have updated_at trigger on documents table");
        
        System.out.println("Found updated_at triggers on tables: " + triggerTables);
        
        // Test the trigger function exists
        Integer functionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_proc WHERE proname = 'update_updated_at_column'", 
                Integer.class);
        assertEquals(1, functionCount, "update_updated_at_column function should exist");
        
        System.out.println("✓ Database triggers created correctly");
    }

    @Test
    void extensions_ShouldBeInstalled() {
        System.out.println("Testing PostgreSQL extensions...");
        
        // Test basic extensions that should always be available
        String[] basicExtensions = {"uuid-ossp", "pg_trgm"};
        
        for (String extensionName : basicExtensions) {
            Integer extensionCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_extension WHERE extname = ?", 
                    Integer.class, extensionName);
            assertEquals(1, extensionCount, extensionName + " extension should be installed");
            System.out.println("✓ " + extensionName + " extension installed");
        }
        
        // Test if vector extension is available (optional, depends on Docker image)
        try {
            Integer vectorCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'", 
                    Integer.class);
            if (vectorCount > 0) {
                System.out.println("✓ pgvector extension is available");
            } else {
                System.out.println("ℹ pgvector extension not available (expected in test environment)");
            }
        } catch (Exception e) {
            System.out.println("ℹ pgvector extension not available (expected in test environment)");
        }
        
        System.out.println("✓ Required extensions are installed");
    }

    @Test
    void sampleData_ShouldBeInserted() {
        System.out.println("Testing sample data insertion...");
        
        // Test that sample nodes were inserted
        Integer nodeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kg.nodes", Integer.class);
        assertTrue(nodeCount >= 3, "Should have at least 3 sample nodes");
        
        // Test specific sample nodes exist
        Integer johnDoeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kg.nodes WHERE name = 'John Doe'", Integer.class);
        assertEquals(1, johnDoeCount, "John Doe node should exist");
        
        Integer techCorpCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kg.nodes WHERE name = 'Tech Corp'", Integer.class);
        assertEquals(1, techCorpCount, "Tech Corp node should exist");
        
        Integer knowledgeGraphsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kg.nodes WHERE name = 'Knowledge Graphs'", Integer.class);
        assertEquals(1, knowledgeGraphsCount, "Knowledge Graphs node should exist");
        
        System.out.println("Found " + nodeCount + " nodes in database");
        
        // Test that sample edge was inserted
        Integer edgeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kg.edges", Integer.class);
        assertTrue(edgeCount >= 1, "Should have at least 1 sample edge");
        
        // Test specific sample edge exists
        Integer affiliationEdgeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kg.edges e " +
                "JOIN kg.nodes source ON e.source_id = source.id " +
                "JOIN kg.nodes target ON e.target_id = target.id " +
                "WHERE source.name = 'John Doe' AND target.name = 'Tech Corp' AND e.type = 'AFFILIATED_WITH'", 
                Integer.class);
        assertEquals(1, affiliationEdgeCount, "John Doe -> Tech Corp affiliation edge should exist");
        
        System.out.println("Found " + edgeCount + " edges in database");
        System.out.println("✓ Sample data inserted correctly");
    }

    @Test
    void jsonbColumns_ShouldWorkCorrectly() {
        System.out.println("Testing JSONB column functionality...");
        
        // Test that we can query JSONB properties
        List<Map<String, Object>> nodesWithOccupation = jdbcTemplate.queryForList(
                "SELECT name, properties FROM kg.nodes WHERE properties ->> 'occupation' IS NOT NULL");
        
        assertTrue(nodesWithOccupation.size() >= 1, "Should find nodes with occupation property");
        
        // Test that we can find John Doe's occupation
        String occupation = jdbcTemplate.queryForObject(
                "SELECT properties ->> 'occupation' FROM kg.nodes WHERE name = 'John Doe'", 
                String.class);
        assertEquals("Software Engineer", occupation, "John Doe should be a Software Engineer");
        
        System.out.println("Found node with occupation: " + occupation);
        
        // Test JSONB indexing works (GIN index)
        List<Map<String, Object>> techNodes = jdbcTemplate.queryForList(
                "SELECT name FROM kg.nodes WHERE properties @> '{\"industry\": \"Technology\"}'");
        
        assertTrue(techNodes.size() >= 1, "Should find nodes in Technology industry");
        
        System.out.println("Found " + techNodes.size() + " nodes in Technology industry");
        System.out.println("✓ JSONB columns work correctly");
    }

    @Test
    void fullTextSearch_ShouldWorkCorrectly() {
        System.out.println("Testing full-text search functionality...");
        
        // Test that search vectors are generated
        Integer nodesWithSearchVector = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kg.nodes WHERE search_vector IS NOT NULL", Integer.class);
        
        assertTrue(nodesWithSearchVector >= 3, "Should have search vectors for all sample nodes");
        
        // Test basic full-text search
        List<Map<String, Object>> softwareResults = jdbcTemplate.queryForList(
                "SELECT name FROM kg.nodes WHERE search_vector @@ plainto_tsquery('english', 'software')");
        
        assertTrue(softwareResults.size() >= 1, "Should find nodes matching 'software'");
        
        boolean foundJohnDoe = softwareResults.stream()
                .anyMatch(row -> "John Doe".equals(row.get("name")));
        assertTrue(foundJohnDoe, "Should find John Doe when searching for 'software'");
        
        System.out.println("Found " + softwareResults.size() + " nodes matching 'software'");
        
        // Test ranked search
        List<Map<String, Object>> rankedResults = jdbcTemplate.queryForList(
                "SELECT name, ts_rank(search_vector, plainto_tsquery('english', 'engineer')) as rank " +
                "FROM kg.nodes WHERE search_vector @@ plainto_tsquery('english', 'engineer') " +
                "ORDER BY rank DESC");
        
        assertTrue(rankedResults.size() >= 1, "Should find nodes matching 'engineer' with ranking");
        
        System.out.println("Found " + rankedResults.size() + " ranked results for 'engineer'");
        System.out.println("✓ Full-text search works correctly");
    }

    @Test
    void timestampDefaults_ShouldWorkCorrectly() {
        System.out.println("Testing timestamp defaults...");
        
        // Check that all sample nodes have created_at and updated_at timestamps
        Integer nodesWithTimestamps = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kg.nodes WHERE created_at IS NOT NULL AND updated_at IS NOT NULL", 
                Integer.class);
        
        Integer totalNodes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kg.nodes", Integer.class);
        assertEquals(totalNodes, nodesWithTimestamps, "All nodes should have timestamps");
        
        // Check that all sample edges have timestamps
        Integer edgesWithTimestamps = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kg.edges WHERE created_at IS NOT NULL AND updated_at IS NOT NULL", 
                Integer.class);
        
        Integer totalEdges = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kg.edges", Integer.class);
        assertEquals(totalEdges, edgesWithTimestamps, "All edges should have timestamps");
        
        System.out.println("All " + totalNodes + " nodes have proper timestamps");
        System.out.println("All " + totalEdges + " edges have proper timestamps");
        System.out.println("✓ Timestamp defaults work correctly");
    }
}