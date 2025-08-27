package com.knowledgegraph.util;

import com.knowledgegraph.model.NodeType;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;

/**
 * Utility class for common ingestion operations
 * Simplifies repeated patterns across services
 */
public class IngestionUtils {
    
    private IngestionUtils() {
        // Utility class
    }
    
    /**
     * Generate SHA-256 hash of content
     */
    public static String generateHash(String content) {
        return DigestUtils.sha256Hex(content);
    }
    
    /**
     * Generate SHA-256 hash of byte content
     */
    public static String generateHash(byte[] content) {
        return DigestUtils.sha256Hex(content);
    }
    
    /**
     * Extract preview from text content
     */
    public static String extractPreview(String content, int maxLength) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        String preview = content.trim();
        if (preview.length() <= maxLength) {
            return preview;
        }
        
        // Try to break at sentence boundary
        int sentenceEnd = preview.indexOf('.', Math.min(maxLength / 2, preview.length() - 1));
        if (sentenceEnd > 0 && sentenceEnd < maxLength) {
            return preview.substring(0, sentenceEnd + 1);
        }
        
        // Break at word boundary
        int lastSpace = preview.lastIndexOf(' ', maxLength);
        if (lastSpace > maxLength / 2) {
            return preview.substring(0, lastSpace) + "...";
        }
        
        return preview.substring(0, maxLength) + "...";
    }
    
    /**
     * Determine node type from properties
     */
    public static NodeType determineNodeType(Map<String, Object> properties) {
        // Check for person indicators
        if (hasAny(properties, "email", "firstName", "lastName", "role")) {
            return NodeType.PERSON;
        }
        
        // Check for organization indicators
        if (hasAny(properties, "organization", "company", "department", "industry")) {
            return NodeType.ORGANIZATION;
        }
        
        // Check for place indicators
        if (hasAny(properties, "location", "address", "city", "country", "coordinates")) {
            return NodeType.PLACE;
        }
        
        // Check for event indicators
        if (hasAny(properties, "date", "eventDate", "startDate", "endDate")) {
            return NodeType.EVENT;
        }
        
        // Check for item indicators
        if (hasAny(properties, "product", "sku", "price")) {
            return NodeType.ITEM;
        }
        
        // Check for document indicators
        if (hasAny(properties, "content", "text", "description")) {
            return NodeType.DOCUMENT;
        }
        
        // Default
        return NodeType.CONCEPT;
    }
    
    private static boolean hasAny(Map<String, Object> properties, String... keys) {
        for (String key : keys) {
            if (properties.containsKey(key) && properties.get(key) != null) {
                Object value = properties.get(key);
                if (value instanceof String && !((String) value).trim().isEmpty()) {
                    return true;
                } else if (!(value instanceof String)) {
                    return true;
                }
            }
        }
        return false;
    }
}