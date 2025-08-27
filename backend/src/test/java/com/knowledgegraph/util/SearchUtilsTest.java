package com.knowledgegraph.util;

import com.knowledgegraph.util.SearchUtils.TimedResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchUtils Tests")
class SearchUtilsTest {

    @Test
    @DisplayName("extractScore - With valid Number")
    void testExtractScore_ValidNumber() {
        System.out.println("Testing extractScore with valid number...");
        
        double score1 = SearchUtils.extractScore(0.95);
        assertEquals(0.95, score1, 0.001);
        
        double score2 = SearchUtils.extractScore(42);
        assertEquals(42.0, score2, 0.001);
        
        double score3 = SearchUtils.extractScore(3.14f);
        assertEquals(3.14, score3, 0.01);
        
        System.out.println("✓ Valid number extraction works correctly");
    }
    
    @Test
    @DisplayName("extractScore - With null value")
    void testExtractScore_NullValue() {
        System.out.println("Testing extractScore with null...");
        
        double score = SearchUtils.extractScore(null);
        assertEquals(0.0, score, 0.001);
        
        System.out.println("✓ Null value returns default 0.0");
    }
    
    @Test
    @DisplayName("extractScore - With non-numeric value")
    void testExtractScore_NonNumericValue() {
        System.out.println("Testing extractScore with non-numeric value...");
        
        double score1 = SearchUtils.extractScore("not a number");
        assertEquals(0.0, score1, 0.001);
        
        double score2 = SearchUtils.extractScore(new Object());
        assertEquals(0.0, score2, 0.001);
        
        System.out.println("✓ Non-numeric values return default 0.0");
    }
    
    @Test
    @DisplayName("normalizeScore - Valid normalization")
    void testNormalizeScore_ValidNormalization() {
        System.out.println("Testing normalizeScore with valid values...");
        
        double normalized1 = SearchUtils.normalizeScore(75.0, 100.0);
        assertEquals(0.75, normalized1, 0.001);
        
        double normalized2 = SearchUtils.normalizeScore(50.0, 200.0);
        assertEquals(0.25, normalized2, 0.001);
        
        double normalized3 = SearchUtils.normalizeScore(100.0, 100.0);
        assertEquals(1.0, normalized3, 0.001);
        
        System.out.println("✓ Score normalization works correctly");
    }
    
    @Test
    @DisplayName("normalizeScore - Edge cases")
    void testNormalizeScore_EdgeCases() {
        System.out.println("Testing normalizeScore edge cases...");
        
        // Zero max value
        double normalized1 = SearchUtils.normalizeScore(50.0, 0.0);
        assertEquals(0.0, normalized1, 0.001);
        
        // Negative max (should still work)
        double normalized2 = SearchUtils.normalizeScore(-50.0, -100.0);
        assertEquals(0.5, normalized2, 0.001);
        
        // Score greater than max
        double normalized3 = SearchUtils.normalizeScore(150.0, 100.0);
        assertEquals(1.5, normalized3, 0.001);
        
        System.out.println("✓ Edge cases handled correctly");
    }
    
    @Test
    @DisplayName("timed - Successful operation timing")
    void testTimed_SuccessfulOperation() throws InterruptedException {
        System.out.println("Testing timed operation execution...");
        
        AtomicInteger counter = new AtomicInteger(0);
        
        TimedResult<String> result = SearchUtils.timed(() -> {
            try {
                Thread.sleep(50); // Simulate some work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            counter.incrementAndGet();
            return "test result";
        });
        
        assertNotNull(result);
        assertEquals("test result", result.getResult());
        assertTrue(result.getDurationMs() >= 50);
        assertTrue(result.getDurationMs() < 200); // Should not take too long
        assertEquals(1, counter.get());
        
        System.out.println("Operation took " + result.getDurationMs() + "ms");
        System.out.println("✓ Operation timing works correctly");
    }
    
    @Test
    @DisplayName("timed - Exception handling")
    void testTimed_ExceptionHandling() {
        System.out.println("Testing timed operation with exception...");
        
        assertThrows(RuntimeException.class, () -> {
            SearchUtils.timed(() -> {
                throw new RuntimeException("Test exception");
            });
        });
        
        System.out.println("✓ Exceptions are propagated correctly");
    }
    
    @Test
    @DisplayName("timed - Null result handling")
    void testTimed_NullResult() {
        System.out.println("Testing timed operation with null result...");
        
        TimedResult<String> result = SearchUtils.timed(() -> null);
        
        assertNotNull(result);
        assertNull(result.getResult());
        assertTrue(result.getDurationMs() >= 0);
        
        System.out.println("✓ Null results handled correctly");
    }
    
    @Test
    @DisplayName("transformPage - Simple transformation")
    void testTransformPage_SimpleTransformation() {
        System.out.println("Testing page transformation...");
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        Page<Integer> intPage = new PageImpl<>(numbers, PageRequest.of(0, 5), 10);
        
        Page<String> stringPage = SearchUtils.transformPage(intPage, i -> "Number: " + i);
        
        assertNotNull(stringPage);
        assertEquals(5, stringPage.getContent().size());
        assertEquals("Number: 1", stringPage.getContent().get(0));
        assertEquals("Number: 5", stringPage.getContent().get(4));
        assertEquals(10, stringPage.getTotalElements());
        assertEquals(0, stringPage.getNumber());
        assertEquals(5, stringPage.getSize());
        
        System.out.println("✓ Page transformation works correctly");
    }
    
    @Test
    @DisplayName("transformPage - Complex object transformation")
    void testTransformPage_ComplexTransformation() {
        System.out.println("Testing complex page transformation...");
        
        class Person {
            String name;
            int age;
            
            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }
        }
        
        class PersonDTO {
            String displayName;
            boolean isAdult;
            
            PersonDTO(String displayName, boolean isAdult) {
                this.displayName = displayName;
                this.isAdult = isAdult;
            }
        }
        
        List<Person> people = Arrays.asList(
            new Person("Alice", 25),
            new Person("Bob", 17),
            new Person("Charlie", 30)
        );
        
        Page<Person> personPage = new PageImpl<>(people, PageRequest.of(0, 10), 3);
        
        Page<PersonDTO> dtoPage = SearchUtils.transformPage(personPage, 
            p -> new PersonDTO(p.name.toUpperCase(), p.age >= 18));
        
        assertEquals(3, dtoPage.getContent().size());
        assertEquals("ALICE", dtoPage.getContent().get(0).displayName);
        assertTrue(dtoPage.getContent().get(0).isAdult);
        assertEquals("BOB", dtoPage.getContent().get(1).displayName);
        assertFalse(dtoPage.getContent().get(1).isAdult);
        
        System.out.println("✓ Complex transformation works correctly");
    }
    
