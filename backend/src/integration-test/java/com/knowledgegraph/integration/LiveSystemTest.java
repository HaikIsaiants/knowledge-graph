package com.knowledgegraph.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

/**
 * Tests against the LIVE running application at localhost:8080
 * Run these tests while the application is running
 */
public class LiveSystemTest {

    private static final String BASE_URL = "http://localhost:8080/api";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void checkSystem() throws Exception {
        System.out.println("\n=== Checking if application is running ===");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/health"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Application should be running at " + BASE_URL);
        System.out.println("✓ Application is running");
    }

    @Test
    public void testFullFileUploadAndSearchFlow() throws Exception {
        System.out.println("\n=== Testing Full File Upload and Search Flow ===");

        // Step 1: Upload a CSV file
        System.out.println("1. Creating and uploading CSV file...");
        String csvContent = """
            name,type,description
            "Test Entity 1","ENTITY","This is a test entity for integration testing"
            "Test Concept 1","CONCEPT","This is a test concept about machine learning"
            "Test Organization","ORGANIZATION","A test organization entity"
            """;

        String boundary = "----" + UUID.randomUUID().toString();
        String multipartBody = "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"test.csv\"\r\n" +
                "Content-Type: text/csv\r\n\r\n" +
                csvContent + "\r\n" +
                "--" + boundary + "--\r\n";

        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/files/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(multipartBody))
                .build();

        HttpResponse<String> uploadResponse = client.send(uploadRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Upload response status: " + uploadResponse.statusCode());
        System.out.println("Upload response: " + uploadResponse.body());

        if (uploadResponse.statusCode() == 200) {
            Map<String, Object> uploadData = objectMapper.readValue(uploadResponse.body(), Map.class);
            System.out.println("✓ File uploaded successfully");
            System.out.println("  File ID: " + uploadData.get("fileId"));
            System.out.println("  Job ID: " + uploadData.get("jobId"));

            // Step 2: Wait for processing
            System.out.println("\n2. Waiting 5 seconds for processing...");
            Thread.sleep(5000);

            // Step 3: Search for the uploaded content
            System.out.println("\n3. Searching for uploaded content...");
            HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/search?q=machine+learning&page=0&size=10"))
                    .GET()
                    .build();

            HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Search response status: " + searchResponse.statusCode());
            
            if (searchResponse.statusCode() == 200) {
                Map<String, Object> searchData = objectMapper.readValue(searchResponse.body(), Map.class);
                System.out.println("✓ Search completed");
                System.out.println("  Total results: " + searchData.get("totalElements"));
            }
        }

        System.out.println("\n=== File Upload and Search Test Complete ===");
    }

    @Test
    public void testNodeOperations() throws Exception {
        System.out.println("\n=== Testing Node Operations ===");

        // Get nodes by type
        System.out.println("1. Getting nodes by type ENTITY...");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/nodes?type=ENTITY&page=0&size=10"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response status: " + response.statusCode());

        if (response.statusCode() == 200) {
            Map<String, Object> data = objectMapper.readValue(response.body(), Map.class);
            System.out.println("✓ Retrieved nodes");
            System.out.println("  Total nodes: " + data.get("totalElements"));
            
            // Get details of first node if exists
            var content = (java.util.List<?>) data.get("content");
            if (!content.isEmpty()) {
                Map<String, Object> firstNode = (Map<String, Object>) content.get(0);
                String nodeId = (String) firstNode.get("id");
                
                System.out.println("\n2. Getting details for node: " + nodeId);
                HttpRequest nodeRequest = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/nodes/" + nodeId))
                        .GET()
                        .build();

                HttpResponse<String> nodeResponse = client.send(nodeRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("Node detail response status: " + nodeResponse.statusCode());
                
                if (nodeResponse.statusCode() == 200) {
                    Map<String, Object> nodeData = objectMapper.readValue(nodeResponse.body(), Map.class);
                    System.out.println("✓ Retrieved node details");
                    System.out.println("  Node name: " + nodeData.get("name"));
                    System.out.println("  Node type: " + nodeData.get("type"));
                }
            }
        }

        System.out.println("\n=== Node Operations Test Complete ===");
    }

    @Test
    public void testGraphVisualization() throws Exception {
        System.out.println("\n=== Testing Graph Visualization ===");

        // Get graph stats
        System.out.println("1. Getting graph statistics...");
        HttpRequest statsRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/graph/stats"))
                .GET()
                .build();

        HttpResponse<String> statsResponse = client.send(statsRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Stats response status: " + statsResponse.statusCode());

        if (statsResponse.statusCode() == 200) {
            Map<String, Object> stats = objectMapper.readValue(statsResponse.body(), Map.class);
            System.out.println("✓ Retrieved graph statistics");
            System.out.println("  Total nodes: " + stats.get("totalNodes"));
            System.out.println("  Total edges: " + stats.get("totalEdges"));
        }

        // Try to get a graph neighborhood
        System.out.println("\n2. Testing graph neighborhood endpoint...");
        // First get any node
        HttpRequest nodesRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/nodes?page=0&size=1"))
                .GET()
                .build();

        HttpResponse<String> nodesResponse = client.send(nodesRequest, HttpResponse.BodyHandlers.ofString());
        if (nodesResponse.statusCode() == 200) {
            Map<String, Object> nodesData = objectMapper.readValue(nodesResponse.body(), Map.class);
            var content = (java.util.List<?>) nodesData.get("content");
            if (!content.isEmpty()) {
                Map<String, Object> node = (Map<String, Object>) content.get(0);
                String nodeId = (String) node.get("id");
                
                HttpRequest graphRequest = HttpRequest.newBuilder()
                        .uri(URI.create(BASE_URL + "/graph/neighborhood/" + nodeId + "?depth=2"))
                        .GET()
                        .build();

                HttpResponse<String> graphResponse = client.send(graphRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("Graph neighborhood response status: " + graphResponse.statusCode());
                
                if (graphResponse.statusCode() == 200) {
                    Map<String, Object> graphData = objectMapper.readValue(graphResponse.body(), Map.class);
                    System.out.println("✓ Retrieved graph neighborhood");
                    var nodes = (java.util.List<?>) graphData.get("nodes");
                    var edges = (java.util.List<?>) graphData.get("edges");
                    System.out.println("  Nodes in neighborhood: " + (nodes != null ? nodes.size() : 0));
                    System.out.println("  Edges in neighborhood: " + (edges != null ? edges.size() : 0));
                }
            }
        }

        System.out.println("\n=== Graph Visualization Test Complete ===");
    }

    @Test
    public void testSearchFunctionality() throws Exception {
        System.out.println("\n=== Testing Search Functionality ===");

        // Test basic search
        System.out.println("1. Testing basic search...");
        HttpRequest searchRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/search?q=test&page=0&size=10"))
                .GET()
                .build();

        HttpResponse<String> searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Search response status: " + searchResponse.statusCode());

        if (searchResponse.statusCode() == 200) {
            Map<String, Object> searchData = objectMapper.readValue(searchResponse.body(), Map.class);
            System.out.println("✓ Search executed successfully");
            System.out.println("  Total results: " + searchData.get("totalElements"));
            System.out.println("  Current page: " + searchData.get("currentPage"));
        }

        // Test search with type filter
        System.out.println("\n2. Testing search with type filter...");
        HttpRequest filteredSearch = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/search?q=test&type=ENTITY&page=0&size=10"))
                .GET()
                .build();

        HttpResponse<String> filteredResponse = client.send(filteredSearch, HttpResponse.BodyHandlers.ofString());
        System.out.println("Filtered search response status: " + filteredResponse.statusCode());

        if (filteredResponse.statusCode() == 200) {
            System.out.println("✓ Filtered search executed successfully");
        }

        System.out.println("\n=== Search Functionality Test Complete ===");
    }

    @Test
    public void testErrorHandling() throws Exception {
        System.out.println("\n=== Testing Error Handling ===");

        // Test invalid endpoint
        System.out.println("1. Testing non-existent endpoint...");
        HttpRequest badRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/nonexistent"))
                .GET()
                .build();

        HttpResponse<String> badResponse = client.send(badRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("Non-existent endpoint status: " + badResponse.statusCode());
        assertTrue(badResponse.statusCode() >= 400, "Should return error status");
        System.out.println("✓ Non-existent endpoint properly returns error");

        // Test invalid node ID
        System.out.println("\n2. Testing invalid node ID...");
        HttpRequest invalidNode = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/nodes/00000000-0000-0000-0000-000000000000"))
                .GET()
                .build();

        HttpResponse<String> invalidResponse = client.send(invalidNode, HttpResponse.BodyHandlers.ofString());
        System.out.println("Invalid node status: " + invalidResponse.statusCode());
        assertEquals(404, invalidResponse.statusCode(), "Should return 404 for non-existent node");
        System.out.println("✓ Non-existent node properly returns 404");

        // Test empty search
        System.out.println("\n3. Testing empty search query...");
        HttpRequest emptySearch = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/search?q=&page=0&size=10"))
                .GET()
                .build();

        HttpResponse<String> emptyResponse = client.send(emptySearch, HttpResponse.BodyHandlers.ofString());
        System.out.println("Empty search status: " + emptyResponse.statusCode());
        assertEquals(400, emptyResponse.statusCode(), "Should return 400 for empty search");
        System.out.println("✓ Empty search properly returns 400");

        System.out.println("\n=== Error Handling Test Complete ===");
    }
}