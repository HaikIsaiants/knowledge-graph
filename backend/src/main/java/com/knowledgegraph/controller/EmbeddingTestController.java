package com.knowledgegraph.controller;

import com.knowledgegraph.service.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for verifying embedding functionality
 */
@RestController
@RequestMapping("/test/embeddings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Embedding Test", description = "Test endpoints for embedding service")
public class EmbeddingTestController {
    
    private final EmbeddingService embeddingService;
    
    @PostMapping("/generate")
    @Operation(summary = "Generate embedding for test text", 
               description = "Generate and return embedding vector for provided text")
    public ResponseEntity<Map<String, Object>> generateEmbedding(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
        }
        
        log.info("Generating embedding for text: '{}'", 
                 text.length() > 50 ? text.substring(0, 50) + "..." : text);
        
        try {
            // Generate embedding
            float[] embedding = embeddingService.generateEmbedding(text);
            
            // Calculate some statistics
            double sum = 0;
            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            
            for (float val : embedding) {
                sum += val;
                min = Math.min(min, val);
                max = Math.max(max, val);
            }
            
            double mean = sum / embedding.length;
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("text", text);
            response.put("dimensions", embedding.length);
            response.put("model", embeddingService.getModelVersion());
            response.put("statistics", Map.of(
                "mean", mean,
                "min", min,
                "max", max,
                "first10", getFirst10(embedding)
            ));
            
            // If you want to see the full vector (warning: large!)
            if (request.containsKey("includeVector") && "true".equals(request.get("includeVector"))) {
                response.put("vector", embedding);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error generating embedding: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to generate embedding: " + e.getMessage()));
        }
    }
    
    @PostMapping("/similarity")
    @Operation(summary = "Calculate similarity between two texts", 
               description = "Generate embeddings for two texts and calculate cosine similarity")
    public ResponseEntity<Map<String, Object>> calculateSimilarity(@RequestBody Map<String, String> request) {
        String text1 = request.get("text1");
        String text2 = request.get("text2");
        
        if (text1 == null || text2 == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Both text1 and text2 are required"));
        }
        
        try {
            // Generate embeddings
            float[] embedding1 = embeddingService.generateEmbedding(text1);
            float[] embedding2 = embeddingService.generateEmbedding(text2);
            
            // Calculate cosine similarity
            double similarity = embeddingService.calculateSimilarity(embedding1, embedding2);
            
            Map<String, Object> response = new HashMap<>();
            response.put("text1", text1);
            response.put("text2", text2);
            response.put("similarity", similarity);
            response.put("similarityPercentage", String.format("%.2f%%", similarity * 100));
            response.put("interpretation", interpretSimilarity(similarity));
            response.put("model", embeddingService.getModelVersion());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error calculating similarity: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to calculate similarity: " + e.getMessage()));
        }
    }
    
    @GetMapping("/status")
    @Operation(summary = "Check embedding service status", 
               description = "Verify if embedding service is configured and working")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Try to generate a test embedding
            float[] testEmbedding = embeddingService.generateEmbedding("test");
            
            status.put("status", "operational");
            status.put("model", embeddingService.getModelVersion());
            status.put("dimensions", testEmbedding.length);
            status.put("isOpenAI", !embeddingService.getModelVersion().contains("mock"));
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            status.put("status", "error");
            status.put("error", e.getMessage());
            status.put("isOpenAI", false);
            
            return ResponseEntity.ok(status);
        }
    }
    
    private float[] getFirst10(float[] array) {
        int length = Math.min(10, array.length);
        float[] result = new float[length];
        System.arraycopy(array, 0, result, 0, length);
        return result;
    }
    
    private String interpretSimilarity(double similarity) {
        if (similarity > 0.9) return "Very similar";
        if (similarity > 0.7) return "Similar";
        if (similarity > 0.5) return "Somewhat similar";
        if (similarity > 0.3) return "Slightly similar";
        return "Not similar";
    }
}