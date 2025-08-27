package com.knowledgegraph.service;

import com.knowledgegraph.config.IngestionConfig;
import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.DocumentRepository;
import com.knowledgegraph.repository.EmbeddingRepository;
import com.knowledgegraph.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarkdownIngestionService Tests")
class MarkdownIngestionServiceTest {

    @InjectMocks
    private MarkdownIngestionService markdownIngestionService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private EmbeddingRepository embeddingRepository;

    @Mock
    private TextChunkingService textChunkingService;

    @Mock
    private MockEmbeddingService embeddingService;

    @Mock
    private IngestionConfig ingestionConfig;

    @TempDir
    Path tempDir;

    private UUID testJobId;
    private Path testMarkdownFile;

    @BeforeEach
    void setUp() throws IOException {
        System.out.println("Setting up MarkdownIngestionService test environment...");
        
        testJobId = UUID.randomUUID();
        testMarkdownFile = tempDir.resolve("test.md");
        
        // Set up default config
        IngestionConfig.Processing processing = new IngestionConfig.Processing();
        processing.setChunkSize(500);
        processing.setChunkOverlap(0.1);
        when(ingestionConfig.getProcessing()).thenReturn(processing);
    }

    @Test
    @DisplayName("Ingest simple markdown file - Success")
    void testIngestMarkdown_SimpleFile_Success() throws Exception {
        System.out.println("Testing simple markdown file ingestion...");
        
        String markdownContent = "# Test Document\n\n" +
                "This is a test paragraph.\n\n" +
                "## Section 1\n" +
                "Content for section 1.\n\n" +
                "## Section 2\n" +
                "Content for section 2.";
        
        Files.writeString(testMarkdownFile, markdownContent);
        
        // Mock document save
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        savedDoc.setUri("file://" + testMarkdownFile.toString());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        // Mock text chunking
        List<String> chunks = Arrays.asList(
            "# Test Document\n\nThis is a test paragraph.",
            "## Section 1\nContent for section 1.",
            "## Section 2\nContent for section 2."
        );
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenReturn(chunks);
        
        // Mock embeddings
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        
        // Mock node saves
        Node node = new Node();
        node.setId(UUID.randomUUID());
        when(nodeRepository.save(any(Node.class))).thenReturn(node);
        
        System.out.println("Processing markdown file: " + testMarkdownFile);
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(testJobId, result.getJobId());
        assertEquals(3, result.getTotalRecords()); // 3 chunks
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        assertNotNull(result.getCreatedNodeIds());
        assertEquals(3, result.getCreatedNodeIds().size());
        
        // Verify document was saved
        verify(documentRepository, times(1)).save(any(Document.class));
        
        // Verify nodes were created for each chunk
        verify(nodeRepository, times(3)).save(any(Node.class));
        
        // Verify embeddings were generated and saved
        verify(embeddingService, times(3)).generateEmbedding(anyString());
        verify(embeddingRepository, times(3)).save(any());
        
        System.out.println("✓ Simple markdown file ingested successfully");
    }

    @Test
    @DisplayName("Ingest markdown with headers - Success")
    void testIngestMarkdown_WithHeaders_Success() throws Exception {
        System.out.println("Testing markdown with multiple header levels...");
        
        String markdownContent = "# Main Title\n\n" +
                "## Subtitle\n\n" +
                "### Sub-subtitle\n\n" +
                "Some content here.\n\n" +
                "#### Deep level\n\n" +
                "More content.";
        
        Files.writeString(testMarkdownFile, markdownContent);
        
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenReturn(Arrays.asList(markdownContent));
        
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[]{0.1f, 0.2f});
        
        Node node = new Node();
        node.setId(UUID.randomUUID());
        when(nodeRepository.save(any(Node.class))).thenReturn(node);
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertTrue(result.isSuccess());
        
        // Verify node was created with proper metadata
        ArgumentCaptor<Node> nodeCaptor = ArgumentCaptor.forClass(Node.class);
        verify(nodeRepository).save(nodeCaptor.capture());
        
        Node capturedNode = nodeCaptor.getValue();
        assertEquals(NodeType.DOCUMENT, capturedNode.getType());
        assertNotNull(capturedNode.getMetadata());
        assertTrue(capturedNode.getMetadata().containsKey("source"));
        
