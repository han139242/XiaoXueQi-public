package com.neuedu.ems.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek 官方兼容接口配置。
 */
@Component
@ConfigurationProperties(prefix = "mes.ai")
public class AiProperties {

    private boolean enabled;
    private String apiUrl = "https://api.deepseek.com";
    private String apiKey;
    private String model = "deepseek-chat";
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
