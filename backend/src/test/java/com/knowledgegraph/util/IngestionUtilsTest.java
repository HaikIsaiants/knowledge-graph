package com.knowledgegraph.util;

import com.knowledgegraph.model.NodeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IngestionUtils Tests")
class IngestionUtilsTest {

    @Test
    @DisplayName("Generate hash from string - Success")
    void testGenerateHash_String_Success() {
        System.out.println("Testing string hash generation...");
        
        String content = "Test content for hashing";
        String hash = IngestionUtils.generateHash(content);
        
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex characters
        assertTrue(hash.matches("[a-f0-9]+"));
        
        // Test consistency
        String hash2 = IngestionUtils.generateHash(content);
        assertEquals(hash, hash2);
        
        System.out.println("Generated hash: " + hash);
        System.out.println("✓ String hash generated successfully");
    }

    @Test
    @DisplayName("Generate hash from bytes - Success")
    void testGenerateHash_Bytes_Success() {
        System.out.println("Testing byte array hash generation...");
        
        byte[] content = "Test content for hashing".getBytes(StandardCharsets.UTF_8);
        String hash = IngestionUtils.generateHash(content);
        
        assertNotNull(hash);
        assertEquals(64, hash.length());
        
        // Compare with string version
        String stringHash = IngestionUtils.generateHash("Test content for hashing");
        assertEquals(stringHash, hash);
        
        System.out.println("✓ Byte array hash generated successfully");
    }

    @Test
    @DisplayName("Generate hash for empty string")
    void testGenerateHash_EmptyString() {
        System.out.println("Testing empty string hash generation...");
        
        String hash = IngestionUtils.generateHash("");
        
        assertNotNull(hash);
        assertEquals(64, hash.length());
        // Known SHA-256 hash for empty string
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
        
        System.out.println("✓ Empty string hash generated correctly");
    }

    @Test
    @DisplayName("Generate hash for null bytes")
    void testGenerateHash_NullBytes() {
        System.out.println("Testing null byte array hash generation...");
        
        assertThrows(NullPointerException.class, () -> {
            IngestionUtils.generateHash((byte[]) null);
        });
        
        System.out.println("✓ Null byte array handled correctly");
    }

    @Test
    @DisplayName("Hash uniqueness for different content")
    void testGenerateHash_Uniqueness() {
        System.out.println("Testing hash uniqueness...");
        
        String hash1 = IngestionUtils.generateHash("Content 1");
        String hash2 = IngestionUtils.generateHash("Content 2");
        String hash3 = IngestionUtils.generateHash("Content 1"); // Same as first
        
        assertNotEquals(hash1, hash2);
        assertEquals(hash1, hash3);
        
        System.out.println("✓ Hash uniqueness verified");
    }

    @Test
    @DisplayName("Extract preview - Normal text")
    void testExtractPreview_NormalText() {
        System.out.println("Testing preview extraction from normal text...");
        
        String content = "This is a test content for preview extraction.";
        String preview = IngestionUtils.extractPreview(content, 20);
        
        assertNotNull(preview);
        assertTrue(preview.length() <= 20 + 3); // +3 for "..."
        assertEquals("This is a test...", preview);
        
        System.out.println("Preview: " + preview);
        System.out.println("✓ Preview extracted successfully");
    }

    @Test
    @DisplayName("Extract preview - Text with sentence boundary")
    void testExtractPreview_SentenceBoundary() {
        System.out.println("Testing preview extraction at sentence boundary...");
        
        String content = "First sentence. Second sentence. Third sentence.";
        String preview = IngestionUtils.extractPreview(content, 30);
        
        assertEquals("First sentence.", preview);
        
        System.out.println("✓ Preview extracted at sentence boundary");
    }

    @Test
    @DisplayName("Extract preview - Short content")
    void testExtractPreview_ShortContent() {
        System.out.println("Testing preview extraction for short content...");
        
        String content = "Short text";
        String preview = IngestionUtils.extractPreview(content, 100);
        
        assertEquals(content, preview);
        assertFalse(preview.contains("..."));
        
        System.out.println("✓ Short content preview handled correctly");
    }

    @Test
    @DisplayName("Extract preview - Empty content")
    void testExtractPreview_EmptyContent() {
        System.out.println("Testing preview extraction for empty content...");
        
        String preview1 = IngestionUtils.extractPreview("", 50);
        String preview2 = IngestionUtils.extractPreview(null, 50);
        String preview3 = IngestionUtils.extractPreview("   ", 50);
        
        assertEquals("", preview1);
        assertEquals("", preview2);
        assertEquals("", preview3);
        
        System.out.println("✓ Empty content handled correctly");
    }

