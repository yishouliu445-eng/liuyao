package com.yishou.liuyao.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yishou.liuyao.analysis.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * LLM 表达层客户端。
 *
 * <p>职责：把机械化分析文本喂给 LLM，让其润色为自然语言。
 * LLM 不参与任何推理判断，只做「重组措辞、通顺表达」。</p>
 *
 * <p>若 LLM 不可用（无 Key、超时、或禁用），返回 null，上游自动降级。</p>
 */
@Component
public class LlmExpressionClient {

    private static final Logger log = LoggerFactory.getLogger(LlmExpressionClient.class);

    private static final String SYSTEM_PROMPT = """
            你是一位资深的六爻分析师助理，擅长将结构化的六爻推算结论用通顺、专业的语言表达出来。
            
            【重要规则】
            1. 你只负责语言表达和润色，绝对不能修改、推翻或替换任何推断结论。
            2. 输入中的"评分""结论""规则命中"等信息是推理引擎的最终结果，必须如实反映。
            3. 不要凭空添加卦象分析，不要自行演算任何五行生克。
            4. 输出应简洁、流畅，控制在300字以内，避免冗余重复。
            5. 语气应客观中立，像一位稳重的命理顾问在做总结。
            """;

    private final LlmProperties llmProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LlmExpressionClient(LlmProperties llmProperties,
                               RestTemplateBuilder restTemplateBuilder,
                               ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(llmProperties.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(llmProperties.getTimeoutMs()))
                .build();
    }

    /**
     * 尝试用 LLM 将机械分析文本润色为自然语言。
     *
     * @param mechanicalText 由 AnalysisSectionComposer 拼接的结构化机械文本
     * @param question       用户提问（为 LLM 提供问题语境）
     * @return 润色后的自然语言文本，或 null（表示需要降级）
     */
    public String refine(String mechanicalText, String question) {
        if (!llmProperties.isEnabled()) {
            return null;
        }
        String apiKey = llmProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("LLM API Key 未配置，跳过 LLM 表达层");
            return null;
        }
        try {
            String url = llmProperties.getBaseUrl().replaceAll("/$", "") + "/chat/completions";
            String userMessage = buildUserMessage(mechanicalText, question);
            String requestBody = buildRequestBody(userMessage);

            Map<String, String> headers = Map.of(
                    "Authorization", "Bearer " + apiKey,
                    "Content-Type", "application/json"
            );

            org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
            httpHeaders.set("Authorization", "Bearer " + apiKey);
            httpHeaders.set("Content-Type", "application/json");

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(requestBody, httpHeaders);

            String response = restTemplate.postForObject(url, entity, String.class);
            return extractContent(response);

        } catch (Exception e) {
            log.warn("LLM 表达层调用失败，降级为机械文本: {}", e.getMessage());
            return null;
        }
    }

    private String buildUserMessage(String mechanicalText, String question) {
        StringBuilder sb = new StringBuilder();
        if (question != null && !question.isBlank()) {
            sb.append("用户提问：").append(question).append("\n\n");
        }
        sb.append("以下是推理引擎的结构化分析结论，请你将其润色为通顺的自然语言，不得改变任何推断结果：\n\n");
        sb.append(mechanicalText);
        return sb.toString();
    }

    private String buildRequestBody(String userMessage) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", llmProperties.getModel());

        ArrayNode messages = root.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        root.put("temperature", 0.5);
        root.put("max_tokens", 600);

        return objectMapper.writeValueAsString(root);
    }

    private String extractContent(String responseJson) {
        if (responseJson == null || responseJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String text = content.asText("").trim();
            return text.isBlank() ? null : text;
        } catch (Exception e) {
            log.warn("解析 LLM 响应失败: {}", e.getMessage());
            return null;
        }
    }
}
