package com.knowledgegraph.controller;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;

/**
 * Test controller for GPT-5-mini chat completions
 */
@RestController
@RequestMapping("/test/chat")
@Slf4j
@Tag(name = "Chat Test", description = "Test GPT-5-mini chat completions")
public class ChatTestController {
    
    @Value("${openai.api.key:}")
    private String apiKey;
    
    private OpenAiService openAiService;
    
    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-api-key-here")) {
            this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
            log.info("Chat test controller initialized with OpenAI API");
        }
    }
    
    @PostMapping("/complete")
    @Operation(summary = "Test GPT-5-mini chat completion", 
               description = "Send a message to GPT-5-mini and get a response")
    public ResponseEntity<Map<String, Object>> testChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }
        
        if (openAiService == null) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "OpenAI service not initialized"));
        }
        
        try {
            // Create chat completion request with GPT-3.5-turbo to test API key
            ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                    .model("gpt-3.5-turbo")  // Using GPT-3.5-turbo for compatibility
                    .messages(Arrays.asList(
                        new ChatMessage("system", "You are a helpful assistant."),
                        new ChatMessage("user", message)
                    ))
                    .maxTokens(150)
                    .temperature(0.7)
                    .build();
            
            // Get completion
            ChatCompletionResult result = openAiService.createChatCompletion(chatRequest);
            
            // Extract response
            String response = result.getChoices().get(0).getMessage().getContent();
            
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("model", "gpt-5-mini");
            responseMap.put("userMessage", message);
            responseMap.put("assistantResponse", response);
            responseMap.put("tokensUsed", result.getUsage().getTotalTokens());
            responseMap.put("success", true);
            
            return ResponseEntity.ok(responseMap);
            
        } catch (Exception e) {
            log.error("Error calling GPT-5-mini: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                        "error", "Failed to call GPT-5-mini: " + e.getMessage(),
                        "success", false
                    ));
        }
    }
    
    @GetMapping("/models")
    @Operation(summary = "List available models", 
               description = "Get list of available OpenAI models")
    public ResponseEntity<Map<String, Object>> listModels() {
        if (openAiService == null) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "OpenAI service not initialized"));
        }
        
        try {
            var models = openAiService.listModels();
            List<String> modelIds = new ArrayList<>();
            
            models.forEach(model -> modelIds.add(model.id));
            
            // Sort and filter for GPT models
            List<String> gptModels = modelIds.stream()
                    .filter(id -> id.contains("gpt"))
                    .sorted()
                    .toList();
            
            return ResponseEntity.ok(Map.of(
                "allModels", modelIds,
                "gptModels", gptModels,
                "hasGPT5Mini", modelIds.contains("gpt-5-mini"),
                "totalModels", modelIds.size()
            ));
            
        } catch (Exception e) {
            log.error("Error listing models: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to list models: " + e.getMessage()));
        }
    }
}