    @Test
    @DisplayName("transformPage - Empty page")
    void testTransformPage_EmptyPage() {
        System.out.println("Testing empty page transformation...");
        
        Page<String> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        
        Page<Integer> transformedPage = SearchUtils.transformPage(emptyPage, String::length);
        
        assertNotNull(transformedPage);
        assertTrue(transformedPage.getContent().isEmpty());
        assertEquals(0, transformedPage.getTotalElements());
        
        System.out.println("✓ Empty page transformation handled correctly");
    }
    
    @Test
    @DisplayName("transformPage - Preserves pagination metadata")
    void testTransformPage_PreservesPaginationMetadata() {
        System.out.println("Testing pagination metadata preservation...");
        
        List<String> items = Arrays.asList("a", "b", "c");
        PageRequest pageable = PageRequest.of(2, 3, Sort.by("name").descending());
        Page<String> originalPage = new PageImpl<>(items, pageable, 30);
        
        Page<Integer> transformedPage = SearchUtils.transformPage(originalPage, String::length);
        
        assertEquals(originalPage.getNumber(), transformedPage.getNumber());
        assertEquals(originalPage.getSize(), transformedPage.getSize());
        assertEquals(originalPage.getTotalElements(), transformedPage.getTotalElements());
        assertEquals(originalPage.getTotalPages(), transformedPage.getTotalPages());
        assertEquals(originalPage.getSort(), transformedPage.getSort());
        
        System.out.println("✓ Pagination metadata preserved correctly");
    }
    
    @Test
    @DisplayName("TimedResult - Getters work correctly")
    void testTimedResult_Getters() {
        System.out.println("Testing TimedResult getters...");
        
        String testResult = "test";
        long testDuration = 123L;
        
        TimedResult<String> timedResult = new TimedResult<>(testResult, testDuration);
        
        assertEquals(testResult, timedResult.getResult());
        assertEquals(testDuration, timedResult.getDurationMs());
        
        System.out.println("✓ TimedResult getters work correctly");
    }
    
    @Test
    @DisplayName("Performance test - Multiple timed operations")
    void testPerformance_MultipleTimedOperations() {
        System.out.println("Testing performance with multiple timed operations...");
        
        long totalTime = 0;
        int iterations = 100;
        
        for (int i = 0; i < iterations; i++) {
            TimedResult<Integer> result = SearchUtils.timed(() -> {
                // Simulate quick computation
                int sum = 0;
                for (int j = 0; j < 1000; j++) {
                    sum += j;
                }
                return sum;
            });
            totalTime += result.getDurationMs();
        }
        
        double avgTime = totalTime / (double) iterations;
        System.out.println("Average operation time: " + avgTime + "ms");
        assertTrue(avgTime < 10, "Operations should be fast");
        
        System.out.println("✓ Multiple timed operations perform well");
    }
    
    @Test
    @DisplayName("Edge case - Transform with null elements")
    void testTransformPage_NullElements() {
        System.out.println("Testing transformation with null elements...");
        
        List<String> itemsWithNulls = Arrays.asList("a", null, "c");
        Page<String> pageWithNulls = new PageImpl<>(itemsWithNulls);
        
        Page<Integer> transformed = SearchUtils.transformPage(pageWithNulls, 
            s -> s != null ? s.length() : -1);
        
        assertEquals(3, transformed.getContent().size());
        assertEquals(1, transformed.getContent().get(0));
        assertEquals(-1, transformed.getContent().get(1));
        assertEquals(1, transformed.getContent().get(2));
        
        System.out.println("✓ Null elements handled in transformation");
    }
}