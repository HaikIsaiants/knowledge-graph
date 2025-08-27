package com.knowledgegraph;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration.class)
@TestPropertySource(properties = {
    "logging.level.com.knowledgegraph=DEBUG",
    "logging.level.org.testcontainers=INFO"
})
class KnowledgeGraphApplicationTests {

    @Test
    void contextLoads() {
        System.out.println("Testing Spring Boot application context loading...");
        
        // If we get here without exceptions, the context loaded successfully
        assertTrue(true, "Spring Boot application context should load successfully");
        
        System.out.println("✓ Spring Boot application context loaded successfully");
    }

    @Test
    void applicationStartsWithoutErrors() {
        System.out.println("Testing application startup without errors...");
        
        // The @SpringBootTest annotation already starts the full application
        // If there are any startup errors, this test would fail
        assertTrue(true, "Application should start without errors");
        
        System.out.println("✓ Application started successfully without errors");
    }
}