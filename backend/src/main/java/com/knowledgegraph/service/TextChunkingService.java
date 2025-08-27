package com.knowledgegraph.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TextChunkingService {

    @Value("${ingestion.processing.chunk-size:500}")
    private int defaultChunkSize;

    @Value("${ingestion.processing.chunk-overlap:50}")
    private int defaultOverlap;
    
    @Value("${ingestion.processing.max-chunk-size:2000}")
    private int maxChunkSize;
    
    @Value("${ingestion.processing.min-chunk-size:100}")
    private int minChunkSize;

    @Getter
    @AllArgsConstructor
    public static class TextChunk {
        private final String content;
        private final int startPosition;
        private final int endPosition;
        private final int chunkIndex;
        private final int totalChunks;
        
        public String getIdentifier() {
            return String.format("chunk_%d_of_%d", chunkIndex + 1, totalChunks);
        }
        
        public int getLength() {
            return content.length();
        }
        
        // Additional getters for tests
        public String getText() {
            return content;
        }
        
        public int getIndex() {
            return chunkIndex;
        }
    }

    /**
     * Simple chunk text method returning strings for backward compatibility
     */
    public List<String> chunkText(String text) {
        List<TextChunk> chunks = chunkTextWithMetadata(text, defaultChunkSize, defaultOverlap);
        return chunks.stream().map(TextChunk::getContent).toList();
    }

    /**
     * Chunk text with custom parameters returning strings
     */
    public List<String> chunkText(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Validate parameters
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("Overlap cannot be negative");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("Overlap must be less than chunk size");
        }
        
        List<TextChunk> chunks = chunkTextWithMetadata(text, chunkSize, overlap);
        return chunks.stream().map(TextChunk::getContent).toList();
    }
    
    /**
     * Chunk text by sentences
     */
    public List<String> chunkTextBySentences(String text, int chunkSize, int overlap) {
        List<TextChunk> chunks = chunkBySentences(text, Math.max(1, chunkSize / 30)); // Estimate sentences per chunk
        return chunks.stream().map(TextChunk::getContent).toList();
    }
    
    /**
     * Chunk text by paragraphs
     */
    public List<String> chunkTextByParagraphs(String text, int chunkSize) {
        List<TextChunk> chunks = chunkByParagraphs(text);
        return chunks.stream().map(TextChunk::getContent).toList();
    }
    
    /**
     * Chunk text by words preserving word boundaries
     */
    public List<String> chunkTextByWords(String text, int chunkSize, int overlap) {
        // This will use the main chunking method which already preserves word boundaries
        return chunkText(text, chunkSize, overlap);
    }
    
    /**
     * Main chunking method with metadata
     */
    public List<TextChunk> chunkTextWithMetadata(String text, int chunkSize, int overlap) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        if (chunkSize <= 0) {
            chunkSize = defaultChunkSize;
        }

        if (overlap < 0 || overlap >= chunkSize) {
            overlap = defaultOverlap;
        }

        List<TextChunk> chunks = new ArrayList<>();
        int textLength = text.length();
        int stride = chunkSize - overlap;

        if (stride <= 0) {
            stride = 1; // Prevent infinite loop
        }

        // Handle case where text is shorter than chunk size
        if (textLength <= chunkSize) {
            chunks.add(new TextChunk(text, 0, textLength, 0, 1));
            return chunks;
        }

        // Create chunks with sliding window
        int position = 0;
        int chunkIndex = 0;

        while (position < textLength) {
            int endPosition = Math.min(position + chunkSize, textLength);
            
            // Try to break at sentence or word boundary
            String chunkText = text.substring(position, endPosition);
            
            // If we're not at the end of the text and not at a good breaking point
            if (endPosition < textLength && !isGoodBreakPoint(text, endPosition)) {
                // Look for a better break point (sentence or word boundary)
                int betterBreak = findBetterBreakPoint(text, position, endPosition);
                if (betterBreak > position) {
                    endPosition = betterBreak;
                    chunkText = text.substring(position, endPosition);
                }
            }

            chunks.add(new TextChunk(chunkText, position, endPosition, chunkIndex, -1));
            
            // Move position forward by stride
            position += stride;
            
            // If the remaining text is very small, include it in the last chunk
            if (textLength - position < stride / 2 && position < textLength) {
                // Extend the last chunk to include remaining text
                TextChunk lastChunk = chunks.get(chunks.size() - 1);
                String extendedContent = text.substring(lastChunk.startPosition, textLength);
                chunks.set(chunks.size() - 1, 
                    new TextChunk(extendedContent, lastChunk.getStartPosition(), textLength, 
                                 lastChunk.getChunkIndex(), -1));
                break;
            }
            
            chunkIndex++;
        }

        // Update total chunks count
        int totalChunks = chunks.size();
        List<TextChunk> finalChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            TextChunk oldChunk = chunks.get(i);
            finalChunks.add(new TextChunk(
                oldChunk.getContent(), 
                oldChunk.getStartPosition(), 
                oldChunk.getEndPosition(),
                i, 
                totalChunks
            ));
        }

        log.debug("Created {} chunks from text of length {} with chunk size {} and overlap {}",
                 finalChunks.size(), textLength, chunkSize, overlap);

        return finalChunks;
    }

    /**
     * Chunk text by paragraphs
     */
    public List<TextChunk> chunkByParagraphs(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<TextChunk> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        int position = 0;

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i].trim();
            if (!paragraph.isEmpty()) {
                int startPos = text.indexOf(paragraph, position);
                int endPos = startPos + paragraph.length();
                chunks.add(new TextChunk(paragraph, startPos, endPos, i, paragraphs.length));
                position = endPos;
            }
        }

        return chunks;
    }

    /**
     * Chunk text by sentences
     */
    public List<TextChunk> chunkBySentences(String text, int sentencesPerChunk) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<TextChunk> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");
        
        int chunkIndex = 0;
        for (int i = 0; i < sentences.length; i += sentencesPerChunk) {
            StringBuilder chunkBuilder = new StringBuilder();
            int startPos = -1;
            int endPos = -1;
            
            for (int j = i; j < Math.min(i + sentencesPerChunk, sentences.length); j++) {
                if (startPos == -1) {
                    startPos = text.indexOf(sentences[j]);
                }
                chunkBuilder.append(sentences[j]).append(" ");
            }
            
            String chunkContent = chunkBuilder.toString().trim();
            endPos = startPos + chunkContent.length();
            
            chunks.add(new TextChunk(chunkContent, startPos, endPos, 
                                    chunkIndex++, (sentences.length + sentencesPerChunk - 1) / sentencesPerChunk));
        }

        return chunks;
    }

    /**
     * Check if a position is a good breaking point
     */
    private boolean isGoodBreakPoint(String text, int position) {
        if (position >= text.length()) {
            return true;
        }
        
        char ch = text.charAt(position);
        
        // Check for sentence endings
        if (position > 0) {
            char prevCh = text.charAt(position - 1);
            if (prevCh == '.' || prevCh == '!' || prevCh == '?') {
                return true;
            }
        }
        
        // Check for word boundaries
        return Character.isWhitespace(ch);
    }

    /**
     * Find a better breaking point near the target position
     */
    private int findBetterBreakPoint(String text, int start, int target) {
        // First, look for sentence endings
        for (int i = target - 1; i > start + (target - start) / 2; i--) {
            char ch = text.charAt(i);
            if (ch == '.' || ch == '!' || ch == '?') {
                // Check if there's whitespace after
                if (i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) {
                    return i + 1;
                }
            }
        }
        
        // If no sentence ending found, look for word boundaries
        for (int i = target - 1; i > start + (target - start) / 2; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        
        // If still no good break point, return the original target
        return target;
    }

    /**
     * Calculate statistics for chunks
     */
    public ChunkStatistics calculateStatistics(List<TextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ChunkStatistics(0, 0, 0, 0, 0);
        }

        int totalChunks = chunks.size();
        int totalCharacters = 0;
        int minSize = Integer.MAX_VALUE;
        int maxSize = 0;
        
        for (TextChunk chunk : chunks) {
            int size = chunk.getLength();
            totalCharacters += size;
            minSize = Math.min(minSize, size);
            maxSize = Math.max(maxSize, size);
        }
        
        double avgSize = (double) totalCharacters / totalChunks;
        
        return new ChunkStatistics(totalChunks, totalCharacters, minSize, maxSize, avgSize);
    }

    @Getter
    @AllArgsConstructor
    public static class ChunkStatistics {
        private final int totalChunks;
        private final int totalCharacters;
        private final int minChunkSize;
        private final int maxChunkSize;
        private final double averageChunkSize;
    }
}