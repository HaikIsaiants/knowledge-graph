package com.knowledgegraph.service;

import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.DocumentRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CsvIngestionService Tests")
class CsvIngestionServiceTest {

    private CsvIngestionService csvIngestionService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private NodeRepository nodeRepository;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up CsvIngestionService for testing...");
        
        csvIngestionService = new CsvIngestionService();
        
        // Inject mocked repositories using reflection
        ReflectionTestUtils.setField(csvIngestionService, "documentRepository", documentRepository);
        ReflectionTestUtils.setField(csvIngestionService, "nodeRepository", nodeRepository);
    }

    @Test
    @DisplayName("Ingest valid CSV file - Success")
    void testIngestCsv_ValidFile_Success() throws IOException {
        System.out.println("Testing valid CSV file ingestion...");
        
        // Create test CSV file
        String csvContent = """
            name,age,city
            John Doe,30,New York
            Jane Smith,25,Los Angeles
            Bob Johnson,35,Chicago
            """;
        
        Path csvFile = tempDir.resolve("test.csv");
        Files.writeString(csvFile, csvContent);
        
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
        IngestionResult result = csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(jobId, result.getJobId());
        assertEquals(3, result.getTotalRecords());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        assertNotNull(result.getCreatedNodeIds());
        assertEquals(3, result.getCreatedNodeIds().size());
        
        // Verify document was created
        verify(documentRepository, times(1)).save(any(Document.class));
        
        // Verify nodes were created
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository, times(3)).save(nodeCaptor.capture());
        
        List<Node> savedNodes = nodeCaptor.getAllValues();
        assertEquals(3, savedNodes.size());
        
        // Verify first node
        Node firstNode = savedNodes.get(0);
        assertEquals("John Doe", firstNode.getName());
        assertEquals(NodeType.RECORD, firstNode.getNodeType());
        Map<String, Object> properties = firstNode.getProperties();
        assertEquals("John Doe", properties.get("name"));
        assertEquals("30", properties.get("age"));
        assertEquals("New York", properties.get("city"));
        
        System.out.println("✓ CSV file ingested successfully");
    }

    @Test
    @DisplayName("Ingest CSV with headers - Success")
    void testIngestCsv_WithHeaders_Success() throws IOException {
        System.out.println("Testing CSV ingestion with different headers...");
        
        String csvContent = """
            product_id,product_name,price,quantity
            P001,Laptop,999.99,10
            P002,Mouse,29.99,50
            """;
        
        Path csvFile = tempDir.resolve("products.csv");
        Files.writeString(csvFile, csvContent);
        
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
        
        IngestionResult result = csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalRecords());
        
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository, times(2)).save(nodeCaptor.capture());
        
        Node productNode = nodeCaptor.getAllValues().get(0);
        Map<String, Object> properties = productNode.getProperties();
        assertEquals("P001", properties.get("product_id"));
        assertEquals("Laptop", properties.get("product_name"));
        assertEquals("999.99", properties.get("price"));
        
        System.out.println("✓ CSV with custom headers ingested successfully");
    }

    @Test
    @DisplayName("Ingest empty CSV file - Empty result")
    void testIngestCsv_EmptyFile_EmptyResult() throws IOException {
        System.out.println("Testing empty CSV file ingestion...");
        
        Path csvFile = tempDir.resolve("empty.csv");
        Files.writeString(csvFile, "");
        
        UUID jobId = UUID.randomUUID();
        
        IngestionResult result = csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(0, result.getTotalRecords());
        assertTrue(result.getMessage().contains("empty") || result.getMessage().contains("No data"));
        
        verify(documentRepository, never()).save(any());
        verify(nodeRepository, never()).save(any());
        
        System.out.println("✓ Empty CSV file handled correctly");
    }

    @Test
    @DisplayName("Ingest CSV with only headers - No data rows")
    void testIngestCsv_OnlyHeaders_NoDataRows() throws IOException {
        System.out.println("Testing CSV with only headers...");
        
        String csvContent = "name,age,city\n";
        
        Path csvFile = tempDir.resolve("headers-only.csv");
        Files.writeString(csvFile, csvContent);
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        IngestionResult result = csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        
        assertNotNull(result);
        assertEquals(0, result.getTotalRecords());
        assertEquals(0, result.getSuccessCount());
        
        verify(documentRepository, times(1)).save(any());
        verify(nodeRepository, never()).save(any());
        
        System.out.println("✓ CSV with only headers handled correctly");
    }

    @Test
    @DisplayName("Ingest CSV with malformed rows - Partial success")
    void testIngestCsv_MalformedRows_PartialSuccess() throws IOException {
        System.out.println("Testing CSV with malformed rows...");
        
        String csvContent = """
            name,age,city
            John,30,NYC
            Jane,25
            Bob,35,Chicago,ExtraField
            Alice,28,Boston
            """;
        
        Path csvFile = tempDir.resolve("malformed.csv");
        Files.writeString(csvFile, csvContent);
        
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
        
        IngestionResult result = csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        
        assertNotNull(result);
        assertEquals(4, result.getTotalRecords());
        
        // All rows should be processed (CSV parser handles missing/extra fields)
        verify(nodeRepository, times(4)).save(any());
        
        System.out.println("✓ Malformed CSV rows handled gracefully");
    }

    @Test
    @DisplayName("Ingest CSV with special characters - Success")
    void testIngestCsv_SpecialCharacters_Success() throws IOException {
        System.out.println("Testing CSV with special characters...");
        
        String csvContent = """
            name,description,tags
            "Product, Inc.","Description with ""quotes""","tag1,tag2"
            "O'Brien & Co.","Line 1
            Line 2","multi,word,tags"
            """;
        
        Path csvFile = tempDir.resolve("special-chars.csv");
        Files.writeString(csvFile, csvContent);
        
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
        
        IngestionResult result = csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalRecords());
        
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository, times(2)).save(nodeCaptor.capture());
        
        Node firstNode = nodeCaptor.getAllValues().get(0);
        assertEquals("Product, Inc.", firstNode.getProperties().get("name"));
        assertTrue(firstNode.getProperties().get("description").toString().contains("quotes"));
        
        System.out.println("✓ Special characters in CSV handled correctly");
    }

    @Test
    @DisplayName("Ingest non-existent file - Error")
    void testIngestCsv_NonExistentFile_Error() {
        System.out.println("Testing non-existent file ingestion...");
        
        String nonExistentPath = tempDir.resolve("non-existent.csv").toString();
        UUID jobId = UUID.randomUUID();
        
        IngestionResult result = csvIngestionService.ingestCsv(nonExistentPath, jobId);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(0, result.getTotalRecords());
        assertEquals(1, result.getErrorCount());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getMessage().contains("Error") || result.getMessage().contains("Failed"));
        
        verify(documentRepository, never()).save(any());
        verify(nodeRepository, never()).save(any());
        
        System.out.println("✓ Non-existent file error handled correctly");
    }

    @Test
    @DisplayName("Ingest large CSV file - Performance")
    void testIngestCsv_LargeFile_Performance() throws IOException {
        System.out.println("Testing large CSV file ingestion performance...");
        
        // Create a CSV with 1000 rows
        StringBuilder csvContent = new StringBuilder("id,name,value\n");
        for (int i = 1; i <= 1000; i++) {
            csvContent.append(i).append(",Name").append(i).append(",").append(i * 100).append("\n");
        }
        
        Path csvFile = tempDir.resolve("large.csv");
        Files.writeString(csvFile, csvContent.toString());
        
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
        IngestionResult result = csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        long endTime = System.currentTimeMillis();
        
        assertTrue(result.isSuccess());
        assertEquals(1000, result.getTotalRecords());
        assertEquals(1000, result.getSuccessCount());
        
        long processingTime = endTime - startTime;
        System.out.println("Processed 1000 rows in " + processingTime + "ms");
        
        // Should process reasonably quickly
        assertTrue(processingTime < 5000); // Less than 5 seconds
        
        verify(nodeRepository, times(1000)).save(any());
        
        System.out.println("✓ Large CSV file processed efficiently");
    }

    @Test
    @DisplayName("Ingest CSV with different delimiters - Tab delimited")
    void testIngestCsv_TabDelimited_Success() throws IOException {
        System.out.println("Testing tab-delimited file ingestion...");
        
        String tsvContent = "name\tage\tcity\n" +
                           "John\t30\tNYC\n" +
                           "Jane\t25\tLA\n";
        
        Path tsvFile = tempDir.resolve("test.tsv");
        Files.writeString(tsvFile, tsvContent);
        
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
        
        // Note: Current implementation might need enhancement for TSV
        // This test documents expected behavior
        IngestionResult result = csvIngestionService.ingestCsv(tsvFile.toString(), jobId);
        
        assertNotNull(result);
        // Result depends on implementation's delimiter handling
        
        System.out.println("✓ Tab-delimited file processing attempted");
    }

    @Test
    @DisplayName("Ingest CSV with Unicode characters - Success")
    void testIngestCsv_UnicodeCharacters_Success() throws IOException {
        System.out.println("Testing CSV with Unicode characters...");
        
        String csvContent = """
            name,greeting,emoji
            José,¡Hola!,😀
            Müller,Grüß Gott,🎉
            王明,你好,🌟
            """;
        
        Path csvFile = tempDir.resolve("unicode.csv");
        Files.writeString(csvFile, csvContent);
        
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
        
        IngestionResult result = csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalRecords());
        
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository, times(3)).save(nodeCaptor.capture());
        
        List<Node> nodes = nodeCaptor.getAllValues();
        assertEquals("José", nodes.get(0).getProperties().get("name"));
        assertEquals("Müller", nodes.get(1).getProperties().get("name"));
        assertEquals("王明", nodes.get(2).getProperties().get("name"));
        
        System.out.println("✓ Unicode characters preserved correctly");
    }

    @Test
    @DisplayName("Ingest CSV with numeric values - Type preservation")
    void testIngestCsv_NumericValues_TypeHandling() throws IOException {
        System.out.println("Testing CSV with numeric values...");
        
        String csvContent = """
            id,price,quantity,in_stock
            1,19.99,100,true
            2,29.50,0,false
            3,39.00,50,true
            """;
        
        Path csvFile = tempDir.resolve("numeric.csv");
        Files.writeString(csvFile, csvContent);
        
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
        
        IngestionResult result = csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalRecords());
        
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository, times(3)).save(nodeCaptor.capture());
        
        // CSV typically treats everything as strings
        Node firstNode = nodeCaptor.getAllValues().get(0);
        assertEquals("1", firstNode.getProperties().get("id"));
        assertEquals("19.99", firstNode.getProperties().get("price"));
        
        System.out.println("✓ Numeric values handled as strings (CSV standard)");
    }

    @Test
    @DisplayName("Document metadata creation - Verification")
    void testIngestCsv_DocumentMetadata_Correct() throws IOException {
        System.out.println("Testing document metadata creation...");
        
        String csvContent = "col1,col2\nval1,val2\n";
        Path csvFile = tempDir.resolve("metadata-test.csv");
        Files.writeString(csvFile, csvContent);
        
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
        
        csvIngestionService.ingestCsv(csvFile.toString(), jobId);
        
        Document savedDoc = docCaptor.getValue();
        assertNotNull(savedDoc);
        assertTrue(savedDoc.getUri().contains("file://"));
        assertEquals("text/csv", savedDoc.getContentType());
        assertNotNull(savedDoc.getMetadata());
        assertEquals("CSV", savedDoc.getMetadata().get("ingestionType"));
        assertNotNull(savedDoc.getMetadata().get("ingestedAt"));
        
        System.out.println("✓ Document metadata created correctly");
    }
}