    @Test
    @DisplayName("Extract preview - Long text with word boundary")
    void testExtractPreview_WordBoundary() {
        System.out.println("Testing preview extraction at word boundary...");
        
        String content = "This is a very long text that needs to be truncated at a word boundary";
        String preview = IngestionUtils.extractPreview(content, 25);
        
        assertTrue(preview.endsWith("..."));
        assertFalse(preview.contains("truncat")); // Should not break mid-word
        
        System.out.println("Preview: " + preview);
        System.out.println("✓ Preview extracted at word boundary");
    }

    @Test
    @DisplayName("Extract preview - Text with no spaces")
    void testExtractPreview_NoSpaces() {
        System.out.println("Testing preview extraction for text without spaces...");
        
        String content = "Verylongtextwithoutanyspacesatallinthiscontent";
        String preview = IngestionUtils.extractPreview(content, 20);
        
        assertEquals("Verylongtextwithouta...", preview);
        
        System.out.println("✓ Text without spaces handled correctly");
    }

    @Test
    @DisplayName("Extract preview - Multiline text")
    void testExtractPreview_MultilineText() {
        System.out.println("Testing preview extraction for multiline text...");
        
        String content = "Line 1\nLine 2\nLine 3. This is a longer line.";
        String preview = IngestionUtils.extractPreview(content, 40);
        
        assertNotNull(preview);
        assertTrue(preview.contains("Line 1"));
        
        System.out.println("Preview: " + preview.replace("\n", "\\n"));
        System.out.println("✓ Multiline text preview extracted");
    }

    @Test
    @DisplayName("Determine node type - PERSON")
    void testDetermineNodeType_Person() {
        System.out.println("Testing PERSON node type determination...");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("firstName", "John");
        properties.put("lastName", "Doe");
        properties.put("email", "john@example.com");
        
        NodeType type = IngestionUtils.determineNodeType(properties);
        assertEquals(NodeType.PERSON, type);
        
        // Test with just email
        properties.clear();
        properties.put("email", "test@test.com");
        type = IngestionUtils.determineNodeType(properties);
        assertEquals(NodeType.PERSON, type);
        
        // Test with role
        properties.clear();
        properties.put("role", "Developer");
        type = IngestionUtils.determineNodeType(properties);
        assertEquals(NodeType.PERSON, type);
        
        System.out.println("✓ PERSON node type determined correctly");
    }

    @Test
    @DisplayName("Determine node type - ORGANIZATION")
    void testDetermineNodeType_Organization() {
        System.out.println("Testing ORGANIZATION node type determination...");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("organization", "Acme Corp");
        NodeType type = IngestionUtils.determineNodeType(properties);
        assertEquals(NodeType.ORGANIZATION, type);
        
        properties.clear();
        properties.put("company", "Tech Inc");
        type = IngestionUtils.determineNodeType(properties);
        assertEquals(NodeType.ORGANIZATION, type);
        
        properties.clear();
        properties.put("department", "Engineering");
        type = IngestionUtils.determineNodeType(properties);
        assertEquals(NodeType.ORGANIZATION, type);
        
        properties.clear();
        properties.put("industry", "Technology");
        type = IngestionUtils.determineNodeType(properties);
        assertEquals(NodeType.ORGANIZATION, type);
        
        System.out.println("✓ ORGANIZATION node type determined correctly");
    }

