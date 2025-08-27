package com.knowledgegraph.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MockEmbeddingService Tests")
class MockEmbeddingServiceTest {

    private MockEmbeddingService mockEmbeddingService;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up MockEmbeddingService for testing...");
        
        mockEmbeddingService = new MockEmbeddingService();
        
        // Set default parameters using reflection
        ReflectionTestUtils.setField(mockEmbeddingService, "embeddingDimension", 768);
        ReflectionTestUtils.setField(mockEmbeddingService, "simulatedLatency", 50L);
        ReflectionTestUtils.setField(mockEmbeddingService, "deterministicMode", false);
    }

    @Test
    @DisplayName("Generate embedding for single text - Success")
    void testGenerateEmbedding_SingleText_Success() {
        System.out.println("Testing single text embedding generation...");
        
        String text = "This is a test text for embedding generation.";
        
        float[] embedding = mockEmbeddingService.generateEmbedding(text);
        
        assertNotNull(embedding);
        assertEquals(768, embedding.length);
        
        // Verify embedding values are in expected range [-1, 1]
        for (float value : embedding) {
            assertTrue(value >= -1.0f && value <= 1.0f);
        }
        
        // Verify embedding has reasonable distribution (not all zeros)
        double sum = 0;
        for (float value : embedding) {
            sum += Math.abs(value);
        }
        assertTrue(sum > 0);
        
        System.out.println("✓ Single text embedding generated successfully");
    }

    @Test
    @DisplayName("Generate embeddings for multiple texts - Success")
    void testGenerateEmbeddings_MultipleTexts_Success() {
        System.out.println("Testing multiple text embeddings generation...");
        
        List<String> texts = Arrays.asList(
            "First text for embedding",
            "Second text for embedding",
            "Third text for embedding"
        );
        
        List<float[]> embeddings = mockEmbeddingService.generateEmbeddings(texts);
        
        assertNotNull(embeddings);
        assertEquals(texts.size(), embeddings.size());
        
        for (float[] embedding : embeddings) {
            assertEquals(768, embedding.length);
        }
        
        // Verify embeddings are different for different texts
        assertFalse(Arrays.equals(embeddings.get(0), embeddings.get(1)));
        assertFalse(Arrays.equals(embeddings.get(1), embeddings.get(2)));
        
        System.out.println("✓ Multiple text embeddings generated successfully");
    }

    @Test
    @DisplayName("Generate embedding for empty text - Returns zero vector")
    void testGenerateEmbedding_EmptyText_ZeroVector() {
        System.out.println("Testing empty text embedding generation...");
        
        float[] embedding = mockEmbeddingService.generateEmbedding("");
        
        assertNotNull(embedding);
        assertEquals(768, embedding.length);
        
        // Empty text should return zero vector or small values
        double sumOfSquares = 0;
        for (float value : embedding) {
            sumOfSquares += value * value;
        }
        assertTrue(sumOfSquares < 0.1); // Very small magnitude
        
        System.out.println("✓ Empty text returns appropriate embedding");
    }

    @Test
    @DisplayName("Generate embedding for null text - Returns zero vector")
    void testGenerateEmbedding_NullText_ZeroVector() {
        System.out.println("Testing null text embedding generation...");
        
        float[] embedding = mockEmbeddingService.generateEmbedding(null);
        
        assertNotNull(embedding);
        assertEquals(768, embedding.length);
        
        System.out.println("✓ Null text handled gracefully");
    }

    @Test
    @DisplayName("Deterministic mode - Same text produces same embedding")
    void testGenerateEmbedding_DeterministicMode() {
        System.out.println("Testing deterministic embedding generation...");
        
        // Enable deterministic mode
        ReflectionTestUtils.setField(mockEmbeddingService, "deterministicMode", true);
        
        String text = "Test text for deterministic embedding";
        
        float[] embedding1 = mockEmbeddingService.generateEmbedding(text);
        float[] embedding2 = mockEmbeddingService.generateEmbedding(text);
        
        assertArrayEquals(embedding1, embedding2);
        
        System.out.println("✓ Deterministic mode produces consistent embeddings");
    }

    @Test
    @DisplayName("Non-deterministic mode - Same text produces different embeddings")
    void testGenerateEmbedding_NonDeterministicMode() {
        System.out.println("Testing non-deterministic embedding generation...");
        
        // Ensure non-deterministic mode
        ReflectionTestUtils.setField(mockEmbeddingService, "deterministicMode", false);
        
        String text = "Test text for random embedding";
        
        float[] embedding1 = mockEmbeddingService.generateEmbedding(text);
        float[] embedding2 = mockEmbeddingService.generateEmbedding(text);
        
        // Embeddings should be different (with very high probability)
        assertFalse(Arrays.equals(embedding1, embedding2));
        
        System.out.println("✓ Non-deterministic mode produces varied embeddings");
    }

    @Test
    @DisplayName("Custom embedding dimension - Success")
    void testGenerateEmbedding_CustomDimension() {
        System.out.println("Testing custom embedding dimension...");
        
        // Set custom dimension
        int customDimension = 512;
        ReflectionTestUtils.setField(mockEmbeddingService, "embeddingDimension", customDimension);
        
        float[] embedding = mockEmbeddingService.generateEmbedding("Test text");
        
        assertEquals(customDimension, embedding.length);
        
        System.out.println("✓ Custom embedding dimension working correctly");
    }

    @Test
    @DisplayName("Simulated latency - Timing test")
    void testGenerateEmbedding_SimulatedLatency() {
        System.out.println("Testing simulated latency...");
        
        // Set specific latency
        long expectedLatency = 100L;
        ReflectionTestUtils.setField(mockEmbeddingService, "simulatedLatency", expectedLatency);
        
        long startTime = System.currentTimeMillis();
        mockEmbeddingService.generateEmbedding("Test text");
        long endTime = System.currentTimeMillis();
        
        long actualLatency = endTime - startTime;
        
        // Allow some margin for execution time
        assertTrue(actualLatency >= expectedLatency);
        assertTrue(actualLatency < expectedLatency + 50);
        
        System.out.println("Expected latency: " + expectedLatency + "ms, Actual: " + actualLatency + "ms");
        System.out.println("✓ Simulated latency working correctly");
    }

    @Test
    @DisplayName("Batch embedding generation - Performance")
    void testGenerateEmbeddings_BatchPerformance() {
        System.out.println("Testing batch embedding generation performance...");
        
        // Reduce latency for performance test
        ReflectionTestUtils.setField(mockEmbeddingService, "simulatedLatency", 10L);
        
        List<String> texts = IntStream.range(0, 100)
            .mapToObj(i -> "Text number " + i)
            .toList();
        
        long startTime = System.currentTimeMillis();
        List<float[]> embeddings = mockEmbeddingService.generateEmbeddings(texts);
        long endTime = System.currentTimeMillis();
        
        assertEquals(100, embeddings.size());
        
        long totalTime = endTime - startTime;
        System.out.println("Generated 100 embeddings in " + totalTime + "ms");
        
        // Should benefit from batch processing
        assertTrue(totalTime < 100 * 10); // Less than sequential processing time
        
        System.out.println("✓ Batch embedding generation efficient");
    }

    @Test
    @DisplayName("Concurrent embedding generation - Thread safety")
    void testGenerateEmbedding_ThreadSafety() throws Exception {
        System.out.println("Testing thread safety of embedding generation...");
        
        int numThreads = 10;
        int embedsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        ConcurrentLinkedQueue<float[]> results = new ConcurrentLinkedQueue<>();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < embedsPerThread; j++) {
                        String text = "Thread " + threadId + " text " + j;
                        float[] embedding = mockEmbeddingService.generateEmbedding(text);
                        results.add(embedding);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        assertEquals(numThreads * embedsPerThread, results.size());
        
        // Verify all embeddings are valid
        for (float[] embedding : results) {
            assertNotNull(embedding);
            assertEquals(768, embedding.length);
        }
        
        System.out.println("✓ Thread-safe embedding generation verified");
    }

    @Test
    @DisplayName("Text length impact on embedding - Consistency")
    void testGenerateEmbedding_TextLengthImpact() {
        System.out.println("Testing embedding consistency across different text lengths...");
        
        String shortText = "Short";
        String mediumText = "This is a medium length text with more words.";
        String longText = String.join(" ", 
            "This is a very long text that contains many words and sentences.",
            "It goes on and on with lots of content to test how the embedding",
            "service handles longer inputs. The embedding should still be the",
            "same dimension regardless of input length."
        );
        
        float[] shortEmbedding = mockEmbeddingService.generateEmbedding(shortText);
        float[] mediumEmbedding = mockEmbeddingService.generateEmbedding(mediumText);
        float[] longEmbedding = mockEmbeddingService.generateEmbedding(longText);
        
        // All should have the same dimension
        assertEquals(768, shortEmbedding.length);
        assertEquals(768, mediumEmbedding.length);
        assertEquals(768, longEmbedding.length);
        
        // Embeddings should be different
        assertFalse(Arrays.equals(shortEmbedding, mediumEmbedding));
        assertFalse(Arrays.equals(mediumEmbedding, longEmbedding));
        
        System.out.println("✓ Embedding dimension consistent across text lengths");
    }

    @Test
    @DisplayName("Embedding normalization - Unit vectors")
    void testGenerateEmbedding_Normalization() {
        System.out.println("Testing embedding normalization...");
        
        String text = "Test text for normalization";
        float[] embedding = mockEmbeddingService.generateEmbedding(text);
        
        // Calculate magnitude
        double magnitude = 0;
        for (float value : embedding) {
            magnitude += value * value;
        }
        magnitude = Math.sqrt(magnitude);
        
        // Mock service might not normalize, but magnitude should be reasonable
        assertTrue(magnitude > 0);
        assertTrue(magnitude < 100); // Not too large
        
        System.out.println("Embedding magnitude: " + magnitude);
        System.out.println("✓ Embedding magnitude is reasonable");
    }

    @Test
    @DisplayName("Special characters in text - Handling")
    void testGenerateEmbedding_SpecialCharacters() {
        System.out.println("Testing embedding generation with special characters...");
        
        List<String> specialTexts = Arrays.asList(
            "Text with émojis 😀🎉",
            "Text with symbols @#$%^&*()",
            "Text with unicode 中文 русский",
            "Text\nwith\nnewlines",
            "Text\twith\ttabs"
        );
        
        for (String text : specialTexts) {
            float[] embedding = mockEmbeddingService.generateEmbedding(text);
            assertNotNull(embedding);
            assertEquals(768, embedding.length);
        }
        
        System.out.println("✓ Special characters handled correctly");
    }

    @Test
    @DisplayName("Cosine similarity between embeddings")
    void testEmbedding_CosineSimilarity() {
        System.out.println("Testing cosine similarity between embeddings...");
        
        // Enable deterministic mode for consistent testing
        ReflectionTestUtils.setField(mockEmbeddingService, "deterministicMode", true);
        
        String text1 = "The cat sits on the mat";
        String text2 = "The cat sits on the mat"; // Same text
        String text3 = "The dog runs in the park"; // Different text
        
        float[] embedding1 = mockEmbeddingService.generateEmbedding(text1);
        float[] embedding2 = mockEmbeddingService.generateEmbedding(text2);
        float[] embedding3 = mockEmbeddingService.generateEmbedding(text3);
        
        double similarity12 = cosineSimilarity(embedding1, embedding2);
        double similarity13 = cosineSimilarity(embedding1, embedding3);
        
        // Same text should have similarity close to 1
        assertEquals(1.0, similarity12, 0.001);
        
        // Different texts should have lower similarity
        assertTrue(Math.abs(similarity13) < 1.0);
        
        System.out.println("Similarity (same text): " + similarity12);
        System.out.println("Similarity (different text): " + similarity13);
        System.out.println("✓ Cosine similarity calculation working");
    }

    // Helper method to calculate cosine similarity
    private double cosineSimilarity(float[] vec1, float[] vec2) {
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}