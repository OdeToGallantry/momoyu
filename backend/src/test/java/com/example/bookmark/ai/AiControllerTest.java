package com.example.bookmark.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;

    @BeforeEach
    void login() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "user",
                "password", "user123"
        ));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        userToken = objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void chatRequiresAuth() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "conversationId", 1,
                "content", "你好"
        ));
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void chatRejectsBlankContent() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "conversationId", 1,
                "content", "   "
        ));
        mockMvc.perform(post("/api/ai/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatRejectsMissingConversationId() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "content", "你好"
        ));
        mockMvc.perform(post("/api/ai/chat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
