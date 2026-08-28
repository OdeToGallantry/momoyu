package com.example.bookmark.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "对话历史", description = "按用户隔离的聊天会话")
public class ChatConversationController {

    private final ChatConversationService conversationService;

    public ChatConversationController(ChatConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    @Operation(summary = "会话列表", description = "按更新时间倒序，不含消息正文")
    public List<ConversationSummaryResponse> list() {
        return conversationService.listForCurrentUser();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "新建空会话")
    public ConversationDetailResponse create() {
        return conversationService.createForCurrentUser();
    }

    @GetMapping("/{id}")
    @Operation(summary = "会话详情", description = "含全部消息")
    public ConversationDetailResponse get(@PathVariable Long id) {
        return conversationService.getForCurrentUser(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "重命名会话")
    public ConversationSummaryResponse rename(@PathVariable Long id, @Valid @RequestBody UpdateConversationRequest request) {
        return conversationService.updateTitle(id, request.getTitle());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "删除会话")
    public void delete(@PathVariable Long id) {
        conversationService.deleteForCurrentUser(id);
    }
}
