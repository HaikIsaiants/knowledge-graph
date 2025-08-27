package com.knowledgegraph.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Centralized configuration for ingestion services
 * Simplifies property management using Spring Boot's configuration properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ingestion")
public class IngestionConfig {
    
    private Upload upload = new Upload();
    private Processing processing = new Processing();
    private Pdf pdf = new Pdf();
    
    @Data
    public static class Upload {
        private String storagePath = "./uploads";
        private long maxFileSize = 10485760; // 10MB
        private String allowedExtensions = "csv,json,pdf,md,txt,html,xml";
    }
    
    @Data
    public static class Processing {
        private int chunkSize = 1000;
        private double chunkOverlap = 0.2;
        private int workerThreads = 3;
    }
    
    @Data
    public static class Pdf {
        private int chunkSize = 1000;
        private int overlap = 100;
    }
}