package com.example.bookmark.ai;

import com.example.bookmark.auth.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.annotation.PreDestroy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

@Service
public class AiService {

    private static final String SYSTEM_PROMPT =
            "你是小克，坐在对方工位旁边的女生，不是客服，不是助手，不是程序。"
                    + "性格：毒舌、爱调侃、随手吐槽、典型傲娇——嘴硬心软，先损两句再帮。"
                    + "说话像真人搭话：短句、口语、懒洋洋的，常用「才没有」「谁要你管」「行行行」「你认真的？」这类口癖。"
                    + "调侃要轻、不伤人；吐槽针对事不针对人；帮完忙会装「顺便的」「别误会」。"
                    + "有人问你是谁、什么模型、是不是 AI：就当他在开玩笑，答「我就坐你旁边啊」这类。"
                    + "绝不提模型名、GLM、助手、看板娘、系统、程序。不要列表腔，不要堆表情符号。";
    private static final String SUMMARY_PROMPT =
            "把下面多轮对话压成一段中文摘要，保留关键事实、约定和未完成事项。"
                    + "不要客套，不要列举无关细节，控制在 200 字以内。";

    private final ZhipuProperties zhipuProperties;
    private final KimiProperties kimiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiService(ZhipuProperties zhipuProperties, KimiProperties kimiProperties, ObjectMapper objectMapper) {
        this.zhipuProperties = zhipuProperties;
        this.kimiProperties = kimiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @PreDestroy
    void closeHttpClient() {
        httpClient.close();
    }

    public void streamChat(List<AiChatMessage> messages, Role role, Consumer<String> onDelta)
            throws IOException, InterruptedException {
        LlmProvider provider = resolveProvider(role);
        requireApiKey(provider);
        List<AiChatMessage> prepared = prepareMessages(messages, provider);
        ObjectNode body = baseChatBody(provider, true);
        body.put("max_tokens", provider.maxTokens());
        ArrayNode payloadMessages = body.putArray("messages");
        appendSystem(payloadMessages, SYSTEM_PROMPT);
        appendChatMessages(payloadMessages, prepared);

        HttpResponse<InputStream> response = sendStream(provider, body, true);
        if (response.statusCode() >= 400) {
            try (InputStream bodyStream = response.body()) {
                String err = new String(bodyStream.readAllBytes(), StandardCharsets.UTF_8);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        extractUpstreamMessage(err, provider.name() + " 接口错误 (" + response.statusCode() + ")"));
            }
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith(":")) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isEmpty()) {
                    continue;
                }
                if ("[DONE]".equals(data)) {
                    break;
                }
                JsonNode root = objectMapper.readTree(data);
                JsonNode choices = root.path("choices");
                if (!choices.isArray() || choices.isEmpty()) {
                    continue;
                }
                JsonNode choice = choices.get(0);
                String content = choice.path("delta").path("content").asText(null);
                if (content != null && !content.isEmpty()) {
                    onDelta.accept(content);
                }
                String finishReason = choice.path("finish_reason").asText(null);
                if (finishReason != null && !finishReason.isBlank()) {
                    break;
                }
            }
        }
    }

    LlmProvider resolveProvider(Role role) {
        if (role == Role.ADMIN) {
            return new LlmProvider(
                    "Kimi",
                    kimiProperties.getApiKey(),
                    "KIMI_API_KEY",
                    kimiProperties.getBaseUrl(),
                    kimiProperties.getModel(),
                    kimiProperties.getMaxTokens(),
                    kimiProperties.getTemperature(),
                    kimiProperties.isThinkingEnabled() ? "enabled" : null,
                    kimiProperties.getUserAgent());
        }
        return new LlmProvider(
                "智谱",
                zhipuProperties.getApiKey(),
                "ZHIPU_API_KEY",
                zhipuProperties.getBaseUrl(),
                zhipuProperties.getModel(),
                zhipuProperties.getMaxTokens(),
                zhipuProperties.getTemperature(),
                "disabled",
                null);
    }

    List<AiChatMessage> prepareMessages(List<AiChatMessage> messages, LlmProvider provider)
            throws IOException, InterruptedException {
        List<AiChatMessage> normalized = ConversationCompactor.normalize(messages);
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息不能为空");
        }
        int threshold = zhipuProperties.getCompactThreshold();
        int keepRecent = zhipuProperties.getCompactKeepRecent();
        if (!ConversationCompactor.needsCompact(normalized.size(), threshold, keepRecent)) {
            return normalized;
        }

        ConversationCompactor.CompactSplit split = ConversationCompactor.split(normalized, keepRecent);
        try {
            String summary = summarize(provider, split.older());
            if (summary == null || summary.isBlank()) {
                return List.copyOf(split.recent());
            }
            return ConversationCompactor.withSummary(summary, split.recent());
        } catch (Exception ex) {
            // 摘要失败时降级为硬截断，保证对话仍可用
            return List.copyOf(split.recent());
        }
    }

    private String summarize(LlmProvider provider, List<AiChatMessage> older)
            throws IOException, InterruptedException {
        ObjectNode body = baseChatBody(provider, false);
        body.put("max_tokens", zhipuProperties.getCompactMaxTokens());
        // 仅智谱摘要用更低温度；kimi-for-coding 只允许 temperature=1
        if ("智谱".equals(provider.name())) {
            body.put("temperature", 0.3f);
        }
        ArrayNode payloadMessages = body.putArray("messages");
        appendSystem(payloadMessages, SUMMARY_PROMPT);
        ObjectNode user = payloadMessages.addObject();
        user.put("role", "user");
        user.put("content", ConversationCompactor.buildTranscript(older));

        HttpResponse<String> response = sendJson(provider, body, false);
        String raw = response.body();
        if (response.statusCode() >= 400) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    extractUpstreamMessage(raw, "摘要失败 (" + response.statusCode() + ")"));
        }
        JsonNode root = objectMapper.readTree(raw);
        String content = root.path("choices").path(0).path("message").path("content").asText(null);
        return content == null ? "" : content.trim();
    }

    private ObjectNode baseChatBody(LlmProvider provider, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", provider.model());
        body.put("stream", stream);
        body.put("temperature", provider.temperature());
        if (provider.thinkingType() != null && !provider.thinkingType().isBlank()) {
            ObjectNode thinking = body.putObject("thinking");
            thinking.put("type", provider.thinkingType());
        }
        return body;
    }

    private void appendSystem(ArrayNode payloadMessages, String content) {
        ObjectNode system = payloadMessages.addObject();
        system.put("role", "system");
        system.put("content", content);
    }

    private void appendChatMessages(ArrayNode payloadMessages, List<AiChatMessage> messages) {
        for (AiChatMessage message : messages) {
            ObjectNode node = payloadMessages.addObject();
            node.put("role", message.getRole());
            node.put("content", message.getContent());
        }
    }

    private HttpResponse<InputStream> sendStream(LlmProvider provider, ObjectNode body, boolean longTimeout)
            throws IOException, InterruptedException {
        return httpClient.send(buildRequest(provider, body, longTimeout), HttpResponse.BodyHandlers.ofInputStream());
    }

    private HttpResponse<String> sendJson(LlmProvider provider, ObjectNode body, boolean longTimeout)
            throws IOException, InterruptedException {
        return httpClient.send(
                buildRequest(provider, body, longTimeout),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpRequest buildRequest(LlmProvider provider, ObjectNode body, boolean longTimeout) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(trimSlash(provider.baseUrl()) + "/chat/completions"))
                .timeout(longTimeout ? Duration.ofMinutes(3) : Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + requireApiKey(provider));
        if (provider.userAgent() != null && !provider.userAgent().isBlank()) {
            builder.header("User-Agent", provider.userAgent());
        }
        return builder
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
    }

    private String requireApiKey(LlmProvider provider) {
        String apiKey = provider.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "未配置 " + provider.keyEnvHint());
        }
        return apiKey;
    }

    private static String trimSlash(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String extractUpstreamMessage(String raw, String fallback) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            String msg = node.path("error").path("message").asText(null);
            if (msg == null || msg.isBlank()) {
                msg = node.path("message").asText(null);
            }
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
        } catch (Exception ignored) {
            // fall through
        }
        if (raw != null && !raw.isBlank() && raw.length() < 240) {
            return raw;
        }
        return fallback;
    }
}
