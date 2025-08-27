package com.knowledgegraph.controller;

import com.knowledgegraph.TestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@Testcontainers
@ActiveProfiles("test")
@Import(TestConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpoint_ShouldReturnOkStatus() throws Exception {
        System.out.println("Testing health endpoint returns OK status...");
        
        MvcResult result = mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.application", is("Knowledge Graph Backend")))
                .andExpect(jsonPath("$.version", is("0.0.1-SNAPSHOT")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andReturn();

        System.out.println("Health endpoint response: " + result.getResponse().getContentAsString());
        System.out.println("✓ Health endpoint returned correct status and structure");
    }

    @Test
    void healthEndpoint_ShouldReturnTimestamp() throws Exception {
        System.out.println("Testing health endpoint includes timestamp...");
        
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.timestamp", isA(String.class)));

        System.out.println("✓ Health endpoint includes valid timestamp");
    }

    @Test
    void healthEndpoint_ShouldReturnCorrectApplicationInfo() throws Exception {
        System.out.println("Testing health endpoint returns correct application information...");
        
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application", is("Knowledge Graph Backend")))
                .andExpect(jsonPath("$.version", is("0.0.1-SNAPSHOT")));

        System.out.println("✓ Health endpoint returns correct application name and version");
    }
}