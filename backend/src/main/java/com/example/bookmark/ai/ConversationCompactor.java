package com.example.bookmark.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 对话压缩：超阈值时把旧消息收成摘要，保留最近若干条原文。
 */
final class ConversationCompactor {

    private ConversationCompactor() {
    }

    static List<AiChatMessage> normalize(List<AiChatMessage> messages) {
        List<AiChatMessage> out = new ArrayList<>();
        for (AiChatMessage message : messages) {
            if (message == null || message.getContent() == null) {
                continue;
            }
            String role = message.getRole() == null ? "" : message.getRole().trim().toLowerCase(Locale.ROOT);
            String content = message.getContent().trim();
            if (content.isEmpty()) {
                continue;
            }
            if (!"user".equals(role) && !"assistant".equals(role)) {
                continue;
            }
            AiChatMessage copy = new AiChatMessage();
            copy.setRole(role);
            copy.setContent(content);
            out.add(copy);
        }
        return out;
    }

    static boolean needsCompact(int messageCount, int threshold, int keepRecent) {
        return messageCount > threshold && keepRecent > 0 && messageCount > keepRecent;
    }

    static CompactSplit split(List<AiChatMessage> messages, int keepRecent) {
        int cut = messages.size() - keepRecent;
        return new CompactSplit(
                List.copyOf(messages.subList(0, cut)),
                List.copyOf(messages.subList(cut, messages.size())));
    }

    static String buildTranscript(List<AiChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (AiChatMessage message : messages) {
            String who = "user".equals(message.getRole()) ? "用户" : "助手";
            sb.append(who).append('：').append(message.getContent()).append('\n');
        }
        return sb.toString().trim();
    }

    static List<AiChatMessage> withSummary(String summary, List<AiChatMessage> recent) {
        List<AiChatMessage> out = new ArrayList<>(recent.size() + 2);
        AiChatMessage summaryUser = new AiChatMessage();
        summaryUser.setRole("user");
        summaryUser.setContent("【此前对话摘要】\n" + summary.trim());
        AiChatMessage ack = new AiChatMessage();
        ack.setRole("assistant");
        ack.setContent("好的，我已了解之前的对话要点。");
        out.add(summaryUser);
        out.add(ack);
        out.addAll(recent);
        return out;
    }

    record CompactSplit(List<AiChatMessage> older, List<AiChatMessage> recent) {
    }
}
