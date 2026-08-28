package com.example.bookmark.chat;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void login() throws Exception {
        userToken = loginAs("user", "user123");
        adminToken = loginAs("admin", "admin123");
    }

    private String loginAs(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    @Test
    void listRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createListGetRenameDelete() throws Exception {
        String createResponse = mockMvc.perform(post("/api/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("新对话"))
                .andExpect(jsonPath("$.messages").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/conversations/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(patch("/api/conversations/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "午饭吃啥"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("午饭吃啥"));

        mockMvc.perform(delete("/api/conversations/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/conversations/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersCannotAccessEachOthersConversations() throws Exception {
        String createResponse = mockMvc.perform(post("/api/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/conversations/" + id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void buildTitleTruncatesLongText() {
        assertThat(ChatConversationService.buildTitle("短标题")).isEqualTo("短标题");
        assertThat(ChatConversationService.buildTitle("  多   空格  ")).isEqualTo("多 空格");
        String longText = "a".repeat(50);
        assertThat(ChatConversationService.buildTitle(longText)).hasSize(41).endsWith("…");
    }
}
