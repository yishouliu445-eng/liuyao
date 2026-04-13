package com.yishou.liuyao.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yishou.liuyao.analysis.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

/**
 * v2.0 LLM 客户端。
 *
 * <p>职责：
 * <ul>
 *   <li>支持 messages 数组（多轮上下文）</li>
 *   <li>强制 JSON 输出 (response_format: json_object)</li>
 *   <li>记录 Token 用量和延迟</li>
 *   <li>统一错误处理和降级标记</li>
 * </ul>
 * </p>
 *
 * <p>旧的 {@link LlmExpressionClient} 保留向后兼容，本类为 v2.0 新增。</p>
 */
@Component
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final LlmProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LlmClient(LlmProperties props,
                     RestTemplateBuilder builder,
                     ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(props.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(props.getTimeoutMs()))
                .build();
    }

    // ---- Public API ----

    /**
     * 发起一次 chat 请求。
     *
     * @param messages    消息数组（system + user + assistant 交替）
     * @param model       使用的模型名（null 则使用默认）
     * @param forceJson   是否强制 JSON 输出
     * @return {@link LlmResponse}，调用方通过 {@code response.success()} 判断是否降级
     */
    public LlmResponse chat(List<ChatMessage> messages, String model, boolean forceJson) {
        if (!props.isEnabled()) {
            return LlmResponse.disabled();
        }
        String apiKey = props.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("LLM API Key 未配置，跳过调用");
            return LlmResponse.disabled();
        }

        long startMs = System.currentTimeMillis();
        try {
            String url = props.getBaseUrl().replaceAll("/$", "") + "/chat/completions";
            String effectiveModel = (model != null && !model.isBlank()) ? model : props.getModel();
            String requestBody = buildRequestBody(messages, effectiveModel, forceJson);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");

            String rawResponse = restTemplate.postForObject(url,
                    new HttpEntity<>(requestBody, headers), String.class);

            int latencyMs = (int) (System.currentTimeMillis() - startMs);
            return parseResponse(rawResponse, effectiveModel, latencyMs, forceJson);

        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - startMs);
            log.warn("LLM 调用失败 ({}ms): {}", latencyMs, e.getMessage());
            return LlmResponse.failure(e.getMessage(), latencyMs);
        }
    }

    // ---- Private Helpers ----

    private String buildRequestBody(List<ChatMessage> messages, String model,
                                    boolean forceJson) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", props.getTemperature());
        root.put("max_tokens", props.getMaxTokens());

        ArrayNode msgArray = root.putArray("messages");
        for (ChatMessage msg : messages) {
            ObjectNode msgNode = msgArray.addObject();
            msgNode.put("role", msg.role());
            msgNode.put("content", msg.content());
        }

        if (forceJson) {
            ObjectNode format = root.putObject("response_format");
            format.put("type", "json_object");
        }

        return objectMapper.writeValueAsString(root);
    }

    private LlmResponse parseResponse(String rawJson, String model,
                                       int latencyMs, boolean forceJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return LlmResponse.failure("空响应", latencyMs);
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String content = root.path("choices").path(0)
                    .path("message").path("content").asText("").strip();

            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);

            log.info("LLM调用完成: model={}, promptTokens={}, completionTokens={}, latencyMs={}",
                    model, promptTokens, completionTokens, latencyMs);

            if (content.isBlank()) {
                return LlmResponse.failure("内容为空", latencyMs);
            }

            // 如果要求 JSON，清洗 markdown 包裹后解析
            if (forceJson) {
                content = stripMarkdownJson(content);
                JsonNode parsed = objectMapper.readTree(content);
                return LlmResponse.success(content, parsed, model, promptTokens, completionTokens, latencyMs);
            }

            return LlmResponse.success(content, null, model, promptTokens, completionTokens, latencyMs);

        } catch (Exception e) {
            log.warn("LLM 响应解析失败: {}", e.getMessage());
            return LlmResponse.failure("响应解析失败: " + e.getMessage(), latencyMs);
        }
    }

    /** 去除 LLM 可能包裹的 ```json ... ``` */
    private String stripMarkdownJson(String content) {
        String trimmed = content.strip();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.strip();
    }

    // ---- Value Types ----

    /**
     * 消息条目（value record）。
     */
    public record ChatMessage(String role, String content) {
        public static ChatMessage system(String content) { return new ChatMessage("system", content); }
        public static ChatMessage user(String content) { return new ChatMessage("user", content); }
        public static ChatMessage assistant(String content) { return new ChatMessage("assistant", content); }
    }

    /**
     * LLM 响应结果。调用方先检查 {@code success()}，再使用 {@code parsedJson()}。
     */
    public record LlmResponse(
            boolean success,
            String rawContent,
            JsonNode parsedJson,
            String model,
            int promptTokens,
            int completionTokens,
            int latencyMs,
            String errorMessage
    ) {
        public int totalTokens() { return promptTokens + completionTokens; }

        static LlmResponse success(String raw, JsonNode parsed, String model,
                                   int prompt, int completion, int latency) {
            return new LlmResponse(true, raw, parsed, model, prompt, completion, latency, null);
        }

        static LlmResponse failure(String error, int latency) {
            return new LlmResponse(false, null, null, null, 0, 0, latency, error);
        }

        static LlmResponse disabled() {
            return new LlmResponse(false, null, null, null, 0, 0, 0, "LLM disabled");
        }
    }
}
