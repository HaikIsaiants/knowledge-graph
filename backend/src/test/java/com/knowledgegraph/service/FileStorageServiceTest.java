package com.knowledgegraph.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService Tests")
class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up FileStorageService for testing...");
        
        fileStorageService = new FileStorageService();
        
        // Set properties using reflection
        ReflectionTestUtils.setField(fileStorageService, "storagePath", tempDir.toString());
        ReflectionTestUtils.setField(fileStorageService, "maxFileSize", 10485760L); // 10MB
        ReflectionTestUtils.setField(fileStorageService, "allowedExtensions", "csv,json,pdf,md,txt,html,xml");
        
        // Initialize the service
        fileStorageService.init();
        
        System.out.println("Storage initialized at: " + tempDir);
    }

    @Test
    @DisplayName("Initialize storage directory - Success")
    void testInit_CreatesStorageDirectory() {
        System.out.println("Testing storage directory initialization...");
        
        assertTrue(Files.exists(tempDir));
        assertTrue(Files.isDirectory(tempDir));
        
        System.out.println("✓ Storage directory created successfully");
    }

    @Test
    @DisplayName("Store valid CSV file - Success")
    void testStoreFile_ValidCsvFile_Success() throws IOException {
        System.out.println("Testing storing valid CSV file...");
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-data.csv",
            "text/csv",
            "name,age\nJohn,30".getBytes()
        );
        
        String storedPath = fileStorageService.storeFile(file);
        
        assertNotNull(storedPath);
        assertTrue(storedPath.contains(".csv"));
        assertTrue(storedPath.contains(LocalDate.now().toString()));
        
        Path actualFile = Paths.get(storedPath);
        assertTrue(Files.exists(actualFile));
        assertEquals("name,age\nJohn,30", Files.readString(actualFile));
        
        System.out.println("Stored file at: " + storedPath);
        System.out.println("✓ CSV file stored successfully");
    }

    @Test
    @DisplayName("Store valid JSON file - Success")
    void testStoreFile_ValidJsonFile_Success() throws IOException {
        System.out.println("Testing storing valid JSON file...");
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-data.json",
            "application/json",
            "{\"test\": \"data\"}".getBytes()
        );
        
        String storedPath = fileStorageService.storeFile(file);
        
        assertNotNull(storedPath);
        assertTrue(storedPath.endsWith(".json"));
        
        Path actualFile = Paths.get(storedPath);
        assertTrue(Files.exists(actualFile));
        assertEquals("{\"test\": \"data\"}", Files.readString(actualFile));
        
        System.out.println("✓ JSON file stored successfully");
    }

    @Test
    @DisplayName("Store file with special characters in name - Success")
    void testStoreFile_SpecialCharactersInName_Success() throws IOException {
        System.out.println("Testing file with special characters in name...");
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test-file (2024) [v1.0].csv",
            "text/csv",
            "data".getBytes()
        );
        
        String storedPath = fileStorageService.storeFile(file);
        
        assertNotNull(storedPath);
        assertTrue(storedPath.endsWith(".csv"));
        assertTrue(Files.exists(Paths.get(storedPath)));
        
        System.out.println("✓ File with special characters stored successfully");
    }

    @Test
    @DisplayName("Store empty file - Validation error")
    void testStoreFile_EmptyFile_ValidationError() {
        System.out.println("Testing empty file validation...");
        
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file",
            "empty.csv",
            "text/csv",
            new byte[0]
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> fileStorageService.storeFile(emptyFile)
        );
        
        assertTrue(exception.getMessage().contains("empty file"));
        
        System.out.println("Error message: " + exception.getMessage());
        System.out.println("✓ Empty file validation working correctly");
    }

    @Test
    @DisplayName("Store oversized file - Validation error")
    void testStoreFile_OversizedFile_ValidationError() {
        System.out.println("Testing oversized file validation...");
        
        byte[] largeContent = new byte[10485761]; // 1 byte over limit
        Arrays.fill(largeContent, (byte) 'A');
        
        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.csv",
            "text/csv",
            largeContent
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> fileStorageService.storeFile(largeFile)
        );
        
        assertTrue(exception.getMessage().contains("exceeds maximum"));
        
        System.out.println("✓ Oversized file validation working correctly");
    }

    @Test
    @DisplayName("Store file with invalid extension - Validation error")
    void testStoreFile_InvalidExtension_ValidationError() {
        System.out.println("Testing invalid file extension validation...");
        
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "program.exe",
            "application/octet-stream",
            "executable content".getBytes()
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> fileStorageService.storeFile(invalidFile)
        );
        
        assertTrue(exception.getMessage().contains("not allowed"));
        
        System.out.println("✓ Invalid extension validation working correctly");
    }

    @Test
    @DisplayName("Store file with path traversal attempt - Validation error")
    void testStoreFile_PathTraversal_ValidationError() {
        System.out.println("Testing path traversal prevention...");
        
        MockMultipartFile maliciousFile = new MockMultipartFile(
            "file",
            "../../../etc/passwd",
            "text/plain",
            "malicious content".getBytes()
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> fileStorageService.storeFile(maliciousFile)
        );
        
        assertTrue(exception.getMessage().contains("invalid path"));
        
        System.out.println("✓ Path traversal prevention working correctly");
    }

    @Test
    @DisplayName("Validate file - All validation checks")
    void testValidateFile_AllChecks() {
        System.out.println("Testing file validation logic...");
        
        // Test valid file
        MockMultipartFile validFile = new MockMultipartFile(
            "file", "valid.csv", "text/csv", "content".getBytes()
        );
        assertDoesNotThrow(() -> fileStorageService.validateFile(validFile, "valid.csv"));
        
        // Test empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", "empty.csv", "text/csv", new byte[0]
        );
        assertThrows(IllegalArgumentException.class, 
            () -> fileStorageService.validateFile(emptyFile, "empty.csv"));
        
        // Test oversized file
        byte[] largeContent = new byte[10485761];
        MockMultipartFile largeFile = new MockMultipartFile(
            "file", "large.csv", "text/csv", largeContent
        );
        assertThrows(IllegalArgumentException.class,
            () -> fileStorageService.validateFile(largeFile, "large.csv"));
        
        // Test invalid extension
        MockMultipartFile invalidExt = new MockMultipartFile(
            "file", "file.bat", "application/octet-stream", "content".getBytes()
        );
        assertThrows(IllegalArgumentException.class,
            () -> fileStorageService.validateFile(invalidExt, "file.bat"));
        
        // Test path traversal
        MockMultipartFile pathTraversal = new MockMultipartFile(
            "file", "../file.csv", "text/csv", "content".getBytes()
        );
        assertThrows(IllegalArgumentException.class,
            () -> fileStorageService.validateFile(pathTraversal, "../file.csv"));
        
        System.out.println("✓ All file validation checks working correctly");
    }

    @Test
    @DisplayName("Load existing file - Success")
    void testLoadFile_ExistingFile_Success() throws IOException {
        System.out.println("Testing loading existing file...");
        
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");
        
        Path loadedPath = fileStorageService.loadFile("test.txt");
        
        assertNotNull(loadedPath);
        assertTrue(loadedPath.endsWith("test.txt"));
        
        System.out.println("✓ File loaded successfully");
    }

    @Test
    @DisplayName("Delete existing file - Success")
    void testDeleteFile_ExistingFile_Success() throws IOException {
        System.out.println("Testing deleting existing file...");
        
        // Create a test file
        Path testFile = tempDir.resolve("to-delete.txt");
        Files.writeString(testFile, "delete me");
        
        boolean deleted = fileStorageService.deleteFile("to-delete.txt");
        
        assertTrue(deleted);
        assertFalse(Files.exists(testFile));
        
        System.out.println("✓ File deleted successfully");
    }

    @Test
    @DisplayName("Delete non-existent file - Returns false")
    void testDeleteFile_NonExistentFile_ReturnsFalse() {
        System.out.println("Testing deleting non-existent file...");
        
        boolean deleted = fileStorageService.deleteFile("non-existent.txt");
        
        assertFalse(deleted);
        
        System.out.println("✓ Non-existent file deletion handled correctly");
    }

    @Test
    @DisplayName("Calculate file hash - Success")
    void testCalculateFileHash_Success() {
        System.out.println("Testing file hash calculation...");
        
        byte[] content = "test content".getBytes();
        String hash1 = fileStorageService.calculateFileHash(content);
        String hash2 = fileStorageService.calculateFileHash(content);
        String hash3 = fileStorageService.calculateFileHash("different content".getBytes());
        
        assertNotNull(hash1);
        assertEquals(64, hash1.length()); // SHA-256 produces 64 hex characters
        assertEquals(hash1, hash2); // Same content produces same hash
        assertNotEquals(hash1, hash3); // Different content produces different hash
        
        System.out.println("Hash: " + hash1);
        System.out.println("✓ File hash calculation working correctly");
    }

    @Test
    @DisplayName("Check file exists - Success")
    void testFileExists_Success() throws IOException {
        System.out.println("Testing file existence check...");
        
        // Create a test file
        Path testFile = tempDir.resolve("exists.txt");
        Files.writeString(testFile, "I exist");
        
        assertTrue(fileStorageService.fileExists("exists.txt"));
        assertFalse(fileStorageService.fileExists("not-exists.txt"));
        
        // Test with directory (should return false)
        Path testDir = tempDir.resolve("testdir");
        Files.createDirectory(testDir);
        assertFalse(fileStorageService.fileExists("testdir"));
        
        System.out.println("✓ File existence check working correctly");
    }

    @Test
    @DisplayName("Get file size - Success")
    void testGetFileSize_Success() throws IOException {
        System.out.println("Testing getting file size...");
        
        String content = "This is test content with known size";
        Path testFile = tempDir.resolve("sized-file.txt");
        Files.writeString(testFile, content);
        
        long size = fileStorageService.getFileSize("sized-file.txt");
        
        assertEquals(content.length(), size);
        
        System.out.println("File size: " + size + " bytes");
        System.out.println("✓ File size retrieval working correctly");
    }

    @Test
    @DisplayName("Get content type - Success")
    void testGetContentType_Success() throws IOException {
        System.out.println("Testing content type detection...");
        
        // Test various file extensions
        testContentTypeForFile("test.csv", "text/csv");
        testContentTypeForFile("test.json", "application/json");
        testContentTypeForFile("test.pdf", "application/pdf");
        testContentTypeForFile("test.md", "text/markdown");
        testContentTypeForFile("test.txt", "text/plain");
        testContentTypeForFile("test.html", "text/html");
        testContentTypeForFile("test.xml", "application/xml");
        testContentTypeForFile("test.unknown", "application/octet-stream");
        
        System.out.println("✓ Content type detection working correctly for all file types");
    }

    @Test
    @DisplayName("Store multiple files concurrently - Success")
    void testStoreFile_ConcurrentStorage_Success() throws Exception {
        System.out.println("Testing concurrent file storage...");
        
        int numberOfFiles = 10;
        Thread[] threads = new Thread[numberOfFiles];
        String[] results = new String[numberOfFiles];
        
        for (int i = 0; i < numberOfFiles; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    MockMultipartFile file = new MockMultipartFile(
                        "file",
                        "concurrent-" + index + ".csv",
                        "text/csv",
                        ("data-" + index).getBytes()
                    );
                    results[index] = fileStorageService.storeFile(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all files were stored
        for (int i = 0; i < numberOfFiles; i++) {
            assertNotNull(results[i]);
            assertTrue(Files.exists(Paths.get(results[i])));
        }
        
        System.out.println("✓ Concurrent file storage working correctly");
    }

    @Test
    @DisplayName("Storage with subdirectories - Success")
    void testStoreFile_CreatesDateSubdirectory() throws IOException {
        System.out.println("Testing date-based subdirectory creation...");
        
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.csv",
            "text/csv",
            "data".getBytes()
        );
        
        String storedPath = fileStorageService.storeFile(file);
        
        assertTrue(storedPath.contains(LocalDate.now().toString()));
        
        Path actualFile = Paths.get(storedPath);
        assertTrue(Files.exists(actualFile));
        assertTrue(Files.exists(actualFile.getParent())); // Date subdirectory exists
        
        System.out.println("✓ Date-based subdirectory created successfully");
    }

    private void testContentTypeForFile(String filename, String expectedType) throws IOException {
        Path testFile = tempDir.resolve(filename);
        Files.writeString(testFile, "content");
        
        String contentType = fileStorageService.getContentType(filename);
        assertEquals(expectedType, contentType);
    }
}