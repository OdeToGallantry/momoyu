package com.example.bookmark.chat;

import java.time.Instant;
import java.util.List;

public record ConversationDetailResponse(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<MessageResponse> messages) {
}
