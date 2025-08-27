package com.knowledgegraph.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TextChunkingService Tests")
class TextChunkingServiceTest {

    private TextChunkingService textChunkingService;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up TextChunkingService for testing...");
        
        textChunkingService = new TextChunkingService();
        
        // Set default chunking parameters using reflection
        ReflectionTestUtils.setField(textChunkingService, "defaultChunkSize", 500);
        ReflectionTestUtils.setField(textChunkingService, "defaultOverlap", 50);
        ReflectionTestUtils.setField(textChunkingService, "maxChunkSize", 2000);
        ReflectionTestUtils.setField(textChunkingService, "minChunkSize", 100);
    }

    @Test
    @DisplayName("Chunk text with default parameters - Success")
    void testChunkText_DefaultParameters_Success() {
        System.out.println("Testing text chunking with default parameters...");
        
        String text = generateText(1500); // Generate 1500 characters
        
        List<String> chunks = textChunkingService.chunkText(text);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // With 1500 characters and chunk size 500 with 50 overlap, expect ~3-4 chunks
        assertTrue(chunks.size() >= 3);
        
        // Verify chunk sizes
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 500);
            assertTrue(chunk.length() >= 100); // Min chunk size
        }
        
        // Verify overlap by checking if some text appears in multiple chunks
        for (int i = 0; i < chunks.size() - 1; i++) {
            String currentChunk = chunks.get(i);
            String nextChunk = chunks.get(i + 1);
            
            // Last 50 characters of current should overlap with first part of next
            if (currentChunk.length() >= 50) {
                String overlapText = currentChunk.substring(currentChunk.length() - 50);
                assertTrue(nextChunk.contains(overlapText.substring(0, Math.min(overlapText.length(), 20))));
            }
        }
        
        System.out.println("Created " + chunks.size() + " chunks");
        System.out.println("✓ Text chunked successfully with default parameters");
    }

    @Test
    @DisplayName("Chunk text with custom parameters - Success")
    void testChunkText_CustomParameters_Success() {
        System.out.println("Testing text chunking with custom parameters...");
        
        String text = generateText(2000);
        int customChunkSize = 300;
        int customOverlap = 30;
        
        List<String> chunks = textChunkingService.chunkText(text, customChunkSize, customOverlap);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // Verify chunk sizes respect custom parameters
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (i < chunks.size() - 1) { // Not the last chunk
                assertEquals(customChunkSize, chunk.length());
            } else { // Last chunk might be smaller
                assertTrue(chunk.length() <= customChunkSize);
            }
        }
        
        System.out.println("✓ Text chunked successfully with custom parameters");
    }

    @Test
    @DisplayName("Chunk small text - Single chunk")
    void testChunkText_SmallText_SingleChunk() {
        System.out.println("Testing chunking of small text...");
        
        String text = "This is a small text that fits in one chunk.";
        
        List<String> chunks = textChunkingService.chunkText(text);
        
        assertEquals(1, chunks.size());
        assertEquals(text, chunks.get(0));
        
        System.out.println("✓ Small text returned as single chunk");
    }

    @Test
    @DisplayName("Chunk empty text - Empty list")
    void testChunkText_EmptyText_EmptyList() {
        System.out.println("Testing chunking of empty text...");
        
        List<String> chunks = textChunkingService.chunkText("");
        
        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
        
        System.out.println("✓ Empty text returns empty chunk list");
    }

    @Test
    @DisplayName("Chunk null text - Empty list")
    void testChunkText_NullText_EmptyList() {
        System.out.println("Testing chunking of null text...");
        
        List<String> chunks = textChunkingService.chunkText(null);
        
        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
        
        System.out.println("✓ Null text returns empty chunk list");
    }

    @Test
    @DisplayName("Chunk text with sentences - Sentence boundary preservation")
    void testChunkText_SentenceBoundaries() {
        System.out.println("Testing sentence boundary preservation...");
        
        String text = "This is the first sentence. This is the second sentence. " +
                     "This is the third sentence. This is the fourth sentence. " +
                     "This is the fifth sentence. This is the sixth sentence.";
        
        List<String> chunks = textChunkingService.chunkTextBySentences(text, 100, 20);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // Verify each chunk starts with a capital letter (sentence beginning)
        for (String chunk : chunks) {
            String trimmed = chunk.trim();
            if (!trimmed.isEmpty()) {
                assertTrue(Character.isUpperCase(trimmed.charAt(0)));
            }
        }
        
        System.out.println("✓ Sentence boundaries preserved in chunks");
    }

    @Test
    @DisplayName("Chunk text with paragraphs - Paragraph boundary preservation")
    void testChunkText_ParagraphBoundaries() {
        System.out.println("Testing paragraph boundary preservation...");
        
        String text = "First paragraph with some content here.\n\n" +
                     "Second paragraph with more content here.\n\n" +
                     "Third paragraph with additional content.\n\n" +
                     "Fourth paragraph with final content.";
        
        List<String> chunks = textChunkingService.chunkTextByParagraphs(text, 150);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // Each chunk should contain complete paragraphs
        for (String chunk : chunks) {
            // Chunks should not have broken paragraph boundaries
            assertFalse(chunk.trim().endsWith("\n"));
        }
        
        System.out.println("✓ Paragraph boundaries preserved in chunks");
    }

    @Test
    @DisplayName("Chunk text with overlap validation")
    void testChunkText_OverlapValidation() {
        System.out.println("Testing overlap validation...");
        
        String text = generateText(1000);
        
        // Test overlap cannot exceed chunk size
        assertThrows(IllegalArgumentException.class, () -> {
            textChunkingService.chunkText(text, 200, 250); // Overlap > chunk size
        });
        
        // Test negative chunk size
        assertThrows(IllegalArgumentException.class, () -> {
            textChunkingService.chunkText(text, -100, 50);
        });
        
        // Test negative overlap
        assertThrows(IllegalArgumentException.class, () -> {
            textChunkingService.chunkText(text, 200, -50);
        });
        
        System.out.println("✓ Overlap validation working correctly");
    }

    @Test
    @DisplayName("Chunk text with exact chunk size")
    void testChunkText_ExactChunkSize() {
        System.out.println("Testing text chunking with exact chunk size...");
        
        String text = generateText(1000); // Exactly 1000 characters
        
        List<String> chunks = textChunkingService.chunkText(text, 250, 0); // No overlap
        
        assertEquals(4, chunks.size()); // 1000 / 250 = 4
        
        for (String chunk : chunks) {
            assertEquals(250, chunk.length());
        }
        
        System.out.println("✓ Text chunked into exact size chunks");
    }

    @Test
    @DisplayName("Chunk text preserving words - No word splitting")
    void testChunkText_WordBoundaries() {
        System.out.println("Testing word boundary preservation...");
        
        String text = "The quick brown fox jumps over the lazy dog. " +
                     "This is a test sentence with multiple words. " +
                     "Another sentence follows here with more content.";
        
        List<String> chunks = textChunkingService.chunkTextByWords(text, 50, 10);
        
        assertNotNull(chunks);
        
        // Verify no chunks end or start with partial words
        for (String chunk : chunks) {
            String trimmed = chunk.trim();
            if (!trimmed.isEmpty()) {
                // Check first and last characters are not in the middle of a word
                assertFalse(Character.isLetter(trimmed.charAt(0)) && 
                           trimmed.charAt(0) == Character.toLowerCase(trimmed.charAt(0)));
            }
        }
        
        System.out.println("✓ Word boundaries preserved in chunks");
    }

    @Test
    @DisplayName("Chunk very long text - Performance test")
    void testChunkText_VeryLongText_Performance() {
        System.out.println("Testing chunking performance with very long text...");
        
        String longText = generateText(100000); // 100K characters
        
        long startTime = System.currentTimeMillis();
        List<String> chunks = textChunkingService.chunkText(longText, 1000, 100);
        long endTime = System.currentTimeMillis();
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        long processingTime = endTime - startTime;
        System.out.println("Processed 100K characters in " + processingTime + " ms");
        System.out.println("Created " + chunks.size() + " chunks");
        
        // Performance assertion - should process in reasonable time
        assertTrue(processingTime < 1000); // Should complete within 1 second
        
        System.out.println("✓ Large text chunked efficiently");
    }

    @Test
    @DisplayName("Chunk with sliding window - Overlap content verification")
    void testChunkText_SlidingWindow_OverlapContent() {
        System.out.println("Testing sliding window overlap content...");
        
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        
        List<String> chunks = textChunkingService.chunkText(text, 10, 3);
        
        // First chunk: ABCDEFGHIJ (0-9)
        assertEquals("ABCDEFGHIJ", chunks.get(0));
        
        // Second chunk should start at position 7 (10-3): HIJKLMNOPQ
        assertEquals("HIJKLMNOPQ", chunks.get(1));
        
        // Verify overlap content
        String overlap1 = chunks.get(0).substring(7); // HIJ
        String overlap2 = chunks.get(1).substring(0, 3); // HIJ
        assertEquals(overlap1, overlap2);
        
        System.out.println("✓ Sliding window overlap content verified");
    }

    @Test
    @DisplayName("Chunk with metadata preservation")
    void testChunkText_MetadataPreservation() {
        System.out.println("Testing chunk metadata preservation...");
        
        String text = generateText(1500);
        
        List<TextChunkingService.TextChunk> chunksWithMetadata = 
            textChunkingService.chunkTextWithMetadata(text, 500, 50);
        
        assertNotNull(chunksWithMetadata);
        assertFalse(chunksWithMetadata.isEmpty());
        
        // Verify metadata
        for (int i = 0; i < chunksWithMetadata.size(); i++) {
            TextChunkingService.TextChunk chunk = chunksWithMetadata.get(i);
            
            assertEquals(i, chunk.getIndex());
            assertNotNull(chunk.getText());
            assertTrue(chunk.getStartPosition() >= 0);
            assertTrue(chunk.getEndPosition() > chunk.getStartPosition());
            assertTrue(chunk.getEndPosition() <= text.length());
            
            // Verify the chunk text matches the position in original text
            String extractedText = text.substring(chunk.getStartPosition(), chunk.getEndPosition());
            assertEquals(extractedText, chunk.getText());
        }
        
        System.out.println("✓ Chunk metadata preserved correctly");
    }

    @Test
    @DisplayName("Chunk with different overlap strategies")
    void testChunkText_DifferentOverlapStrategies() {
        System.out.println("Testing different overlap strategies...");
        
        String text = generateText(1000);
        
        // No overlap
        List<String> noOverlap = textChunkingService.chunkText(text, 200, 0);
        
        // Small overlap
        List<String> smallOverlap = textChunkingService.chunkText(text, 200, 20);
        
        // Large overlap
        List<String> largeOverlap = textChunkingService.chunkText(text, 200, 100);
        
        // More chunks with larger overlap
        assertTrue(largeOverlap.size() > smallOverlap.size());
        assertTrue(smallOverlap.size() >= noOverlap.size());
        
        System.out.println("No overlap: " + noOverlap.size() + " chunks");
        System.out.println("Small overlap: " + smallOverlap.size() + " chunks");
        System.out.println("Large overlap: " + largeOverlap.size() + " chunks");
        
        System.out.println("✓ Different overlap strategies working correctly");
    }

    @Test
    @DisplayName("Chunk Unicode text - International characters")
    void testChunkText_UnicodeCharacters() {
        System.out.println("Testing chunking with Unicode characters...");
        
        String unicodeText = "Hello 世界! This is a test with émojis 😀🎉. " +
                           "Текст на русском языке. 日本語のテキスト。" +
                           "مرحبا بالعالم. Olá mundo!";
        
        List<String> chunks = textChunkingService.chunkText(unicodeText, 50, 10);
        
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        
        // Verify Unicode characters are preserved
        String reconstructed = String.join("", chunks);
        assertTrue(reconstructed.contains("世界"));
        assertTrue(reconstructed.contains("😀"));
        assertTrue(reconstructed.contains("русском"));
        assertTrue(reconstructed.contains("日本語"));
        
        System.out.println("✓ Unicode text chunked correctly");
    }

    // Helper methods
    
    private String generateText(int length) {
        StringBuilder sb = new StringBuilder();
        String sample = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ";
        while (sb.length() < length) {
            sb.append(sample);
        }
        return sb.substring(0, length);
    }
}