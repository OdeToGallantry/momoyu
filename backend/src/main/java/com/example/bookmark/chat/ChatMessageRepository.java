package com.example.bookmark.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    void deleteByConversationId(Long conversationId);

    long countByConversationId(Long conversationId);

    boolean existsByConversationIdAndRole(Long conversationId, String role);
}
