package com.knowledgegraph.service;

import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Embedding;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.repository.DocumentRepository;
import com.knowledgegraph.repository.EmbeddingRepository;
import com.knowledgegraph.repository.NodeRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
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
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PdfIngestionService Tests")
class PdfIngestionServiceTest {

    private PdfIngestionService pdfIngestionService;

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

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up PdfIngestionService for testing...");
        
        pdfIngestionService = new PdfIngestionService();
        
        // Inject mocked dependencies using reflection
        ReflectionTestUtils.setField(pdfIngestionService, "documentRepository", documentRepository);
        ReflectionTestUtils.setField(pdfIngestionService, "nodeRepository", nodeRepository);
        ReflectionTestUtils.setField(pdfIngestionService, "embeddingRepository", embeddingRepository);
        ReflectionTestUtils.setField(pdfIngestionService, "textChunkingService", textChunkingService);
        ReflectionTestUtils.setField(pdfIngestionService, "embeddingService", embeddingService);
    }

    @Test
    @DisplayName("Ingest simple PDF file - Success")
    void testIngestPdf_SimpleFile_Success() throws IOException {
        System.out.println("Testing simple PDF file ingestion...");
        
        // Create a test PDF file
        Path pdfFile = createTestPdf("Test PDF Content\nThis is a test PDF document.\nIt contains multiple lines.");
        
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
        
        when(textChunkingService.chunkText(anyString())).thenReturn(
            java.util.List.of("Test PDF Content", "This is a test PDF document.", "It contains multiple lines.")
        );
        
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[768]);
        
        when(embeddingRepository.save(any(Embedding.class))).thenAnswer(invocation -> {
            Embedding embedding = invocation.getArgument(0);
            embedding.setId(UUID.randomUUID());
            return embedding;
        });
        
        // Execute ingestion
        IngestionResult result = pdfIngestionService.ingestPdf(pdfFile.toString(), jobId);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(jobId, result.getJobId());
        
        // Verify document was created
        verify(documentRepository, times(1)).save(any(Document.class));
        
        // Verify text was chunked
        verify(textChunkingService, atLeastOnce()).chunkText(anyString());
        
        System.out.println("✓ Simple PDF file ingested successfully");
    }

    @Test
    @DisplayName("Ingest multi-page PDF - Success")
    void testIngestPdf_MultiPage_Success() throws IOException {
        System.out.println("Testing multi-page PDF ingestion...");
        
        // Create a multi-page PDF
        Path pdfFile = createMultiPagePdf();
        
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
        
        when(textChunkingService.chunkText(anyString())).thenReturn(
            java.util.List.of("Page 1 content", "Page 2 content", "Page 3 content")
        );
        
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[768]);
        
        when(embeddingRepository.save(any())).thenAnswer(inv -> {
            Embedding emb = inv.getArgument(0);
            emb.setId(UUID.randomUUID());
            return emb;
        });
        
        IngestionResult result = pdfIngestionService.ingestPdf(pdfFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        
        // Should process all pages
        verify(textChunkingService, atLeastOnce()).chunkText(anyString());
        
        System.out.println("✓ Multi-page PDF ingested successfully");
    }

    @Test
    @DisplayName("Ingest empty PDF - Handles gracefully")
    void testIngestPdf_EmptyFile_HandlesGracefully() throws IOException {
        System.out.println("Testing empty PDF handling...");
        
        // Create an empty PDF
        Path pdfFile = createEmptyPdf();
        
        UUID jobId = UUID.randomUUID();
        
        when(documentRepository.save(any())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        IngestionResult result = pdfIngestionService.ingestPdf(pdfFile.toString(), jobId);
        
        assertNotNull(result);
        
        // Document should still be created
        verify(documentRepository, times(1)).save(any(Document.class));
        
        System.out.println("✓ Empty PDF handled gracefully");
    }

    @Test
    @DisplayName("Ingest non-existent file - Error")
    void testIngestPdf_NonExistentFile_Error() {
        System.out.println("Testing non-existent file handling...");
        
        String nonExistentPath = tempDir.resolve("non-existent.pdf").toString();
        UUID jobId = UUID.randomUUID();
        
        IngestionResult result = pdfIngestionService.ingestPdf(nonExistentPath, jobId);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrorCount());
        
        verify(documentRepository, never()).save(any());
        
        System.out.println("✓ Non-existent file error handled correctly");
    }

    @Test
    @DisplayName("Ingest corrupted PDF - Error handling")
    void testIngestPdf_CorruptedFile_ErrorHandling() throws IOException {
        System.out.println("Testing corrupted PDF handling...");
        
        // Create a file that's not a valid PDF
        Path corruptedFile = tempDir.resolve("corrupted.pdf");
        java.nio.file.Files.writeString(corruptedFile, "This is not a valid PDF content");
        
        UUID jobId = UUID.randomUUID();
        
        IngestionResult result = pdfIngestionService.ingestPdf(corruptedFile.toString(), jobId);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Error") || result.getMessage().contains("Failed"));
        
        System.out.println("✓ Corrupted PDF error handled correctly");
    }

    @Test
    @DisplayName("PDF text extraction and chunking - Verification")
    void testIngestPdf_TextExtractionAndChunking() throws IOException {
        System.out.println("Testing PDF text extraction and chunking...");
        
        String longText = "This is a long text that should be chunked. " +
                         "It contains multiple sentences and paragraphs. " +
                         "The chunking service will split it into smaller pieces. " +
                         "Each chunk will be processed separately.";
        
        Path pdfFile = createTestPdf(longText);
        
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
        
        // Mock chunking to return 3 chunks
        when(textChunkingService.chunkText(anyString())).thenReturn(
            java.util.List.of(
                "This is a long text that should be chunked.",
                "It contains multiple sentences and paragraphs.",
                "The chunking service will split it into smaller pieces."
            )
        );
        
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[768]);
        
        when(embeddingRepository.save(any())).thenAnswer(inv -> {
            Embedding emb = inv.getArgument(0);
            emb.setId(UUID.randomUUID());
            return emb;
        });
        
        IngestionResult result = pdfIngestionService.ingestPdf(pdfFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        
        // Verify chunking was called
        verify(textChunkingService, times(1)).chunkText(anyString());
        
        // Verify nodes were created for chunks
        verify(nodeRepository, atLeast(3)).save(any(Node.class));
        
        // Verify embeddings were generated
        verify(embeddingService, atLeast(3)).generateEmbedding(anyString());
        
        System.out.println("✓ PDF text extraction and chunking verified");
    }

    @Test
    @DisplayName("PDF with metadata - Extraction")
    void testIngestPdf_WithMetadata_Extraction() throws IOException {
        System.out.println("Testing PDF with metadata extraction...");
        
        // Create PDF with metadata
        Path pdfFile = createPdfWithMetadata();
        
        UUID jobId = UUID.randomUUID();
        
        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        when(documentRepository.save(docCaptor.capture())).thenAnswer(inv -> {
            Document doc = inv.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });
        
        when(textChunkingService.chunkText(anyString())).thenReturn(
            java.util.List.of("Content from PDF")
        );
        
        when(nodeRepository.save(any())).thenAnswer(inv -> {
            Node node = inv.getArgument(0);
            node.setId(UUID.randomUUID());
            return node;
        });
        
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[768]);
        
        when(embeddingRepository.save(any())).thenAnswer(inv -> {
            Embedding emb = inv.getArgument(0);
            emb.setId(UUID.randomUUID());
            return emb;
        });
        
        IngestionResult result = pdfIngestionService.ingestPdf(pdfFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        
        Document savedDoc = docCaptor.getValue();
        assertNotNull(savedDoc);
        assertNotNull(savedDoc.getMetadata());
        
        System.out.println("✓ PDF metadata extracted successfully");
    }

    @Test
    @DisplayName("Embedding generation for PDF chunks - Verification")
    void testIngestPdf_EmbeddingGeneration() throws IOException {
        System.out.println("Testing embedding generation for PDF chunks...");
        
        Path pdfFile = createTestPdf("Test content for embedding");
        
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
        
        when(textChunkingService.chunkText(anyString())).thenReturn(
            java.util.List.of("Test content", "for embedding")
        );
        
        // Mock embedding generation
        float[] mockEmbedding = new float[768];
        for (int i = 0; i < 768; i++) {
            mockEmbedding[i] = (float) Math.random();
        }
        when(embeddingService.generateEmbedding(anyString())).thenReturn(mockEmbedding);
        
        ArgumentCaptor<Embedding> embeddingCaptor = ArgumentCaptor.forClass(Embedding.class);
        when(embeddingRepository.save(embeddingCaptor.capture())).thenAnswer(inv -> {
            Embedding emb = inv.getArgument(0);
            emb.setId(UUID.randomUUID());
            return emb;
        });
        
        IngestionResult result = pdfIngestionService.ingestPdf(pdfFile.toString(), jobId);
        
        assertTrue(result.isSuccess());
        
        // Verify embeddings were saved
        verify(embeddingRepository, atLeast(2)).save(any(Embedding.class));
        
        // Check embedding properties
        java.util.List<Embedding> savedEmbeddings = embeddingCaptor.getAllValues();
        for (Embedding embedding : savedEmbeddings) {
            assertNotNull(embedding.getEmbeddingVector());
            assertEquals(768, embedding.getEmbeddingVector().length);
            assertNotNull(embedding.getSourceText());
        }
        
        System.out.println("✓ Embeddings generated and saved correctly");
    }

    // Helper methods to create test PDFs
    
    private Path createTestPdf(String content) throws IOException {
        Path pdfFile = tempDir.resolve("test.pdf");
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(50, 700);
                
                String[] lines = content.split("\n");
                for (String line : lines) {
                    contentStream.showText(line);
                    contentStream.newLine();
                }
                
                contentStream.endText();
            }
            
            document.save(pdfFile.toFile());
        }
        
        return pdfFile;
    }
    
    private Path createMultiPagePdf() throws IOException {
        Path pdfFile = tempDir.resolve("multipage.pdf");
        
        try (PDDocument document = new PDDocument()) {
            for (int i = 1; i <= 3; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.newLineAtOffset(50, 700);
                    contentStream.showText("This is page " + i);
                    contentStream.endText();
                }
            }
            
            document.save(pdfFile.toFile());
        }
        
        return pdfFile;
    }
    
    private Path createEmptyPdf() throws IOException {
        Path pdfFile = tempDir.resolve("empty.pdf");
        
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdfFile.toFile());
        }
        
        return pdfFile;
    }
    
    private Path createPdfWithMetadata() throws IOException {
        Path pdfFile = tempDir.resolve("metadata.pdf");
        
        try (PDDocument document = new PDDocument()) {
            // Set document metadata
            document.getDocumentInformation().setTitle("Test PDF Document");
            document.getDocumentInformation().setAuthor("Test Author");
            document.getDocumentInformation().setSubject("Test Subject");
            document.getDocumentInformation().setKeywords("test, pdf, metadata");
            
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Document with metadata");
                contentStream.endText();
            }
            
            document.save(pdfFile.toFile());
        }
        
        return pdfFile;
    }
}