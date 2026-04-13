package com.yishou.liuyao.analysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yishou.liuyao.analysis.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 测试环境下的 Mock LLM 客户端。
 *
 * <p>激活条件：{@code spring.profiles.active=test}（或 application-test.yml 配置 liuyao.llm.enabled=false）</p>
 *
 * <p>固定返回符合 JSON Schema 的分析结果，保证测试可重复且无 API 成本。</p>
 */
@Component
@Primary
@Profile({"test", "prompt-test"})
public class MockLlmClient extends LlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockLlmClient.class);

    private final ObjectMapper objectMapper;

    public MockLlmClient(LlmProperties props,
                          RestTemplateBuilder builder,
                          ObjectMapper objectMapper) {
        super(props, builder, objectMapper);
        this.objectMapper = objectMapper;
        log.info("MockLlmClient 已激活，所有 LLM 调用将返回固定 Mock 响应");
    }

    @Override
    public LlmResponse chat(List<ChatMessage> messages, String model, boolean forceJson) {
        log.debug("MockLlmClient.chat() called — returning fixed mock response");
        try {
            JsonNode parsed = buildMockResponse(messages);
            String raw = objectMapper.writeValueAsString(parsed);
            return LlmResponse.success(
                    raw, parsed,
                    "mock", 100, 200, 10
            );
        } catch (Exception e) {
            return LlmResponse.failure("Mock JSON解析失败: " + e.getMessage(), 0);
        }
    }

    private JsonNode buildMockResponse(List<ChatMessage> messages) {
        String initialQuestion = extractInitialQuestion(messages);
        String latestQuestion = extractLatestQuestion(messages);
        boolean negativeMode = isNegativeConversation(initialQuestion, latestQuestion);

        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode analysis = root.putObject("analysis");
        analysis.put("hexagramOverview", "【测试模式】本卦天火同人，象征协作共进，用神旺相。");
        analysis.put("useGodAnalysis", "用神官鬼处旺相，日月生扶，利于所问之事。");
        analysis.put(
                "detailedReasoning",
                "详细推演：初始问题聚焦“" + initialQuestion + "”，本轮追问聚焦“" + latestQuestion + "”，因此可验证多轮上下文已被带入。"
        );
        ArrayNode references = analysis.putArray("classicReferences");
        ObjectNode reference = references.addObject();
        reference.put("source", "《增删卜易·用神章》");
        reference.put("quote", "用神旺相则事可成");
        reference.put("relevance", "用神当令，与本卦情形相符");

        ArrayNode actionPlan = analysis.putArray("actionPlan");
        if (negativeMode) {
            analysis.put("conclusion", "综合来看，当前阻力偏大，需先稳住节奏再谋推进。");
            actionPlan.add("先缩小投入范围，优先验证关键风险点");
            actionPlan.add("尽快和相关方对齐预期，避免误判继续扩大");
            actionPlan.add("为最坏情况预留备选方案和时间缓冲");
            analysis.put("emotionalTone", "CAUTIOUS");
        } else {
            analysis.put(
                    "conclusion",
                    "综合来看，初始问题“" + initialQuestion + "”与本轮追问“" + latestQuestion + "”走势仍偏积极，建议把握时机稳步推进。"
            );
            actionPlan.add("近期可主动行动，时机较佳");
            actionPlan.add("注意人际协调，借助贵人之力");
            analysis.put("emotionalTone", "ENCOURAGING");
        }
        analysis.put("predictedTimeline", "一至两个月内有进展");

        ObjectNode metadata = root.putObject("metadata");
        metadata.put("confidence", 0.85);
        metadata.put("modelUsed", "mock");
        metadata.put("ragSourceCount", 1);
        metadata.put("processingTimeMs", 10);

        ArrayNode smartPrompts = root.putArray("smartPrompts");
        smartPrompts.add("空亡对结果有什么影响？");
        smartPrompts.add("如何把握最佳时机？");
        smartPrompts.add("有哪些风险需要注意？");
        return root;
    }

    private boolean isNegativeConversation(String initialQuestion, String latestQuestion) {
        String merged = (initialQuestion + " " + latestQuestion).toLowerCase();
        return merged.contains("失败")
                || merged.contains("不顺")
                || merged.contains("风险")
                || merged.contains("最坏")
                || merged.contains("不利")
                || merged.contains("应对");
    }

    private String extractInitialQuestion(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            if (!"user".equals(message.role())) {
                continue;
            }
            String normalized = normalizeUserContent(message.content());
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "当前问题";
    }

    private String extractLatestQuestion(List<ChatMessage> messages) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            ChatMessage message = messages.get(index);
            if (!"user".equals(message.role())) {
                continue;
            }
            String normalized = normalizeUserContent(message.content());
            if (!normalized.isBlank()) {
                return normalized;
            }
        }
        return "当前追问";
    }

    private String normalizeUserContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.strip();
        int knowledgeIndex = normalized.lastIndexOf("\n\n");
        if (normalized.startsWith("## 本轮相关古籍片段") && knowledgeIndex >= 0) {
            normalized = normalized.substring(knowledgeIndex + 2).strip();
        }
        if (normalized.startsWith("[历史摘要] 用户问：")) {
            normalized = normalized.substring("[历史摘要] 用户问：".length());
            int assistantIndex = normalized.indexOf("| AI答：");
            if (assistantIndex >= 0) {
                normalized = normalized.substring(0, assistantIndex);
            }
            normalized = normalized.strip();
        }
        return normalized;
    }
}