        System.out.println("✓ Markdown with headers processed correctly");
    }

    @Test
    @DisplayName("Ingest markdown with code blocks - Success")
    void testIngestMarkdown_WithCodeBlocks_Success() throws Exception {
        System.out.println("Testing markdown with code blocks...");
        
        String markdownContent = "# Code Examples\n\n" +
                "Here's some code:\n\n" +
                "```java\n" +
                "public class Test {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello\");\n" +
                "    }\n" +
                "}\n" +
                "```\n\n" +
                "And some inline `code` too.";
        
        Files.writeString(testMarkdownFile, markdownContent);
        
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenReturn(Arrays.asList(markdownContent));
        
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[]{0.1f});
        
        Node node = new Node();
        node.setId(UUID.randomUUID());
        when(nodeRepository.save(any(Node.class))).thenReturn(node);
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalRecords());
        
        System.out.println("✓ Markdown with code blocks processed correctly");
    }

    @Test
    @DisplayName("Ingest markdown with lists - Success")
    void testIngestMarkdown_WithLists_Success() throws Exception {
        System.out.println("Testing markdown with lists...");
        
        String markdownContent = "# Lists\n\n" +
                "## Unordered List\n" +
                "- Item 1\n" +
                "- Item 2\n" +
                "  - Nested item\n" +
                "- Item 3\n\n" +
                "## Ordered List\n" +
                "1. First\n" +
                "2. Second\n" +
                "3. Third";
        
        Files.writeString(testMarkdownFile, markdownContent);
        
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        List<String> chunks = Arrays.asList(
            "# Lists\n\n## Unordered List\n- Item 1\n- Item 2\n  - Nested item\n- Item 3",
            "## Ordered List\n1. First\n2. Second\n3. Third"
        );
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenReturn(chunks);
        
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[]{0.1f});
        
        Node node = new Node();
        node.setId(UUID.randomUUID());
        when(nodeRepository.save(any(Node.class))).thenReturn(node);
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalRecords());
        
        System.out.println("✓ Markdown with lists processed correctly");
    }

    @Test
    @DisplayName("Ingest markdown with links and images - Success")
    void testIngestMarkdown_WithLinksAndImages_Success() throws Exception {
        System.out.println("Testing markdown with links and images...");
        
        String markdownContent = "# Links and Images\n\n" +
                "[This is a link](https://example.com)\n\n" +
                "![Alt text](image.png)\n\n" +
                "[Reference link][ref]\n\n" +
                "[ref]: https://reference.com";
        
        Files.writeString(testMarkdownFile, markdownContent);
        
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenReturn(Arrays.asList(markdownContent));
        
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[]{0.1f});
        
        Node node = new Node();
        node.setId(UUID.randomUUID());
        when(nodeRepository.save(any(Node.class))).thenReturn(node);
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertTrue(result.isSuccess());
        
        System.out.println("✓ Markdown with links and images processed correctly");
    }

    @Test
    @DisplayName("Ingest empty markdown file - Edge case")
    void testIngestMarkdown_EmptyFile() throws Exception {
        System.out.println("Testing empty markdown file...");
        
        Files.writeString(testMarkdownFile, "");
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertFalse(result.isSuccess());
        assertEquals("File is empty or contains only whitespace", result.getMessage());
        assertEquals(1, result.getErrorCount());
        
        // Verify no nodes were created
        verify(nodeRepository, never()).save(any(Node.class));
        verify(documentRepository, never()).save(any(Document.class));
        
        System.out.println("✓ Empty file handled correctly");
    }

    @Test
    @DisplayName("Ingest non-existent file - Error handling")
    void testIngestMarkdown_FileNotFound() {
        System.out.println("Testing non-existent file handling...");
        
        String nonExistentPath = tempDir.resolve("non-existent.md").toString();
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            nonExistentPath, testJobId
        );
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Error reading file"));
        assertEquals(1, result.getErrorCount());
        assertNotNull(result.getErrors());
        assertFalse(result.getErrors().isEmpty());
        
        System.out.println("✓ File not found error handled correctly");
    }

    @Test
    @DisplayName("Ingest large markdown file - Performance test")
    void testIngestMarkdown_LargeFile() throws Exception {
        System.out.println("Testing large markdown file processing...");
        
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeContent.append("# Section ").append(i).append("\n\n");
            largeContent.append("This is content for section ").append(i).append(".\n");
            largeContent.append("It contains multiple paragraphs and lots of text.\n\n");
        }
        
        Files.writeString(testMarkdownFile, largeContent.toString());
        
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        // Simulate chunking into 10 chunks
        List<String> chunks = Arrays.asList(new String[10]);
        Arrays.fill(chunks.toArray(), "chunk content");
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenReturn(chunks);
        
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[]{0.1f});
        
        Node node = new Node();
        node.setId(UUID.randomUUID());
        when(nodeRepository.save(any(Node.class))).thenReturn(node);
        
        long startTime = System.currentTimeMillis();
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        long endTime = System.currentTimeMillis();
        
        assertTrue(result.isSuccess());
        assertEquals(10, result.getTotalRecords());
        
        System.out.println("Processing time: " + (endTime - startTime) + "ms");
        System.out.println("✓ Large file processed successfully");
    }

    @Test
    @DisplayName("Handle chunking service failure - Error recovery")
    void testIngestMarkdown_ChunkingFailure() throws Exception {
        System.out.println("Testing chunking service failure handling...");
        
        String markdownContent = "# Test\n\nContent";
        Files.writeString(testMarkdownFile, markdownContent);
        
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenThrow(new RuntimeException("Chunking service error"));
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Error processing markdown"));
        assertEquals(1, result.getErrorCount());
        
        System.out.println("✓ Chunking service failure handled gracefully");
    }

    @Test
    @DisplayName("Handle embedding service failure - Partial success")
    void testIngestMarkdown_EmbeddingFailure() throws Exception {
        System.out.println("Testing embedding service failure handling...");
        
        String markdownContent = "# Test\n\nContent for testing";
        Files.writeString(testMarkdownFile, markdownContent);
        
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        List<String> chunks = Arrays.asList("Chunk 1", "Chunk 2", "Chunk 3");
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenReturn(chunks);
        
        // First embedding succeeds, second fails, third succeeds
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[]{0.1f})
            .thenThrow(new RuntimeException("Embedding error"))
            .thenReturn(new float[]{0.3f});
        
        Node node = new Node();
        node.setId(UUID.randomUUID());
        when(nodeRepository.save(any(Node.class))).thenReturn(node);
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertFalse(result.isSuccess()); // Has errors
        assertEquals(3, result.getTotalRecords());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());
        assertNotNull(result.getErrors());
        assertEquals(1, result.getErrors().size());
        
        System.out.println("✓ Embedding service failure handled with partial success");
    }

    @Test
    @DisplayName("Ingest markdown with tables - Success")
    void testIngestMarkdown_WithTables() throws Exception {
        System.out.println("Testing markdown with tables...");
        
        String markdownContent = "# Data Table\n\n" +
                "| Header 1 | Header 2 | Header 3 |\n" +
                "|----------|----------|----------|\n" +
                "| Cell 1   | Cell 2   | Cell 3   |\n" +
                "| Cell 4   | Cell 5   | Cell 6   |\n";
        
        Files.writeString(testMarkdownFile, markdownContent);
        
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenReturn(Arrays.asList(markdownContent));
        
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[]{0.1f});
        
        Node node = new Node();
        node.setId(UUID.randomUUID());
        when(nodeRepository.save(any(Node.class))).thenReturn(node);
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertTrue(result.isSuccess());
        
        System.out.println("✓ Markdown with tables processed correctly");
    }

    @Test
    @DisplayName("Ingest markdown with blockquotes - Success")
    void testIngestMarkdown_WithBlockquotes() throws Exception {
        System.out.println("Testing markdown with blockquotes...");
        
        String markdownContent = "# Quotes\n\n" +
                "> This is a blockquote\n" +
                "> with multiple lines\n\n" +
                "Normal text\n\n" +
                "> > Nested quote";
        
        Files.writeString(testMarkdownFile, markdownContent);
        
        Document savedDoc = new Document();
        savedDoc.setId(UUID.randomUUID());
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);
        
        when(textChunkingService.chunkText(anyString(), anyInt(), anyDouble()))
            .thenReturn(Arrays.asList(markdownContent));
        
        when(embeddingService.generateEmbedding(anyString()))
            .thenReturn(new float[]{0.1f});
        
        Node node = new Node();
        node.setId(UUID.randomUUID());
        when(nodeRepository.save(any(Node.class))).thenReturn(node);
        
        IngestionResult result = markdownIngestionService.ingestMarkdown(
            testMarkdownFile.toString(), testJobId
        );
        
        assertTrue(result.isSuccess());
        
        System.out.println("✓ Markdown with blockquotes processed correctly");
    }
}