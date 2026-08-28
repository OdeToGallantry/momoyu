package com.example.bookmark.chat;

import com.example.bookmark.ai.AiChatMessage;
import com.example.bookmark.auth.AppUser;
import com.example.bookmark.auth.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatConversationService {

    private static final int TITLE_MAX_LEN = 40;

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final CurrentUserService currentUserService;

    public ChatConversationService(
            ChatConversationRepository conversationRepository,
            ChatMessageRepository messageRepository,
            CurrentUserService currentUserService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.currentUserService = currentUserService;
    }

    public List<ConversationSummaryResponse> listForCurrentUser() {
        AppUser user = currentUserService.requireCurrentUser();
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .filter(conv -> messageRepository.existsByConversationIdAndRole(conv.getId(), "user"))
                .map(conv -> new ConversationSummaryResponse(
                        conv.getId(),
                        conv.getTitle(),
                        conv.getUpdatedAt(),
                        messageRepository.countByConversationId(conv.getId())))
                .toList();
    }

    @Transactional
    public ConversationDetailResponse createForCurrentUser() {
        AppUser user = currentUserService.requireCurrentUser();
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(user.getId());
        conversation.setTitle("新对话");
        conversationRepository.save(conversation);
        return toDetail(conversation, List.of());
    }

    public ConversationDetailResponse getForCurrentUser(Long conversationId) {
        ChatConversation conversation = requireOwnedConversation(conversationId);
        List<ChatMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        return toDetail(conversation, messages);
    }

    @Transactional
    public ConversationSummaryResponse updateTitle(Long conversationId, String title) {
        ChatConversation conversation = requireOwnedConversation(conversationId);
        conversation.setTitle(title.trim());
        conversationRepository.save(conversation);
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getUpdatedAt(),
                messageRepository.countByConversationId(conversationId));
    }

    @Transactional
    public void deleteForCurrentUser(Long conversationId) {
        requireOwnedConversation(conversationId);
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.deleteById(conversationId);
    }

    public List<AiChatMessage> loadAiMessages(Long conversationId) {
        requireOwnedConversation(conversationId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toAiMessage)
                .toList();
    }

    @Transactional
    public void appendExchange(Long conversationId, String userContent, String assistantContent) {
        ChatConversation conversation = requireOwnedConversation(conversationId);
        boolean isFirstExchange = messageRepository.countByConversationId(conversationId) == 0;
        saveMessage(conversationId, "user", userContent);
        saveMessage(conversationId, "assistant", assistantContent);
        if (isFirstExchange) {
            conversation.setTitle(buildTitle(userContent));
        }
        conversationRepository.save(conversation);
    }

    ChatConversation requireOwnedConversation(Long conversationId) {
        AppUser user = currentUserService.requireCurrentUser();
        return conversationRepository.findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "对话不存在"));
    }

    private ChatMessage saveMessage(Long conversationId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        return messageRepository.save(message);
    }

    private AiChatMessage toAiMessage(ChatMessage message) {
        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setRole(message.getRole());
        aiMessage.setContent(message.getContent());
        return aiMessage;
    }

    private ConversationDetailResponse toDetail(ChatConversation conversation, List<ChatMessage> messages) {
        List<MessageResponse> messageResponses = messages.stream()
                .map(m -> new MessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                messageResponses);
    }

    static String buildTitle(String userContent) {
        String trimmed = userContent == null ? "" : userContent.strip();
        if (trimmed.isEmpty()) {
            return "新对话";
        }
        trimmed = trimmed.replaceAll("\\s+", " ");
        if (trimmed.length() <= TITLE_MAX_LEN) {
            return trimmed;
        }
        return trimmed.substring(0, TITLE_MAX_LEN) + "…";
    }

    /** 把 DB 消息转成 AI 请求列表并追加新用户句 */
    public static List<AiChatMessage> withUserMessage(List<AiChatMessage> existing, String userContent) {
        List<AiChatMessage> merged = new ArrayList<>(existing);
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setRole("user");
        userMessage.setContent(userContent);
        merged.add(userMessage);
        return merged;
    }
}
