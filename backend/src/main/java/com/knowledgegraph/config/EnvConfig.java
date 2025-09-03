package com.knowledgegraph.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class EnvConfig {
    
    @PostConstruct
    public void loadEnvFile() {
        try {
            // Load .env file from project root
            Path currentPath = Paths.get("").toAbsolutePath();
            Path envPath = currentPath.getParent().resolve(".env");
            
            if (envPath.toFile().exists()) {
                Dotenv dotenv = Dotenv.configure()
                    .directory(currentPath.getParent().toString())
                    .load();
                
                // Set system properties from .env file
                dotenv.entries().forEach(entry -> {
                    System.setProperty(entry.getKey(), entry.getValue());
                });
                
                System.out.println("Loaded .env file from: " + envPath);
            } else {
                System.out.println(".env file not found at: " + envPath);
            }
        } catch (Exception e) {
            System.err.println("Error loading .env file: " + e.getMessage());
        }
    }
}