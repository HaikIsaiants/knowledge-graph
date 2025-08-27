package com.knowledgegraph.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IngestionConfig Tests")
class IngestionConfigTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        System.out.println("Setting up IngestionConfig test environment...");
        contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(TestConfiguration.class);
    }

    @Test
    @DisplayName("Load default configuration values")
    void testDefaultConfiguration() {
        System.out.println("Testing default configuration values...");
        
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(IngestionConfig.class);
            
            IngestionConfig config = context.getBean(IngestionConfig.class);
            
            // Test Upload defaults
            assertNotNull(config.getUpload());
            assertEquals("./uploads", config.getUpload().getStoragePath());
            assertEquals(10485760L, config.getUpload().getMaxFileSize());
            assertEquals("csv,json,pdf,md,txt,html,xml", config.getUpload().getAllowedExtensions());
            
            // Test Processing defaults
            assertNotNull(config.getProcessing());
            assertEquals(1000, config.getProcessing().getChunkSize());
            assertEquals(0.2, config.getProcessing().getChunkOverlap());
            assertEquals(3, config.getProcessing().getWorkerThreads());
            
            // Test PDF defaults
            assertNotNull(config.getPdf());
            assertEquals(1000, config.getPdf().getChunkSize());
            assertEquals(100, config.getPdf().getOverlap());
            
            System.out.println("✓ Default configuration loaded successfully");
        });
    }

    @Test
    @DisplayName("Override upload configuration")
    void testOverrideUploadConfiguration() {
        System.out.println("Testing upload configuration override...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.upload.storage-path=/custom/path",
                "ingestion.upload.max-file-size=20971520",
                "ingestion.upload.allowed-extensions=txt,doc,docx"
            )
            .run(context -> {
                IngestionConfig config = context.getBean(IngestionConfig.class);
                
                assertEquals("/custom/path", config.getUpload().getStoragePath());
                assertEquals(20971520L, config.getUpload().getMaxFileSize());
                assertEquals("txt,doc,docx", config.getUpload().getAllowedExtensions());
                
                System.out.println("✓ Upload configuration overridden successfully");
            });
    }

    @Test
    @DisplayName("Override processing configuration")
    void testOverrideProcessingConfiguration() {
        System.out.println("Testing processing configuration override...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.processing.chunk-size=2000",
                "ingestion.processing.chunk-overlap=0.5",
                "ingestion.processing.worker-threads=10"
            )
            .run(context -> {
                IngestionConfig config = context.getBean(IngestionConfig.class);
                
                assertEquals(2000, config.getProcessing().getChunkSize());
                assertEquals(0.5, config.getProcessing().getChunkOverlap());
                assertEquals(10, config.getProcessing().getWorkerThreads());
                
                System.out.println("✓ Processing configuration overridden successfully");
            });
    }

    @Test
    @DisplayName("Override PDF configuration")
    void testOverridePdfConfiguration() {
        System.out.println("Testing PDF configuration override...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.pdf.chunk-size=500",
                "ingestion.pdf.overlap=50"
            )
            .run(context -> {
                IngestionConfig config = context.getBean(IngestionConfig.class);
                
                assertEquals(500, config.getPdf().getChunkSize());
                assertEquals(50, config.getPdf().getOverlap());
                
                System.out.println("✓ PDF configuration overridden successfully");
            });
    }

    @Test
    @DisplayName("Partial configuration override")
    void testPartialConfigurationOverride() {
        System.out.println("Testing partial configuration override...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.upload.storage-path=/partial/path"
                // Other values should remain as defaults
            )
            .run(context -> {
                IngestionConfig config = context.getBean(IngestionConfig.class);
                
                // Overridden value
                assertEquals("/partial/path", config.getUpload().getStoragePath());
                
                // Default values
                assertEquals(10485760L, config.getUpload().getMaxFileSize());
                assertEquals("csv,json,pdf,md,txt,html,xml", config.getUpload().getAllowedExtensions());
                assertEquals(1000, config.getProcessing().getChunkSize());
                
                System.out.println("✓ Partial configuration override works correctly");
            });
    }

    @Test
    @DisplayName("Test configuration with environment variables")
    void testConfigurationWithEnvironmentVariables() {
        System.out.println("Testing configuration with environment variable placeholders...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.upload.storage-path=${UPLOAD_PATH:./default-uploads}"
            )
            .run(context -> {
                IngestionConfig config = context.getBean(IngestionConfig.class);
                
                // Should use default value when env var is not set
                assertEquals("./default-uploads", config.getUpload().getStoragePath());
                
                System.out.println("✓ Environment variable defaults work correctly");
            });
    }

    @Test
    @DisplayName("Configuration boundary values")
    void testConfigurationBoundaryValues() {
        System.out.println("Testing configuration with boundary values...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.upload.max-file-size=0",
                "ingestion.processing.chunk-size=1",
                "ingestion.processing.chunk-overlap=0.0",
                "ingestion.processing.worker-threads=1",
                "ingestion.pdf.chunk-size=1",
                "ingestion.pdf.overlap=0"
            )
            .run(context -> {
                IngestionConfig config = context.getBean(IngestionConfig.class);
                
                assertEquals(0L, config.getUpload().getMaxFileSize());
                assertEquals(1, config.getProcessing().getChunkSize());
                assertEquals(0.0, config.getProcessing().getChunkOverlap());
                assertEquals(1, config.getProcessing().getWorkerThreads());
                assertEquals(1, config.getPdf().getChunkSize());
                assertEquals(0, config.getPdf().getOverlap());
                
                System.out.println("✓ Boundary values handled correctly");
            });
    }

    @Test
    @DisplayName("Configuration with maximum values")
    void testConfigurationMaximumValues() {
        System.out.println("Testing configuration with maximum values...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.upload.max-file-size=" + Long.MAX_VALUE,
                "ingestion.processing.chunk-size=" + Integer.MAX_VALUE,
                "ingestion.processing.chunk-overlap=1.0",
                "ingestion.processing.worker-threads=" + Integer.MAX_VALUE
            )
            .run(context -> {
                IngestionConfig config = context.getBean(IngestionConfig.class);
                
                assertEquals(Long.MAX_VALUE, config.getUpload().getMaxFileSize());
                assertEquals(Integer.MAX_VALUE, config.getProcessing().getChunkSize());
                assertEquals(1.0, config.getProcessing().getChunkOverlap());
                assertEquals(Integer.MAX_VALUE, config.getProcessing().getWorkerThreads());
                
                System.out.println("✓ Maximum values handled correctly");
            });
    }

    @Test
    @DisplayName("Configuration with special characters in paths")
    void testConfigurationSpecialCharacterPaths() {
        System.out.println("Testing configuration with special characters in paths...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.upload.storage-path=/path with spaces/uploads",
                "ingestion.upload.allowed-extensions=csv,json,pdf,md,txt,html,xml,doc,docx"
            )
            .run(context -> {
                IngestionConfig config = context.getBean(IngestionConfig.class);
                
                assertEquals("/path with spaces/uploads", config.getUpload().getStoragePath());
                assertTrue(config.getUpload().getAllowedExtensions().contains("doc"));
                
                System.out.println("✓ Special characters in paths handled correctly");
            });
    }

    @Test
    @DisplayName("Test Upload configuration getters and setters")
    void testUploadGettersSetters() {
        System.out.println("Testing Upload configuration getters and setters...");
        
        IngestionConfig.Upload upload = new IngestionConfig.Upload();
        
        upload.setStoragePath("/new/path");
        assertEquals("/new/path", upload.getStoragePath());
        
        upload.setMaxFileSize(5242880L);
        assertEquals(5242880L, upload.getMaxFileSize());
        
        upload.setAllowedExtensions("pdf,doc");
        assertEquals("pdf,doc", upload.getAllowedExtensions());
        
        System.out.println("✓ Upload getters and setters work correctly");
    }

    @Test
    @DisplayName("Test Processing configuration getters and setters")
    void testProcessingGettersSetters() {
        System.out.println("Testing Processing configuration getters and setters...");
        
        IngestionConfig.Processing processing = new IngestionConfig.Processing();
        
        processing.setChunkSize(500);
        assertEquals(500, processing.getChunkSize());
        
        processing.setChunkOverlap(0.3);
        assertEquals(0.3, processing.getChunkOverlap());
        
        processing.setWorkerThreads(5);
        assertEquals(5, processing.getWorkerThreads());
        
        System.out.println("✓ Processing getters and setters work correctly");
    }

    @Test
    @DisplayName("Test PDF configuration getters and setters")
    void testPdfGettersSetters() {
        System.out.println("Testing PDF configuration getters and setters...");
        
        IngestionConfig.Pdf pdf = new IngestionConfig.Pdf();
        
        pdf.setChunkSize(750);
        assertEquals(750, pdf.getChunkSize());
        
        pdf.setOverlap(75);
        assertEquals(75, pdf.getOverlap());
        
        System.out.println("✓ PDF getters and setters work correctly");
    }

    @Test
    @DisplayName("Test main configuration getters and setters")
    void testMainConfigGettersSetters() {
        System.out.println("Testing main configuration getters and setters...");
        
        IngestionConfig config = new IngestionConfig();
        
        IngestionConfig.Upload upload = new IngestionConfig.Upload();
        upload.setStoragePath("/test");
        config.setUpload(upload);
        assertEquals("/test", config.getUpload().getStoragePath());
        
        IngestionConfig.Processing processing = new IngestionConfig.Processing();
        processing.setChunkSize(100);
        config.setProcessing(processing);
        assertEquals(100, config.getProcessing().getChunkSize());
        
        IngestionConfig.Pdf pdf = new IngestionConfig.Pdf();
        pdf.setOverlap(10);
        config.setPdf(pdf);
        assertEquals(10, config.getPdf().getOverlap());
        
        System.out.println("✓ Main configuration getters and setters work correctly");
    }

    @Test
    @DisplayName("Configuration with invalid types - handled gracefully")
    void testConfigurationWithInvalidTypes() {
        System.out.println("Testing configuration with invalid type values...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.upload.max-file-size=not-a-number"
            )
            .run(context -> {
                // Context should fail to start with invalid configuration
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasMessageContaining("Failed to bind properties");
                
                System.out.println("✓ Invalid configuration types handled correctly");
            });
    }

    @Test
    @DisplayName("Configuration with negative values")
    void testConfigurationWithNegativeValues() {
        System.out.println("Testing configuration with negative values...");
        
        contextRunner
            .withPropertyValues(
                "ingestion.upload.max-file-size=-1",
                "ingestion.processing.chunk-size=-100",
                "ingestion.processing.chunk-overlap=-0.5",
                "ingestion.processing.worker-threads=-5"
            )
            .run(context -> {
                IngestionConfig config = context.getBean(IngestionConfig.class);
                
                // Negative values are accepted but should be validated in the service layer
                assertEquals(-1L, config.getUpload().getMaxFileSize());
                assertEquals(-100, config.getProcessing().getChunkSize());
                assertEquals(-0.5, config.getProcessing().getChunkOverlap());
                assertEquals(-5, config.getProcessing().getWorkerThreads());
                
                System.out.println("✓ Negative values stored (validation should be in service layer)");
            });
    }

    @EnableConfigurationProperties(IngestionConfig.class)
    static class TestConfiguration {
        // Test configuration class
    }
}