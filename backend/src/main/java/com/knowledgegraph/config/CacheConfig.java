package com.knowledgegraph.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {
    
    @Value("${search.cache.ttl:300}")
    private int cacheTtl;
    
    @Value("${search.cache.max-size:1000}")
    private int cacheMaxSize;
    
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
            "neighborhoods",    // Graph neighborhoods
            "graphStats",      // Graph statistics
            "searchResults",   // Search results
            "nodeDetails",     // Node details
            "paths"           // Path queries
        );
        
        cacheManager.setCaffeine(caffeineConfig());
        log.info("Cache manager configured with TTL={}s, maxSize={}", cacheTtl, cacheMaxSize);
        
        return cacheManager;
    }
    
    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        return Caffeine.newBuilder()
            .expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
            .maximumSize(cacheMaxSize)
            .recordStats();
    }
    
    @Bean("customKeyGenerator")
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> 
            String.format("%s_%s_%s",
                target.getClass().getSimpleName(),
                method.getName(),
                Arrays.stream(params)
                    .map(p -> p != null ? p.toString() : "null")
                    .collect(Collectors.joining("_"))
            );
    }
}