package com.example.bookmark.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.zhipu")
public class ZhipuProperties {

    private String apiKey = "";
    private String baseUrl = "https://open.bigmodel.cn/api/paas/v4";
    private String model = "glm-4.7-flash";
    private int maxTokens = 2048;
    private float temperature = 0.7f;
    /** 消息条数超过该值时，对更早的内容做摘要 */
    private int compactThreshold = 12;
    /** 摘要后仍保留的最近消息条数 */
    private int compactKeepRecent = 6;
    /** 摘要本身的 max_tokens */
    private int compactMaxTokens = 320;

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

    public int getCompactThreshold() {
        return compactThreshold;
    }

    public void setCompactThreshold(int compactThreshold) {
        this.compactThreshold = compactThreshold;
    }

    public int getCompactKeepRecent() {
        return compactKeepRecent;
    }

    public void setCompactKeepRecent(int compactKeepRecent) {
        this.compactKeepRecent = compactKeepRecent;
    }

    public int getCompactMaxTokens() {
        return compactMaxTokens;
    }

    public void setCompactMaxTokens(int compactMaxTokens) {
        this.compactMaxTokens = compactMaxTokens;
    }
}
