package com.knowledgegraph.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${ingestion.upload.storage-path:./uploads}")
    private String storagePath;

    @Value("${ingestion.upload.max-file-size:10485760}")
    private long maxFileSize;

    @Value("${ingestion.upload.allowed-extensions:csv,json,pdf,md,txt,html,xml}")
    private String allowedExtensions;

    private Path storageLocation;
    private List<String> allowedExtensionsList;

    @PostConstruct
    public void init() {
        this.storageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        this.allowedExtensionsList = Arrays.asList(allowedExtensions.split(","));
        
        try {
            Files.createDirectories(this.storageLocation);
            log.info("File storage initialized at: {}", this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.storageLocation, e);
        }
    }

    public String storeFile(MultipartFile file) throws IOException {
        // Clean filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        
        // Validate file
        validateFile(file, originalFilename);
        
        // Generate unique filename
        String fileExtension = FilenameUtils.getExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
        
        // Create subdirectory based on date
        String dateSubdir = java.time.LocalDate.now().toString();
        Path targetLocation = this.storageLocation.resolve(dateSubdir);
        Files.createDirectories(targetLocation);
        
        // Store the file
        Path targetPath = targetLocation.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("File stored: {} -> {}", originalFilename, targetPath);
        return targetPath.toString();
    }

    public void validateFile(MultipartFile file, String filename) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file: " + filename);
        }
        
        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum allowed size of %d bytes: %s", 
                    maxFileSize, filename)
            );
        }
        
        // Check file extension
        String extension = FilenameUtils.getExtension(filename).toLowerCase();
        if (!allowedExtensionsList.contains(extension)) {
            throw new IllegalArgumentException(
                "File type not allowed. Allowed types: " + allowedExtensions
            );
        }
        
        // Check for path traversal attempts
        if (filename.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence: " + filename);
        }
    }

    public Path loadFile(String filename) {
        return storageLocation.resolve(filename).normalize();
    }

    public boolean deleteFile(String filename) {
        try {
            Path filePath = loadFile(filename);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Error deleting file: " + filename, e);
            return false;
        }
    }

    public String calculateFileHash(byte[] fileContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fileContent);
            // Use Apache Commons Codec for simpler hex conversion
            return org.apache.commons.codec.binary.Hex.encodeHexString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available in Java
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public boolean fileExists(String filename) {
        Path filePath = loadFile(filename);
        return Files.exists(filePath) && Files.isRegularFile(filePath);
    }

    public long getFileSize(String filename) throws IOException {
        Path filePath = loadFile(filename);
        return Files.size(filePath);
    }

    public String getContentType(String filename) throws IOException {
        Path filePath = loadFile(filename);
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            // Fallback based on extension
            String extension = FilenameUtils.getExtension(filename).toLowerCase();
            contentType = switch (extension) {
                case "csv" -> "text/csv";
                case "json" -> "application/json";
                case "pdf" -> "application/pdf";
                case "md" -> "text/markdown";
                case "txt" -> "text/plain";
                case "html" -> "text/html";
                case "xml" -> "application/xml";
                default -> "application/octet-stream";
            };
        }
        return contentType;
    }
}