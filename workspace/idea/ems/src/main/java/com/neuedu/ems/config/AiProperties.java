package com.neuedu.ems.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenAI 兼容模型接口配置。密钥只能通过后端环境变量注入。
 */
@Component
@ConfigurationProperties(prefix = "mes.ai")
public class AiProperties {

    private boolean enabled;
    private String apiUrl = "https://blackaicoding.com/";
    private String apiKey;
    private String model = "gpt-5.5";
    private String reasoningEffort = "high";
    private int maxTokens = 1200;
    private int maxContextChars = 5000;
    private int maxAnswerChars = 8000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }

    public void setMaxContextChars(int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }

    public int getMaxAnswerChars() {
        return maxAnswerChars;
    }

    public void setMaxAnswerChars(int maxAnswerChars) {
        this.maxAnswerChars = maxAnswerChars;
    }
}
