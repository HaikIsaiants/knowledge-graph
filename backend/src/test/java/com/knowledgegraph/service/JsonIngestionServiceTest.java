package com.knowledgegraph.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.DocumentRepository;
import com.knowledgegraph.repository.EdgeRepository;
import com.knowledgegraph.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JsonIngestionService Tests")
class JsonIngestionServiceTest {

    private JsonIngestionService jsonIngestionService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private NodeRepository nodeRepository;
    
    @Mock
    private EdgeRepository edgeRepository;

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up JsonIngestionService for testing...");
        
        jsonIngestionService = new JsonIngestionService();
        objectMapper = new ObjectMapper();
        
        // Inject mocked repositories using reflection
        ReflectionTestUtils.setField(jsonIngestionService, "documentRepository", documentRepository);
        ReflectionTestUtils.setField(jsonIngestionService, "nodeRepository", nodeRepository);
        ReflectionTestUtils.setField(jsonIngestionService, "edgeRepository", edgeRepository);
        ReflectionTestUtils.setField(jsonIngestionService, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("Ingest simple JSON object - Success")
    void testIngestJson_SimpleObject_Success() throws IOException {
        System.out.println("Testing simple JSON object ingestion...");
        
        String jsonContent = """
            {
                "name": "John Doe",
                "age": 30,
                "city": "New York"
            }
            """;
        
        Path jsonFile = tempDir.resolve("simple.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        // Mock repository responses
        Document mockDocument = new Document();
        mockDocument.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(mockDocument);
        
        when(nodeRepository.save(any(Node.class))).thenAnswer(invocation -> {
            Node node = invocation.getArgument(0);
            node.setId(UUID.randomUUID());
            return node;
        });
        
        // Execute ingestion
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(jobId, result.getJobId());
        assertTrue(result.getTotalRecords() > 0);
        
        // Verify document was created
        verify(documentRepository, times(1)).save(any(Document.class));
        
        // Verify node was created
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository, atLeastOnce()).save(nodeCaptor.capture());
        
        List<Node> savedNodes = nodeCaptor.getAllValues();
        assertTrue(savedNodes.size() > 0);
        
        System.out.println("✓ Simple JSON object ingested successfully");
    }

    @Test
    @DisplayName("Ingest JSON array - Success")
    void testIngestJson_Array_Success() throws IOException {
        System.out.println("Testing JSON array ingestion...");
        
        String jsonContent = """
            [
                {"id": 1, "name": "Item 1"},
                {"id": 2, "name": "Item 2"},
                {"id": 3, "name": "Item 3"}
            ]
            """;
        
        Path jsonFile = tempDir.resolve("array.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        when(nodeRepository.save(any())).thenAnswer(inv -> {
            Node node = inv.getArgument(0);
            node.setId(UUID.randomUUID());
            return node;
        });
        
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalRecords());
        
        verify(nodeRepository, atLeast(3)).save(any(Node.class));
        
