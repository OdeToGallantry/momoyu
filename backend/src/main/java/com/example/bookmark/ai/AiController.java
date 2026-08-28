package com.example.bookmark.ai;

import com.example.bookmark.auth.Role;
import com.example.bookmark.chat.ChatConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "问 AI", description = "按角色路由：管理员 Kimi，普通用户智谱 GLM")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final AiService aiService;
    private final ChatConversationService conversationService;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public AiController(AiService aiService, ChatConversationService conversationService) {
        this.aiService = aiService;
        this.conversationService = conversationService;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天", description = "SSE：delta 推增量，done / error 收尾。消息写入指定会话。")
    public SseEmitter chat(@Valid @RequestBody AiChatRequest request) {
        Role role = currentRole();
        Long conversationId = request.getConversationId();
        String userContent = request.getContent().trim();

        List<AiChatMessage> history;
        try {
            history = ChatConversationService.withUserMessage(
                    conversationService.loadAiMessages(conversationId),
                    userContent);
        } catch (ResponseStatusException ex) {
            SseEmitter emitter = new SseEmitter(0L);
            sendError(emitter, ex.getReason() != null ? ex.getReason() : "对话不存在");
            return emitter;
        }

        SseEmitter emitter = new SseEmitter(180_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(ex -> emitter.complete());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        executor.execute(() -> {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            StringBuilder assistant = new StringBuilder();
            try {
                aiService.streamChat(history, role, delta -> {
                    assistant.append(delta);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("delta")
                                .data(Map.of("text", delta), MediaType.APPLICATION_JSON));
                    } catch (IOException e) {
                        throw new IllegalStateException("SSE 发送失败", e);
                    }
                });
                conversationService.appendExchange(conversationId, userContent, assistant.toString());
                emitter.send(SseEmitter.event().name("done").data(Map.of("ok", true), MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (ResponseStatusException ex) {
                sendError(emitter, ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                sendError(emitter, "对话失败");
            } catch (Exception ex) {
                log.warn("AI 对话失败", ex);
                sendError(emitter, "对话失败");
            } finally {
                SecurityContextHolder.clearContext();
            }
        });

        return emitter;
    }

    private static Role currentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                    return Role.ADMIN;
                }
            }
        }
        return Role.USER;
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("message", message), MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (Exception ignored) {
            emitter.completeWithError(new IllegalStateException(message));
        }
    }
}
