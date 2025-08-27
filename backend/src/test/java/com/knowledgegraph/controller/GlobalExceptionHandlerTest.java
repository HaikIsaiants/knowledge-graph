package com.knowledgegraph.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowledgegraph.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private SearchService searchService;
    
    @MockBean
    private com.knowledgegraph.service.VectorSearchService vectorSearchService;
    
    @MockBean
    private com.knowledgegraph.service.HybridSearchService hybridSearchService;
    
    @BeforeEach
    void setUp() {
        System.out.println("Setting up GlobalExceptionHandler test environment...");
    }
    
    @Test
    @DisplayName("Handle IllegalArgumentException")
    void testHandleIllegalArgumentException() throws Exception {
        System.out.println("Testing IllegalArgumentException handling...");
        
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenThrow(new IllegalArgumentException("Invalid query parameter"));
        
        mockMvc.perform(get("/search")
                .param("q", "test")
                .param("highlight", "true"))
            .andDo(print())
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Invalid query parameter"))
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.path").value("/search"));
        
        System.out.println("✓ IllegalArgumentException handled correctly");
    }
    
    @Test
    @DisplayName("Handle NullPointerException")
    void testHandleNullPointerException() throws Exception {
        System.out.println("Testing NullPointerException handling...");
        
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenThrow(new NullPointerException("Null value encountered"));
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("Null value encountered"));
        
        System.out.println("✓ NullPointerException handled correctly");
    }
    
    @Test
    @DisplayName("Handle DataAccessException")
    void testHandleDataAccessException() throws Exception {
        System.out.println("Testing DataAccessException handling...");
        
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenThrow(new DataAccessException("Database connection failed") {});
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.error").value("Service Unavailable"))
            .andExpect(jsonPath("$.message").value("Database error: Database connection failed"));
        
        System.out.println("✓ DataAccessException handled correctly");
    }
    
    @Test
    @DisplayName("Handle Generic RuntimeException")
    void testHandleRuntimeException() throws Exception {
        System.out.println("Testing generic RuntimeException handling...");
        
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenThrow(new RuntimeException("Unexpected error"));
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        
        System.out.println("✓ RuntimeException handled correctly");
    }
    
    @Test
    @DisplayName("Handle Method Argument Type Mismatch")
    void testHandleMethodArgumentTypeMismatch() throws Exception {
        System.out.println("Testing method argument type mismatch...");
        
        // Try to pass invalid UUID format
        mockMvc.perform(get("/search/similar/not-a-valid-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
        
        System.out.println("✓ Method argument type mismatch handled correctly");
    }
    
    @Test
    @DisplayName("Handle Missing Required Parameter")
    void testHandleMissingRequiredParameter() throws Exception {
        System.out.println("Testing missing required parameter...");
        
        // Missing required 'q' parameter
        mockMvc.perform(get("/search"))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Missing required parameter handled correctly");
    }
    
    @Test
    @DisplayName("Handle Invalid Request Body")
    void testHandleInvalidRequestBody() throws Exception {
        System.out.println("Testing invalid request body...");
        
        mockMvc.perform(post("/some-post-endpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("invalid json {"))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Invalid request body handled correctly");
    }
    
    @Test
    @DisplayName("Handle Resource Not Found")
    void testHandleResourceNotFound() throws Exception {
        System.out.println("Testing resource not found...");
        
        UUID nodeId = UUID.randomUUID();
        when(vectorSearchService.findSimilarNodes(any(UUID.class), any()))
            .thenThrow(new IllegalArgumentException("Node not found: " + nodeId));
        
        mockMvc.perform(get("/search/similar/{nodeId}", nodeId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Node not found: " + nodeId));
        
        System.out.println("✓ Resource not found handled correctly");
    }
    
    @Test
    @DisplayName("Handle Custom Business Exception")
    void testHandleCustomBusinessException() throws Exception {
        System.out.println("Testing custom business exception...");
        
        when(hybridSearchService.hybridSearch(anyString(), any(), any(), any()))
            .thenThrow(new IllegalStateException("Invalid weight configuration"));
        
        mockMvc.perform(get("/search/hybrid")
                .param("q", "test")
                .param("ftsWeight", "2.0")
                .param("vectorWeight", "-1.0"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Invalid weight configuration"));
        
        System.out.println("✓ Custom business exception handled correctly");
    }
    
    @Test
    @DisplayName("Error response includes all required fields")
    void testErrorResponseStructure() throws Exception {
        System.out.println("Testing error response structure...");
        
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenThrow(new IllegalArgumentException("Test error"));
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.status").isNumber())
            .andExpect(jsonPath("$.error").isString())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.path").isString());
        
        System.out.println("✓ Error response structure is complete");
    }
    
    @Test
    @DisplayName("Handle validation errors properly")
    void testHandleValidationErrors() throws Exception {
        System.out.println("Testing validation error handling...");
        
        // Test with invalid page size (negative)
        mockMvc.perform(get("/search")
                .param("q", "test")
                .param("page", "-1"))
            .andExpect(status().isOk()); // Spring doesn't validate page by default
        
        // Test with invalid type enum value
        mockMvc.perform(get("/search")
                .param("q", "test")
                .param("type", "INVALID_TYPE"))
            .andExpect(status().isBadRequest());
        
        System.out.println("✓ Validation errors handled correctly");
    }
    
    @Test
    @DisplayName("Handle concurrent modification exception")
    void testHandleConcurrentModificationException() throws Exception {
        System.out.println("Testing concurrent modification exception...");
        
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenThrow(new java.util.ConcurrentModificationException("Collection modified during iteration"));
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Collection modified during iteration"));
        
        System.out.println("✓ Concurrent modification exception handled correctly");
    }
    
    @Test
    @DisplayName("Handle timeout exception")
    void testHandleTimeoutException() throws Exception {
        System.out.println("Testing timeout exception...");
        
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenThrow(new java.util.concurrent.TimeoutException("Request timeout"));
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isInternalServerError());
        
        System.out.println("✓ Timeout exception handled correctly");
    }
    
    @Test
    @DisplayName("Verify error messages are sanitized")
    void testErrorMessageSanitization() throws Exception {
        System.out.println("Testing error message sanitization...");
        
        // Throw exception with potentially sensitive information
        String sensitiveMessage = "Database error: password=secret123, user=admin";
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenThrow(new RuntimeException(sensitiveMessage));
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
            .andExpect(jsonPath("$.message", not(containsString("password"))))
            .andExpect(jsonPath("$.message", not(containsString("secret123"))));
        
        System.out.println("✓ Sensitive information properly sanitized");
    }
    
    @Test
    @DisplayName("Handle stack overflow error gracefully")
    void testHandleStackOverflowError() throws Exception {
        System.out.println("Testing stack overflow error handling...");
        
        when(searchService.searchWithHighlight(anyString(), any()))
            .thenAnswer(invocation -> {
                throw new StackOverflowError("Stack overflow in recursive method");
            });
        
        mockMvc.perform(get("/search")
                .param("q", "test"))
            .andExpect(status().isInternalServerError());
        
        System.out.println("✓ Stack overflow error handled gracefully");
    }
}