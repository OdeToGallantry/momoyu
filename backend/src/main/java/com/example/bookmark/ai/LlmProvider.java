package com.example.bookmark.ai;

/**
 * 当前对话选用的上游模型配置（GLM / Kimi 均为 OpenAI 兼容接口）。
 *
 * @param thinkingType 请求体 thinking.type；null 表示不传该字段
 * @param userAgent    仅 Kimi Code 需要；null 表示不覆盖默认 UA
 */
record LlmProvider(
        String name,
        String apiKey,
        String keyEnvHint,
        String baseUrl,
        String model,
        int maxTokens,
        float temperature,
        String thinkingType,
        String userAgent
) {
}
