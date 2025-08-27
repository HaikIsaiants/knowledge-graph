package com.knowledgegraph.service;

import com.knowledgegraph.dto.SearchResponseDTO;
import com.knowledgegraph.dto.SearchResultDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HybridSearchService {
    
    private final SearchService searchService;
    private final VectorSearchService vectorSearchService;
    
    @Value("${search.hybrid.fts_weight:0.5}")
    private double defaultFtsWeight;
    
    @Value("${search.hybrid.vector_weight:0.5}")
    private double defaultVectorWeight;
    
    /**
     * Perform hybrid search combining FTS and vector search
     */
    public SearchResponseDTO hybridSearch(String query, Double ftsWeight, Double vectorWeight, 
                                         Pageable pageable) {
        long startTime = System.currentTimeMillis();
        log.debug("Hybrid search for: '{}', weights: FTS={}, Vector={}", 
                  query, ftsWeight, vectorWeight);
        
        // Use provided weights or defaults
        double ftsW = ftsWeight != null ? ftsWeight : defaultFtsWeight;
        double vectorW = vectorWeight != null ? vectorWeight : defaultVectorWeight;
        
        // Normalize weights
        double totalWeight = ftsW + vectorW;
        ftsW = ftsW / totalWeight;
        vectorW = vectorW / totalWeight;
        
        // Perform both searches
        SearchResponseDTO ftsResults = searchService.searchWithHighlight(query, pageable);
        SearchResponseDTO vectorResults = vectorSearchService.findSimilar(
            query, 
            null,  // use default threshold
            pageable.getPageSize() * 2  // get more results for merging
        );
        
        // Merge and re-rank results
        List<SearchResultDTO> mergedResults = mergeResults(
            ftsResults.getResults(), 
            vectorResults.getResults(),
            ftsW,
            vectorW,
            pageable
        );
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        // Combine facets from both searches
        Map<String, Long> combinedFacets = new HashMap<>();
        if (ftsResults.getTypeFacets() != null) {
            combinedFacets.putAll(ftsResults.getTypeFacets());
        }
        
        return SearchResponseDTO.builder()
            .results(mergedResults)
            .totalElements(mergedResults.size())
            .totalPages((int) Math.ceil(mergedResults.size() / (double) pageable.getPageSize()))
            .currentPage(pageable.getPageNumber())
            .pageSize(pageable.getPageSize())
            .query(query)
            .searchType(SearchResponseDTO.SearchType.HYBRID)
            .searchTimeMs(searchTime)
            .typeFacets(combinedFacets)
            .build();
    }
    
    /**
     * Merge and re-rank results from FTS and vector search
     */
    private List<SearchResultDTO> mergeResults(List<SearchResultDTO> ftsResults,
                                              List<SearchResultDTO> vectorResults,
                                              double ftsWeight,
                                              double vectorWeight,
                                              Pageable pageable) {
        
        Map<UUID, MergedResult> mergedMap = new HashMap<>();
        
        // Process and normalize scores for both result sets
        processResults(ftsResults, mergedMap, true, 
            calculateMaxScore(ftsResults));
        processResults(vectorResults, mergedMap, false, 
            calculateMaxScore(vectorResults));
        
        // Calculate combined scores, apply boost, and paginate
        return mergedMap.values().stream()
            .peek(mr -> calculateCombinedScore(mr, ftsWeight, vectorWeight))
            .sorted(Comparator.comparing((MergedResult mr) -> mr.combinedScore).reversed())
            .skip(pageable.getOffset())
            .limit(pageable.getPageSize())
            .map(mr -> mr.result)
            .collect(Collectors.toList());
    }
    
    /**
     * Process search results and add to merged map
     */
    private void processResults(List<SearchResultDTO> results, 
                                Map<UUID, MergedResult> mergedMap,
                                boolean isFts,
                                double maxScore) {
        results.forEach(result -> {
            double normalizedScore = Optional.ofNullable(result.getScore())
                .orElse(0.0) / maxScore;
            
            MergedResult merged = mergedMap.computeIfAbsent(result.getId(), 
                id -> createMergedResult(result, isFts));
            
            if (isFts) {
                merged.ftsScore = normalizedScore;
                merged.hasHighlight = result.getHighlightedSnippet() != null;
            } else {
                merged.vectorScore = normalizedScore;
            }
        });
    }
    
    /**
     * Create new MergedResult instance
     */
    private MergedResult createMergedResult(SearchResultDTO result, boolean isFts) {
        MergedResult merged = new MergedResult();
        merged.result = result;
        merged.ftsScore = 0.0;
        merged.vectorScore = 0.0;
        merged.hasHighlight = isFts && result.getHighlightedSnippet() != null;
        return merged;
    }
    
    /**
     * Calculate maximum score from results
     */
    private double calculateMaxScore(List<SearchResultDTO> results) {
        return results.stream()
            .mapToDouble(r -> Optional.ofNullable(r.getScore()).orElse(0.0))
            .max()
            .orElse(1.0);
    }
    
    /**
     * Calculate combined score with boost
     */
    private void calculateCombinedScore(MergedResult mr, double ftsWeight, double vectorWeight) {
        mr.combinedScore = (mr.ftsScore * ftsWeight) + (mr.vectorScore * vectorWeight);
        
        // Apply boost if result appears in both searches
        if (mr.ftsScore > 0 && mr.vectorScore > 0) {
            mr.combinedScore *= 1.2;
        }
        
        mr.result.setScore(mr.combinedScore);
    }
    
    /**
     * Perform adaptive hybrid search that adjusts weights based on result quality
     */
    public SearchResponseDTO adaptiveHybridSearch(String query, Pageable pageable) {
        log.debug("Adaptive hybrid search for: '{}'", query);
        
        // First, do a quick probe with both methods
        SearchResponseDTO ftsProbe = searchService.searchNodes(query, null, Pageable.ofSize(5));
        SearchResponseDTO vectorProbe = vectorSearchService.findSimilar(query, null, 5);
        
        // Analyze result quality
        double ftsQuality = assessResultQuality(ftsProbe);
        double vectorQuality = assessResultQuality(vectorProbe);
        
        // Adjust weights based on quality
        double ftsWeight = ftsQuality / (ftsQuality + vectorQuality);
        double vectorWeight = vectorQuality / (ftsQuality + vectorQuality);
        
        log.info("Adaptive weights - FTS: {:.2f}, Vector: {:.2f}", ftsWeight, vectorWeight);
        
        // Perform hybrid search with adjusted weights
        return hybridSearch(query, ftsWeight, vectorWeight, pageable);
    }
    
    /**
     * Assess quality of search results
     */
    private double assessResultQuality(SearchResponseDTO results) {
        if (results.getResults().isEmpty()) {
            return 0.1;  // Minimum weight
        }
        
        // Combine quality factors using streams
        double resultCountFactor = Math.min(results.getResults().size(), 5) * 0.2;
        double avgScoreFactor = Math.min(calculateAverageScore(results.getResults()), 1.0);
        double spreadFactor = calculateScoreSpread(results) * 0.5;
        
        return Math.min(resultCountFactor + avgScoreFactor + spreadFactor, 1.0);
    }
    
    /**
     * Calculate average score from results
     */
    private double calculateAverageScore(List<SearchResultDTO> results) {
        return results.stream()
            .mapToDouble(r -> Optional.ofNullable(r.getScore()).orElse(0.0))
            .average()
            .orElse(0.0);
    }
    
    /**
     * Calculate score spread for diversity assessment
     */
    private double calculateScoreSpread(SearchResponseDTO results) {
        if (results.getResults().size() <= 1) {
            return 0.0;
        }
        
        double maxScore = Optional.ofNullable(results.getMaxScore()).orElse(0.0);
        double minScore = Optional.ofNullable(results.getMinScore()).orElse(0.0);
        return Math.min((maxScore - minScore) * 2, 1.0);
    }
    
    /**
     * Helper class for merging results
     */
    private static class MergedResult {
        SearchResultDTO result;
        double ftsScore;
        double vectorScore;
        double combinedScore;
        boolean hasHighlight;
    }
}