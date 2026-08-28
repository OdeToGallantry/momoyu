package com.example.bookmark.chat;

import java.time.Instant;

public record ConversationSummaryResponse(
        Long id,
        String title,
        Instant updatedAt,
        long messageCount) {
}
