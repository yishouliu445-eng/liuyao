package com.yishou.liuyao.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "liuyao.llm")
public class LlmProperties {

    /**
     * 是否启用 LLM 表达层。默认 false，代表纯机械拼文模式。
     */
    private boolean enabled = false;

    /**
     * OpenAI-compatible base URL。
     * 支持通义千问、月之暗面等兼容 OpenAI 接口的模型入口。
     */
    private String baseUrl = "https://api.openai.com/v1";

    /**
     * API Key。
     */
    private String apiKey = "";

    /**
     * 模型名称，默认 gpt-4o。
     */
    private String model = "gpt-4o";

    /**
     * 请求超时（毫秒），超时后自动降级到机械文案。
     */
    private int timeoutMs = 8000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
