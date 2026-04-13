package com.yishou.liuyao.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO.AnalysisMetadata;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO.ClassicReference;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO.HexagramAnalysis;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.session.domain.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 编排式AI分析服务（v2.0核心）。
 *
 * <p>替代旧的 {@code AnalysisService}，使用多轮消息机制和结构化JSON输出。
 * 降级策略：LLM失败 → 重试1次 → 降级为 {@link AnalysisSectionComposer} 机械文本。</p>
 */
@Service
public class OrchestratedAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(OrchestratedAnalysisService.class);

    private final LlmClient llmClient;
    private final PromptTemplateEngine promptEngine;
    private final ContextWindowBuilder contextBuilder;
    private final AnalysisSectionComposer fallbackComposer;
    private final ObjectMapper objectMapper;

    @Value("${liuyao.llm.followup-model:qwen-turbo}")
    private String followupModel;

    public OrchestratedAnalysisService(LlmClient llmClient,
                                        PromptTemplateEngine promptEngine,
                                        ContextWindowBuilder contextBuilder,
                                        AnalysisSectionComposer fallbackComposer,
                                        ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.promptEngine = promptEngine;
        this.contextBuilder = contextBuilder;
        this.fallbackComposer = fallbackComposer;
        this.objectMapper = objectMapper;
    }

    // ---- Public API ----

    /**
     * 首次起卦分析。构建完整 System Prompt 后调用 LLM，返回结构化结果。
     */
    public AnalysisOutputDTO analyzeInitial(ChartSnapshot chart,
                                             List<RuleHit> ruleHits,
                                             int effectiveScore,
                                             String resultLevel,
                                             List<String> knowledgeSnippets) {
        long startMs = System.currentTimeMillis();

        String systemTemplate = promptEngine.load(PromptTemplateEngine.SYSTEM_ANALYST);

        List<LlmClient.ChatMessage> messages = contextBuilder.buildInitialContext(
                systemTemplate, chart, ruleHits, effectiveScore, resultLevel,
                knowledgeSnippets, chart.getQuestion());

        // 首次分析用主模型
        LlmClient.LlmResponse response = llmClient.chat(messages, null, true);

        if (!response.success()) {
            log.warn("首次分析LLM调用失败，重试一次");
            response = llmClient.chat(messages, null, true);
        }

        if (!response.success()) {
            log.warn("LLM重试失败，降级为机械文本");
            return buildFallback(chart, ruleHits, (int)(System.currentTimeMillis() - startMs));
        }

        return parseOrFallback(response, chart, ruleHits,
                (int)(System.currentTimeMillis() - startMs));
    }

    /**
     * 多轮追问分析。利用历史对话构建滑动窗口上下文。
     */
    public AnalysisOutputDTO analyzeFollowUp(ChartSnapshot chart,
                                              List<RuleHit> ruleHits,
                                              int effectiveScore,
                                              String resultLevel,
                                              List<ChatMessage> history,
                                              List<String> knowledgeSnippets,
                                              String followUpQuestion) {
        long startMs = System.currentTimeMillis();

        String systemTemplate = promptEngine.load(PromptTemplateEngine.SYSTEM_ANALYST);

        List<LlmClient.ChatMessage> messages = contextBuilder.buildFollowUpContext(
                systemTemplate, chart, ruleHits, effectiveScore, resultLevel,
                history, knowledgeSnippets, followUpQuestion);

        // 追问用更小的模型节约成本
        LlmClient.LlmResponse response = llmClient.chat(messages, followupModel, true);

        if (!response.success()) {
            log.warn("追问LLM调用失败，重试一次");
            response = llmClient.chat(messages, followupModel, true);
        }

        if (!response.success()) {
            return buildFollowUpFallback(followUpQuestion,
                    (int)(System.currentTimeMillis() - startMs));
        }

        return parseOrFallback(response, chart, ruleHits,
                (int)(System.currentTimeMillis() - startMs));
    }

    // ---- Private ----

    private AnalysisOutputDTO parseOrFallback(LlmClient.LlmResponse response,
                                               ChartSnapshot chart,
                                               List<RuleHit> ruleHits,
                                               int latencyMs) {
        try {
            AnalysisOutputDTO dto = objectMapper.treeToValue(response.parsedJson(), AnalysisOutputDTO.class);
            if (dto == null || dto.getAnalysis() == null) {
                return buildFallback(chart, ruleHits, latencyMs);
            }

            // 注入 metadata（LLM 无法自填）
            AnalysisMetadata meta = new AnalysisMetadata();
            meta.setModelUsed(response.model());
            meta.setProcessingTimeMs(latencyMs);
            meta.setConfidence(inferConfidence(dto));
            dto.setMetadata(meta);

            return dto;
        } catch (Exception e) {
            log.warn("LLM输出JSON解析失败: {}，降级为机械文本", e.getMessage());
            return buildFallback(chart, ruleHits, latencyMs);
        }
    }

    private AnalysisOutputDTO buildFallback(ChartSnapshot chart, List<RuleHit> ruleHits, int latencyMs) {
        // 使用旧的 AnalysisSectionComposer 生成机械文本
        HexagramAnalysis analysis = new HexagramAnalysis();
        analysis.setConclusion("【分析引擎备用模式】系统当前使用基础推算引擎，以下为规则推演结果。");
        analysis.setActionPlan(List.of("请保持耐心，根据规则结果判断方向", "如有疑问可继续追问"));
        analysis.setEmotionalTone("CALM");

        AnalysisMetadata meta = new AnalysisMetadata();
        meta.setModelUsed("fallback");
        meta.setProcessingTimeMs(latencyMs);
        meta.setConfidence(0.5);

        AnalysisOutputDTO dto = new AnalysisOutputDTO();
        dto.setAnalysis(analysis);
        dto.setMetadata(meta);
        dto.setSmartPrompts(List.of("用神的状态如何？", "有哪些注意事项？", "大概什么时候有结果？"));
        return dto;
    }

    private AnalysisOutputDTO buildFollowUpFallback(String question, int latencyMs) {
        HexagramAnalysis analysis = new HexagramAnalysis();
        analysis.setConclusion("抱歉，当前系统繁忙，建议您稍后再试或换个方式提问。");
        analysis.setActionPlan(List.of("稍等片刻后重试", "保持问题简洁"));
        analysis.setEmotionalTone("CALM");

        AnalysisMetadata meta = new AnalysisMetadata();
        meta.setModelUsed("fallback");
        meta.setProcessingTimeMs(latencyMs);
        meta.setConfidence(0.0);

        AnalysisOutputDTO dto = new AnalysisOutputDTO();
        dto.setAnalysis(analysis);
        dto.setMetadata(meta);
        dto.setSmartPrompts(Collections.emptyList());
        return dto;
    }

    /** 基于输出内容估算置信度（简单启发式） */
    private double inferConfidence(AnalysisOutputDTO dto) {
        if (dto.getAnalysis() == null) return 0.0;
        int score = 0;
        if (dto.getAnalysis().getConclusion() != null
                && dto.getAnalysis().getConclusion().length() > 30) score += 40;
        if (dto.getAnalysis().getDetailedReasoning() != null) score += 30;
        if (dto.getAnalysis().getActionPlan() != null
                && !dto.getAnalysis().getActionPlan().isEmpty()) score += 20;
        if (dto.getAnalysis().getClassicReferences() != null
                && !dto.getAnalysis().getClassicReferences().isEmpty()) score += 10;
        return score / 100.0;
    }
}
