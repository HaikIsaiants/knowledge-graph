package com.knowledgegraph.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for common search operations
 */
public class SearchUtils {
    
    private SearchUtils() {
        // Utility class
    }
    
    /**
     * Safe score extraction with default value
     */
    public static double extractScore(Object score) {
        return Optional.ofNullable(score)
            .filter(Number.class::isInstance)
            .map(Number.class::cast)
            .map(Number::doubleValue)
            .orElse(0.0);
    }
    
    /**
     * Normalize score to 0-1 range
     */
    public static double normalizeScore(double score, double max) {
        return max > 0 ? score / max : 0.0;
    }
    
    /**
     * Execute operation with timing
     */
    public static <T> TimedResult<T> timed(Supplier<T> operation) {
        long start = System.currentTimeMillis();
        T result = operation.get();
        long duration = System.currentTimeMillis() - start;
        return new TimedResult<>(result, duration);
    }
    
    /**
     * Transform page content while preserving pagination
     */
    public static <T, R> Page<R> transformPage(Page<T> page, Function<T, R> transformer) {
        return new PageImpl<>(
            page.getContent().stream().map(transformer).toList(),
            page.getPageable(),
            page.getTotalElements()
        );
    }
    
    /**
     * Result wrapper with execution time
     */
    public static class TimedResult<T> {
        private final T result;
        private final long durationMs;
        
        public TimedResult(T result, long durationMs) {
            this.result = result;
            this.durationMs = durationMs;
        }
        
        public T getResult() {
            return result;
        }
        
        public long getDurationMs() {
            return durationMs;
        }
    }
}