package com.example.bookmark.chat;

import java.time.Instant;

public record MessageResponse(
        Long id,
        String role,
        String content,
        Instant createdAt) {
}
