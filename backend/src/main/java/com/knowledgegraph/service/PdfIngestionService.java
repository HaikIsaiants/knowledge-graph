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
    private final GPTEntityExtractor gptEntityExtractor;

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
            log.info("About to create document record");
            Document document = null;
            try {
                document = createDocumentRecord(filePath, "application/pdf", "PDF");
                log.info("Document record created with ID: {}", document.getId());
            } catch (Exception e) {
                log.error("Failed to create document record: {}", e.getMessage(), e);
                throw e;
            }
            createdDocumentIds.add(document.getId());

            // Extract metadata and content using both PDFBox and Tika for comprehensive extraction
            Map<String, Object> pdfMetadata = extractPdfMetadata(pdfFile);
            document.getMetadata().putAll(pdfMetadata);
            document = documentRepository.save(document);

            // Process PDF content with page-level chunking
            log.info("About to load PDF file: {}", pdfFile.getAbsolutePath());
            try (PDDocument pdDocument = Loader.loadPDF(pdfFile)) {
                log.info("PDF loaded successfully");
                
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
                            List<String> chunks = 
                                textChunkingService.chunkText(pageText.trim(), chunkSize, chunkOverlap);
                            
                            for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
                                String chunkContent = chunks.get(chunkIndex);
                                Node node = createDocumentNode(chunkContent, document, 
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

            // Extract entities from the full document text using GPT
            try {
                log.info("Extracting entities from PDF document using GPT");
                StringBuilder fullText = new StringBuilder();
                
                // Collect all text from the document for entity extraction
                try (PDDocument pdDocForEntities = Loader.loadPDF(pdfFile)) {
                    PDFTextStripper fullTextStripper = new PDFTextStripper();
                    fullText.append(fullTextStripper.getText(pdDocForEntities));
                }
                
                // Extract entities and relationships using GPT
                GPTEntityExtractor.ExtractionResult extractionResult = gptEntityExtractor.extractEntitiesAndRelationships(
                    fullText.toString(), 
                    document.getId()
                );
                
                // Add extracted entity IDs to the result
                for (Node entity : extractionResult.nodes) {
                    createdNodeIds.add(entity.getId());
                }
                
                log.info("Extracted {} entities and {} relationships from PDF document", 
                         extractionResult.nodes.size(), extractionResult.edges.size());
                
            } catch (Exception e) {
                log.warn("Failed to extract entities using GPT: {}", e.getMessage());
                // Continue without entity extraction - not a fatal error
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
        log.info("Attempting to load PDF for metadata extraction: {}", pdfFile.getName());
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            log.info("PDF loaded for metadata extraction");
            PDDocumentInformation info = document.getDocumentInformation();
            
            if (info != null) {
                log.info("Getting PDF metadata fields");
                try {
                    log.debug("Getting title");
                    addMetadataField(metadata, "title", info.getTitle());
                    log.debug("Getting author");
                    addMetadataField(metadata, "author", info.getAuthor());
                    log.debug("Getting subject");
                    addMetadataField(metadata, "subject", info.getSubject());
                    log.debug("Getting keywords");
                    addMetadataField(metadata, "keywords", info.getKeywords());
                    log.debug("Getting creator");
                    addMetadataField(metadata, "creator", info.getCreator());
                    log.debug("Getting producer");
                    addMetadataField(metadata, "producer", info.getProducer());
                } catch (Exception e) {
                    log.warn("Failed to get metadata field: {}", e.getMessage());
                }
                
                log.info("Skipping date metadata - known to hang on some PDFs");
                // SKIP DATE METADATA - Causes hanging on certain PDFs
                // The getCreationDate() and getModificationDate() methods can hang indefinitely
                // on some PDFs with malformed date metadata
            }
            
            log.info("Getting page count");
            try {
                metadata.put("numberOfPages", document.getNumberOfPages());
            } catch (Exception e) {
                log.warn("Failed to get page count: {}", e.getMessage());
            }
            
            log.info("Getting encryption status");
            try {
                metadata.put("isEncrypted", document.isEncrypted());
            } catch (Exception e) {
                log.warn("Failed to get encryption status: {}", e.getMessage());
            }
            
            log.info("Getting PDF version");
            try {
                metadata.put("version", document.getVersion());
            } catch (Exception e) {
                log.warn("Failed to get PDF version: {}", e.getMessage());
            }
            
            log.info("Metadata extraction complete");
            
        } catch (IOException e) {
            log.warn("Could not extract PDF metadata using PDFBox for file: {}", pdfFile.getPath(), e);
        }
        
        // SKIP TIKA METADATA - Causes hanging on certain PDFs
        // Tika's PDF parser can hang indefinitely on some PDF files
        log.info("Skipping Tika metadata extraction - known to hang on some PDFs");
        
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