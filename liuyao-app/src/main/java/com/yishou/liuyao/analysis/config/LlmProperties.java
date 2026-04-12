package com.yishou.liuyao.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "liuyao.llm")
public class LlmProperties {

    private boolean enabled = false;
    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private String apiKey = "";
    private String model = "qwen-plus";
    /** 追问时使用的更小模型，节省成本 */
    private String followupModel = "qwen-turbo";
    private int timeoutMs = 15000;
    private int maxTokens = 1500;
    private double temperature = 0.5;
    private boolean forceJson = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getFollowupModel() { return followupModel; }
    public void setFollowupModel(String followupModel) { this.followupModel = followupModel; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public boolean isForceJson() { return forceJson; }
    public void setForceJson(boolean forceJson) { this.forceJson = forceJson; }
}
