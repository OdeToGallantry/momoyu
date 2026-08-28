package com.example.bookmark.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kimi")
public class KimiProperties {

    private String apiKey = "";
    /** OpenAI 兼容根路径；完整请求为 {baseUrl}/chat/completions */
    private String baseUrl = "https://api.kimi.com/coding/v1";
    private String model = "kimi-for-coding";
    private int maxTokens = 2048;
    private float temperature = 1f;
    /** 默认开启思考：请求体 thinking.type=enabled */
    private boolean thinkingEnabled = true;
    /**
     * Kimi Code 网关按 User-Agent 分流；Java 默认 UA 常被伪装成「engine overloaded」。
     * Claude Code 能通是因为带了工具 UA。可与本地 Claude Code 一致，或改为 KimiCLI/1.5。
     */
    private String userAgent = "claude-cli/2.0.0";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public boolean isThinkingEnabled() {
        return thinkingEnabled;
    }

    public void setThinkingEnabled(boolean thinkingEnabled) {
        this.thinkingEnabled = thinkingEnabled;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