    @Test
    @DisplayName("Determine node type - PLACE")
    void testDetermineNodeType_Place() {
        System.out.println("Testing PLACE node type determination...");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("location", "New York");
        assertEquals(NodeType.PLACE, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("address", "123 Main St");
        assertEquals(NodeType.PLACE, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("city", "Boston");
        properties.put("country", "USA");
        assertEquals(NodeType.PLACE, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("coordinates", "40.7128,-74.0060");
        assertEquals(NodeType.PLACE, IngestionUtils.determineNodeType(properties));
        
        System.out.println("✓ PLACE node type determined correctly");
    }

    @Test
    @DisplayName("Determine node type - EVENT")
    void testDetermineNodeType_Event() {
        System.out.println("Testing EVENT node type determination...");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("date", "2024-01-15");
        assertEquals(NodeType.EVENT, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("eventDate", "2024-02-20");
        assertEquals(NodeType.EVENT, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("startDate", "2024-03-01");
        properties.put("endDate", "2024-03-05");
        assertEquals(NodeType.EVENT, IngestionUtils.determineNodeType(properties));
        
        System.out.println("✓ EVENT node type determined correctly");
    }

    @Test
    @DisplayName("Determine node type - ITEM")
    void testDetermineNodeType_Item() {
        System.out.println("Testing ITEM node type determination...");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("product", "Widget");
        assertEquals(NodeType.ITEM, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("sku", "ABC123");
        assertEquals(NodeType.ITEM, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("price", 99.99);
        assertEquals(NodeType.ITEM, IngestionUtils.determineNodeType(properties));
        
        System.out.println("✓ ITEM node type determined correctly");
    }

    @Test
    @DisplayName("Determine node type - DOCUMENT")
    void testDetermineNodeType_Document() {
        System.out.println("Testing DOCUMENT node type determination...");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("content", "Document content here");
        assertEquals(NodeType.DOCUMENT, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("text", "Some text");
        assertEquals(NodeType.DOCUMENT, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("description", "A description");
        assertEquals(NodeType.DOCUMENT, IngestionUtils.determineNodeType(properties));
        
        System.out.println("✓ DOCUMENT node type determined correctly");
    }

    @Test
    @DisplayName("Determine node type - CONCEPT (default)")
    void testDetermineNodeType_Concept() {
        System.out.println("Testing CONCEPT node type determination (default)...");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("randomField", "value");
        assertEquals(NodeType.CONCEPT, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("unknownProperty", "data");
        assertEquals(NodeType.CONCEPT, IngestionUtils.determineNodeType(properties));
        
        properties.clear(); // Empty properties
        assertEquals(NodeType.CONCEPT, IngestionUtils.determineNodeType(properties));
        
        System.out.println("✓ CONCEPT node type determined correctly as default");
    }

    @Test
    @DisplayName("Determine node type - Mixed properties (priority test)")
    void testDetermineNodeType_MixedProperties() {
        System.out.println("Testing node type determination with mixed properties...");
        
        Map<String, Object> properties = new HashMap<>();
        // Has both person and organization properties - should detect person first
        properties.put("email", "john@example.com");
        properties.put("company", "Acme Corp");
        assertEquals(NodeType.PERSON, IngestionUtils.determineNodeType(properties));
        
        // Has document and place properties - should detect place first
        properties.clear();
        properties.put("location", "New York");
        properties.put("content", "Some content");
        assertEquals(NodeType.PLACE, IngestionUtils.determineNodeType(properties));
        
        System.out.println("✓ Mixed properties handled with correct priority");
    }

    @Test
    @DisplayName("Determine node type - Null and empty values")
    void testDetermineNodeType_NullAndEmptyValues() {
        System.out.println("Testing node type determination with null and empty values...");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("email", null);
        properties.put("firstName", "");
        properties.put("lastName", "   "); // Whitespace only
        
        // Should not detect as PERSON due to null/empty values
        assertEquals(NodeType.CONCEPT, IngestionUtils.determineNodeType(properties));
        
        // Add valid value
        properties.put("role", "Developer");
        assertEquals(NodeType.PERSON, IngestionUtils.determineNodeType(properties));
        
        System.out.println("✓ Null and empty values handled correctly");
    }

    @Test
    @DisplayName("Determine node type - Non-string values")
    void testDetermineNodeType_NonStringValues() {
        System.out.println("Testing node type determination with non-string values...");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("price", 99.99); // Double
        assertEquals(NodeType.ITEM, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("coordinates", new double[]{40.7128, -74.0060}); // Array
        assertEquals(NodeType.PLACE, IngestionUtils.determineNodeType(properties));
        
        properties.clear();
        properties.put("date", System.currentTimeMillis()); // Long
        assertEquals(NodeType.EVENT, IngestionUtils.determineNodeType(properties));
        
        System.out.println("✓ Non-string values handled correctly");
    }

    @Test
    @DisplayName("Hash performance for large content")
    void testGenerateHash_Performance() {
        System.out.println("Testing hash generation performance for large content...");
        
        // Create 1MB of data
        byte[] largeContent = new byte[1024 * 1024];
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }
        
        long startTime = System.currentTimeMillis();
        String hash = IngestionUtils.generateHash(largeContent);
        long endTime = System.currentTimeMillis();
        
        assertNotNull(hash);
        assertEquals(64, hash.length());
        
        long duration = endTime - startTime;
        System.out.println("Hash generation for 1MB took: " + duration + "ms");
        assertTrue(duration < 1000, "Hash generation should be fast");
        
        System.out.println("✓ Large content hashed efficiently");
    }

    @Test
    @DisplayName("Preview extraction performance")
    void testExtractPreview_Performance() {
        System.out.println("Testing preview extraction performance...");
        
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeText.append("This is line ").append(i).append(". ");
        }
        
        long startTime = System.currentTimeMillis();
        String preview = IngestionUtils.extractPreview(largeText.toString(), 100);
        long endTime = System.currentTimeMillis();
        
        assertNotNull(preview);
        assertTrue(preview.length() <= 103);
        
        long duration = endTime - startTime;
        System.out.println("Preview extraction from large text took: " + duration + "ms");
        assertTrue(duration < 100, "Preview extraction should be fast");
        
        System.out.println("✓ Preview extracted efficiently from large text");
    }
}