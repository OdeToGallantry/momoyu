package com.example.bookmark.ai;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationCompactorTest {

    @Test
    void underThresholdDoesNotCompact() {
        assertFalse(ConversationCompactor.needsCompact(10, 12, 6));
        assertFalse(ConversationCompactor.needsCompact(12, 12, 6));
    }

    @Test
    void overThresholdNeedsCompact() {
        assertTrue(ConversationCompactor.needsCompact(13, 12, 6));
    }

    @Test
    void splitKeepsRecentTail() {
        List<AiChatMessage> messages = sample(8);
        ConversationCompactor.CompactSplit split = ConversationCompactor.split(messages, 3);
        assertEquals(5, split.older().size());
        assertEquals(3, split.recent().size());
        assertEquals("a5", split.recent().get(0).getContent());
        assertEquals("a7", split.recent().get(2).getContent());
    }

    @Test
    void withSummaryPrependsPair() {
        List<AiChatMessage> recent = sample(2);
        List<AiChatMessage> packed = ConversationCompactor.withSummary("聊过天气", recent);
        assertEquals(4, packed.size());
        assertTrue(packed.get(0).getContent().contains("聊过天气"));
        assertEquals("assistant", packed.get(1).getRole());
        assertEquals("u0", packed.get(2).getContent());
    }

    @Test
    void normalizeDropsBlankAndSystem() {
        List<AiChatMessage> raw = new ArrayList<>();
        raw.add(msg("system", "ignore"));
        raw.add(msg("user", "  hi  "));
        raw.add(msg("assistant", ""));
        raw.add(msg("user", "ok"));
        List<AiChatMessage> normalized = ConversationCompactor.normalize(raw);
        assertEquals(2, normalized.size());
        assertEquals("hi", normalized.get(0).getContent());
    }

    private static List<AiChatMessage> sample(int n) {
        List<AiChatMessage> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(msg(i % 2 == 0 ? "user" : "assistant", (i % 2 == 0 ? "u" : "a") + i));
        }
        return list;
    }

    private static AiChatMessage msg(String role, String content) {
        AiChatMessage m = new AiChatMessage();
        m.setRole(role);
        m.setContent(content);
        return m;
    }
}
