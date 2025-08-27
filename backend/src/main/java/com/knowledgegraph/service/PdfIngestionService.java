package com.knowledgegraph.service;

import com.knowledgegraph.dto.IngestionResult;
import com.knowledgegraph.model.Document;
import com.knowledgegraph.model.Node;
import com.knowledgegraph.model.NodeType;
import com.knowledgegraph.repository.NodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfIngestionService extends AbstractIngestionService {

    private final NodeRepository nodeRepository;
    private final TextChunkingService textChunkingService;

    @Value("${pdf.ingestion.chunk-size:1000}")
    private int defaultChunkSize;

    @Value("${pdf.ingestion.overlap:100}")
    private int chunkOverlap;

    @Transactional
    public IngestionResult processPdfFile(String filePath, UUID jobId) {
        return processPdfFile(filePath, jobId, defaultChunkSize);
    }

    @Transactional
    public IngestionResult processPdfFile(String filePath, UUID jobId, int chunkSize) {
        log.info("Processing PDF file: {} with chunk size: {}", filePath, chunkSize);
        
        IngestionResult.IngestionResultBuilder resultBuilder = IngestionResult.builder()
                .jobId(jobId)
                .processedAt(LocalDateTime.now());
        
        List<IngestionResult.ProcessingError> errors = new ArrayList<>();
        List<UUID> createdNodeIds = new ArrayList<>();
        List<UUID> createdDocumentIds = new ArrayList<>();
        
        int totalPages = 0;
        int successCount = 0;
        int errorCount = 0;
        int totalChunks = 0;

        try {
            Path path = Paths.get(filePath);
            File pdfFile = path.toFile();
            
            if (!pdfFile.exists()) {
                return buildErrorResult(jobId, "PDF file not found: " + filePath, "FileNotFoundException");
            }

            // First, create a document record for the PDF file
            Document document = createDocumentRecord(filePath, "application/pdf", "PDF");
            createdDocumentIds.add(document.getId());

            // Extract metadata and content using both PDFBox and Tika for comprehensive extraction
            Map<String, Object> pdfMetadata = extractPdfMetadata(pdfFile);
            document.getMetadata().putAll(pdfMetadata);
            document = documentRepository.save(document);

            // Process PDF content with page-level chunking
            try (PDDocument pdDocument = Loader.loadPDF(pdfFile)) {
                
                if (pdDocument.isEncrypted()) {
                    AccessPermission permission = pdDocument.getCurrentAccessPermission();
                    if (!permission.canExtractContent()) {
                        log.warn("PDF is encrypted and content extraction is not permitted: {}", filePath);
                        return buildErrorResult(jobId, 
                            "PDF is encrypted and cannot be processed", "EncryptedPdfException");
                    }
                    log.warn("Processing encrypted PDF (content extraction permitted): {}", filePath);
                }

                totalPages = pdDocument.getNumberOfPages();
                log.info("PDF has {} pages", totalPages);

                PDFTextStripper stripper = new PDFTextStripper();
                
                // Process each page separately for better granularity
                for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                    try {
                        stripper.setStartPage(pageNum);
                        stripper.setEndPage(pageNum);
                        String pageText = stripper.getText(pdDocument);
                        
                        if (pageText != null && !pageText.trim().isEmpty()) {
                            // Use TextChunkingService for consistent chunking
                            List<TextChunkingService.TextChunk> chunks = 
                                textChunkingService.chunkText(pageText.trim(), chunkSize, 
                                    (double) chunkOverlap / chunkSize);
                            
                            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                                TextChunkingService.TextChunk chunk = chunks.get(chunkIndex);
                                Node node = createDocumentNode(chunk.getContent(), document, 
                                    pageNum, chunkIndex + 1, chunks.size());
                                if (node != null) {
                                    createdNodeIds.add(node.getId());
                                    totalChunks++;
                                    successCount++;
                                }
                            }
                        } else {
                            log.debug("Page {} is empty or contains only whitespace", pageNum);
                        }
                        
                    } catch (Exception e) {
                        errorCount++;
                        errors.add(IngestionResult.ProcessingError.builder()
                                .lineNumber(pageNum)
                                .field("page")
                                .value(String.valueOf(pageNum))
                                .errorMessage("Failed to process page: " + e.getMessage())
                                .errorType(e.getClass().getSimpleName())
                                .build());
                        log.error("Error processing page {} of PDF {}: {}", pageNum, filePath, e.getMessage());
                    }
                }
            }

            log.info("PDF processing complete. Pages: {}, Chunks: {}, Success: {}, Errors: {}", 
                     totalPages, totalChunks, successCount, errorCount);

            return resultBuilder
                    .success(errorCount == 0 || (successCount > 0 && errorCount < totalPages / 2))
                    .message(String.format("Processed PDF with %d pages into %d chunks", totalPages, totalChunks))
                    .totalRecords(totalChunks)
                    .successCount(successCount)
                    .errorCount(errorCount)
                    .createdNodeIds(createdNodeIds)
                    .createdDocumentIds(createdDocumentIds)
                    .errors(errors)
                    .build();

        } catch (IOException e) {
            log.error("Failed to process PDF file: {}", filePath, e);
            return buildErrorResult(jobId, "Failed to process PDF file: " + e.getMessage(), "IOException");
        } catch (Exception e) {
            log.error("Unexpected error processing PDF file: {}", filePath, e);
            return buildErrorResult(jobId, "Unexpected error: " + e.getMessage(), e.getClass().getSimpleName());
        }
    }


    private Map<String, Object> extractPdfMetadata(File pdfFile) {
        Map<String, Object> metadata = new HashMap<>();
        
        // Extract metadata using PDFBox
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDDocumentInformation info = document.getDocumentInformation();
            
            if (info != null) {
                addMetadataField(metadata, "title", info.getTitle());
                addMetadataField(metadata, "author", info.getAuthor());
                addMetadataField(metadata, "subject", info.getSubject());
                addMetadataField(metadata, "keywords", info.getKeywords());
                addMetadataField(metadata, "creator", info.getCreator());
                addMetadataField(metadata, "producer", info.getProducer());
                
                if (info.getCreationDate() != null) {
                    metadata.put("creationDate", info.getCreationDate().getTime().toString());
                }
                if (info.getModificationDate() != null) {
                    metadata.put("modificationDate", info.getModificationDate().getTime().toString());
                }
            }
            
            metadata.put("numberOfPages", document.getNumberOfPages());
            metadata.put("isEncrypted", document.isEncrypted());
            metadata.put("version", document.getVersion());
            
        } catch (IOException e) {
            log.warn("Could not extract PDF metadata using PDFBox for file: {}", pdfFile.getPath(), e);
        }
        
        // Extract additional metadata using Tika
        try (FileInputStream inputStream = new FileInputStream(pdfFile)) {
            BodyContentHandler handler = new BodyContentHandler(-1); // No content limit for metadata extraction
            Metadata tikaMetadata = new Metadata();
            PDFParser parser = new PDFParser();
            ParseContext context = new ParseContext();
            
            parser.parse(inputStream, handler, tikaMetadata, context);
            
            // Add Tika-specific metadata
            String[] metadataNames = tikaMetadata.names();
            for (String name : metadataNames) {
                if (!metadata.containsKey(name.toLowerCase())) {
                    addMetadataField(metadata, name.toLowerCase(), tikaMetadata.get(name));
                }
            }
            
        } catch (Exception e) {
            log.warn("Could not extract PDF metadata using Tika for file: {}", pdfFile.getPath(), e);
        }
        
        return metadata;
    }

    private void addMetadataField(Map<String, Object> metadata, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            metadata.put(key, value.trim());
        }
    }


    private Node createDocumentNode(String content, Document document, int pageNumber, int chunkNumber, int totalChunks) {
        try {
            // Generate a meaningful name for the chunk
            String name = generateChunkName(content, document, pageNumber, chunkNumber);
            
            // Check if similar node already exists
            List<Node> existingNodes = nodeRepository.findByNameAndType(name, NodeType.DOCUMENT);
            if (!existingNodes.isEmpty()) {
                log.debug("Document chunk already exists: {} (Page {}, Chunk {})", name, pageNumber, chunkNumber);
                return existingNodes.get(0);
            }
            
            // Create new document node
            Node node = new Node();
            node.setName(name);
            node.setType(NodeType.DOCUMENT);
            node.setSourceUri(document.getUri());
            node.setCapturedAt(LocalDateTime.now());
            
            // Set properties with PDF-specific information
            Map<String, Object> properties = new HashMap<>();
            properties.put("content", content);
            properties.put("pageNumber", pageNumber);
            properties.put("chunkNumber", chunkNumber);
            properties.put("totalChunks", totalChunks);
            properties.put("contentLength", content.length());
            properties.put("documentType", "PDF");
            
            // Add first sentence as preview if content is long
            if (content.length() > 200) {
                String preview = extractPreview(content, 150);
                properties.put("preview", preview);
            }
            
            // Add citation information
            properties.put("citation", String.format("Page %d, Chunk %d of %d", pageNumber, chunkNumber, totalChunks));
            
            node.setProperties(properties);
            
            Node savedNode = nodeRepository.save(node);
            log.debug("Created document node: {} (Page {}, Chunk {})", name, pageNumber, chunkNumber);
            
            return savedNode;
            
        } catch (Exception e) {
            log.error("Failed to create document node for page {}, chunk {}: {}", pageNumber, chunkNumber, e.getMessage());
            return null;
        }
    }

    private String generateChunkName(String content, Document document, int pageNumber, int chunkNumber) {
        // Extract document name from URI
        String documentName = extractDocumentName(document.getUri());
        
        // Try to extract a meaningful title from the first line or sentence
        String contentPreview = extractPreview(content, 50);
        
        return String.format("%s - Page %d, Chunk %d: %s", documentName, pageNumber, chunkNumber, contentPreview);
    }

    private String extractDocumentName(String uri) {
        if (uri == null) {
            return "Unknown Document";
        }
        
        // Extract filename from URI
        String filename = uri.substring(uri.lastIndexOf('/') + 1);
        if (filename.contains(".")) {
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }
        
        return filename.replace("_", " ").replace("-", " ");
    }

    private String extractPreview(String content, int maxLength) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        
        String preview = content.trim();
        
        // Try to break at sentence boundary
        if (preview.length() > maxLength) {
            int sentenceEnd = preview.indexOf('.', Math.min(maxLength / 2, preview.length() - 1));
            if (sentenceEnd > 0 && sentenceEnd < maxLength) {
                preview = preview.substring(0, sentenceEnd + 1);
            } else {
                // Break at word boundary
                int lastSpace = preview.lastIndexOf(' ', maxLength);
                if (lastSpace > maxLength / 2) {
                    preview = preview.substring(0, lastSpace) + "...";
                } else {
                    preview = preview.substring(0, maxLength) + "...";
                }
            }
        }
        
        return preview;
    }

}