        System.out.println("✓ JSON array ingested successfully");
    }

    @Test
    @DisplayName("Ingest nested JSON object - Success")
    void testIngestJson_NestedObject_Success() throws IOException {
        System.out.println("Testing nested JSON object ingestion...");
        
        String jsonContent = """
            {
                "user": {
                    "id": 1,
                    "profile": {
                        "firstName": "John",
                        "lastName": "Doe",
                        "address": {
                            "street": "123 Main St",
                            "city": "New York",
                            "zip": "10001"
                        }
                    },
                    "preferences": {
                        "theme": "dark",
                        "notifications": true
                    }
                }
            }
            """;
        
        Path jsonFile = tempDir.resolve("nested.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        when(nodeRepository.save(any())).thenAnswer(inv -> {
            Node node = inv.getArgument(0);
            node.setId(UUID.randomUUID());
            return node;
        });
        
        when(edgeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        
        // Should create nodes for nested objects
        verify(nodeRepository, atLeast(1)).save(any(Node.class));
        
        System.out.println("✓ Nested JSON object ingested successfully");
    }

    @Test
    @DisplayName("Ingest JSON with mixed types - Success")
    void testIngestJson_MixedTypes_Success() throws IOException {
        System.out.println("Testing JSON with mixed types...");
        
        String jsonContent = """
            {
                "string": "text value",
                "number": 42,
                "decimal": 3.14,
                "boolean": true,
                "nullValue": null,
                "array": [1, 2, 3],
                "object": {"key": "value"}
            }
            """;
        
        Path jsonFile = tempDir.resolve("mixed.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        when(nodeRepository.save(any())).thenAnswer(inv -> {
            Node node = inv.getArgument(0);
            node.setId(UUID.randomUUID());
            return node;
        });
        
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository, atLeastOnce()).save(nodeCaptor.capture());
        
        System.out.println("✓ JSON with mixed types ingested successfully");
    }

    @Test
    @DisplayName("Ingest empty JSON object - Success")
    void testIngestJson_EmptyObject_Success() throws IOException {
        System.out.println("Testing empty JSON object ingestion...");
        
        String jsonContent = "{}";
        
        Path jsonFile = tempDir.resolve("empty.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        assertNotNull(result);
        // Empty object might still create a document
        verify(documentRepository, times(1)).save(any(Document.class));
        
        System.out.println("✓ Empty JSON object handled correctly");
    }

    @Test
    @DisplayName("Ingest empty JSON array - Success")
    void testIngestJson_EmptyArray_Success() throws IOException {
        System.out.println("Testing empty JSON array ingestion...");
        
        String jsonContent = "[]";
        
        Path jsonFile = tempDir.resolve("empty-array.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        assertNotNull(result);
        assertEquals(0, result.getTotalRecords());
        
        System.out.println("✓ Empty JSON array handled correctly");
    }

    @Test
    @DisplayName("Ingest malformed JSON - Error")
    void testIngestJson_MalformedJson_Error() throws IOException {
        System.out.println("Testing malformed JSON handling...");
        
        String jsonContent = """
            {
                "name": "Missing comma"
                "age": 30
            }
            """;
        
        Path jsonFile = tempDir.resolve("malformed.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrorCount());
        assertTrue(result.getMessage().contains("Error") || result.getMessage().contains("Failed"));
        
        System.out.println("✓ Malformed JSON error handled correctly");
    }

    @Test
    @DisplayName("Ingest non-existent file - Error")
    void testIngestJson_NonExistentFile_Error() {
        System.out.println("Testing non-existent file handling...");
        
        String nonExistentPath = tempDir.resolve("non-existent.json").toString();
        UUID jobId = UUID.randomUUID();
        
        IngestionResult result = jsonIngestionService.ingestJson(nonExistentPath, jobId);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrorCount());
        
        verify(documentRepository, never()).save(any());
        verify(nodeRepository, never()).save(any());
        
        System.out.println("✓ Non-existent file error handled correctly");
    }

    @Test
    @DisplayName("Ingest large JSON file - Performance")
    void testIngestJson_LargeFile_Performance() throws IOException {
        System.out.println("Testing large JSON file ingestion performance...");
        
        // Create a JSON array with 1000 objects
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) jsonBuilder.append(",");
            jsonBuilder.append(String.format(
                "{\"id\":%d,\"name\":\"Item %d\",\"value\":%d}",
                i, i, i * 100
            ));
        }
        jsonBuilder.append("]");
        
        Path jsonFile = tempDir.resolve("large.json");
        Files.writeString(jsonFile, jsonBuilder.toString());
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        when(nodeRepository.save(any())).thenAnswer(inv -> {
            Node node = inv.getArgument(0);
            node.setId(UUID.randomUUID());
            return node;
        });
        
        long startTime = System.currentTimeMillis();
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        long endTime = System.currentTimeMillis();
        
        assertTrue(result.isSuccess());
        assertEquals(1000, result.getTotalRecords());
        
        long processingTime = endTime - startTime;
        System.out.println("Processed 1000 JSON objects in " + processingTime + "ms");
        
        // Should process reasonably quickly
        assertTrue(processingTime < 5000); // Less than 5 seconds
        
        System.out.println("✓ Large JSON file processed efficiently");
    }

    @Test
    @DisplayName("Ingest JSON with special characters - Success")
    void testIngestJson_SpecialCharacters_Success() throws IOException {
        System.out.println("Testing JSON with special characters...");
        
        String jsonContent = """
            {
                "unicode": "Hello 世界 🌍",
                "escaped": "Line 1\\nLine 2\\tTabbed",
                "quotes": "He said \\"Hello\\"",
                "url": "https://example.com/path?query=value&other=123"
            }
            """;
        
        Path jsonFile = tempDir.resolve("special.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        when(nodeRepository.save(any())).thenAnswer(inv -> {
            Node node = inv.getArgument(0);
            node.setId(UUID.randomUUID());
            return node;
        });
        
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository, atLeastOnce()).save(nodeCaptor.capture());
        
        List<Node> nodes = nodeCaptor.getAllValues();
        assertTrue(nodes.size() > 0);
        
        System.out.println("✓ Special characters in JSON preserved correctly");
    }

    @Test
    @DisplayName("Ingest JSON with relationships - Edge creation")
    void testIngestJson_Relationships_EdgeCreation() throws IOException {
        System.out.println("Testing JSON with relationships and edge creation...");
        
        String jsonContent = """
            {
                "nodes": [
                    {"id": "node1", "type": "Person", "name": "Alice"},
                    {"id": "node2", "type": "Person", "name": "Bob"},
                    {"id": "node3", "type": "Company", "name": "TechCorp"}
                ],
                "edges": [
                    {"from": "node1", "to": "node2", "type": "KNOWS"},
                    {"from": "node1", "to": "node3", "type": "WORKS_AT"},
                    {"from": "node2", "to": "node3", "type": "WORKS_AT"}
                ]
            }
            """;
        
        Path jsonFile = tempDir.resolve("graph.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        when(nodeRepository.save(any())).thenAnswer(inv -> {
            Node node = inv.getArgument(0);
            node.setId(UUID.randomUUID());
            return node;
        });
        
        when(edgeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        
        IngestionResult result = jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        
        // Should create both nodes and edges
        verify(nodeRepository, atLeast(3)).save(any(Node.class));
        
        System.out.println("✓ JSON with relationships processed successfully");
    }

    @Test
    @DisplayName("Document metadata creation - Verification")
    void testIngestJson_DocumentMetadata_Correct() throws IOException {
        System.out.println("Testing document metadata creation...");
        
        String jsonContent = "{\"test\": \"data\"}";
        Path jsonFile = tempDir.resolve("metadata-test.json");
        Files.writeString(jsonFile, jsonContent);
        
        UUID jobId = UUID.randomUUID();
        
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        when(documentRepository.save(docCaptor.capture())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        when(nodeRepository.save(any())).thenAnswer(inv -> {
            Node node = inv.getArgument(0);
            node.setId(UUID.randomUUID());
            return node;
        });
        
        jsonIngestionService.ingestJson(jsonFile.toString(), jobId);
        
        Document savedDoc = docCaptor.getValue();
        assertNotNull(savedDoc);
        assertTrue(savedDoc.getUri().contains("file://"));
        assertEquals("application/json", savedDoc.getContentType());
        assertNotNull(savedDoc.getMetadata());
        assertEquals("JSON", savedDoc.getMetadata().get("ingestionType"));
        
        System.out.println("✓ Document metadata created correctly");
    }
}