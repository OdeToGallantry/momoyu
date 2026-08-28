package com.example.bookmark.ai;

import com.example.bookmark.auth.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiServiceProviderRoutingTest {

    @Test
    void adminUsesKimiUserUsesZhipu() {
        ZhipuProperties zhipu = new ZhipuProperties();
        zhipu.setApiKey("glm-key");
        zhipu.setModel("glm-4.7-flash");

        KimiProperties kimi = new KimiProperties();
        kimi.setApiKey("kimi-key");

        AiService service = new AiService(zhipu, kimi, new ObjectMapper());

        LlmProvider admin = service.resolveProvider(Role.ADMIN);
        assertEquals("Kimi", admin.name());
        assertEquals("kimi-key", admin.apiKey());
        assertEquals("kimi-for-coding", admin.model());
        assertEquals("https://api.kimi.com/coding/v1", admin.baseUrl());
        assertEquals("enabled", admin.thinkingType());
        assertEquals("claude-cli/2.0.0", admin.userAgent());

        LlmProvider user = service.resolveProvider(Role.USER);
        assertEquals("智谱", user.name());
        assertEquals("glm-key", user.apiKey());
        assertEquals("glm-4.7-flash", user.model());
        assertEquals("disabled", user.thinkingType());
        assertEquals(null, user.userAgent());
    }
}
