package com.yishou.liuyao.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * LLM 编排式分析输出的结构化结果。
 *
 * <p>对应设计文档中的 JSON Schema 定义。前端直接消费此结构渲染对话界面。</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisOutputDTO {

    private HexagramAnalysis analysis;
    private AnalysisMetadata metadata;
    private List<String> smartPrompts;

    // ---- Inner Types ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HexagramAnalysis {
        /** 卦象概览（本变卦描述，nullable on follow-up） */
        private String hexagramOverview;
        /** 用神分析（nullable on follow-up） */
        private String useGodAnalysis;
        /** 详细推演 */
        private String detailedReasoning;
        /** 古籍引用列表 */
        private List<ClassicReference> classicReferences;
        /** 综合结论 */
        private String conclusion;
        /** 可操作的行动建议 */
        private List<String> actionPlan;
        /** AI推断的应期，null 表示无法判断 */
        private String predictedTimeline;
        /** 情绪语气: CALM / ENCOURAGING / CAUTIOUS */
        private String emotionalTone;

        public String getHexagramOverview() { return hexagramOverview; }
        public void setHexagramOverview(String hexagramOverview) { this.hexagramOverview = hexagramOverview; }
        public String getUseGodAnalysis() { return useGodAnalysis; }
        public void setUseGodAnalysis(String useGodAnalysis) { this.useGodAnalysis = useGodAnalysis; }
        public String getDetailedReasoning() { return detailedReasoning; }
        public void setDetailedReasoning(String detailedReasoning) { this.detailedReasoning = detailedReasoning; }
        public List<ClassicReference> getClassicReferences() { return classicReferences; }
        public void setClassicReferences(List<ClassicReference> classicReferences) { this.classicReferences = classicReferences; }
        public String getConclusion() { return conclusion; }
        public void setConclusion(String conclusion) { this.conclusion = conclusion; }
        public List<String> getActionPlan() { return actionPlan; }
        public void setActionPlan(List<String> actionPlan) { this.actionPlan = actionPlan; }
        public String getPredictedTimeline() { return predictedTimeline; }
        public void setPredictedTimeline(String predictedTimeline) { this.predictedTimeline = predictedTimeline; }
        public String getEmotionalTone() { return emotionalTone; }
        public void setEmotionalTone(String emotionalTone) { this.emotionalTone = emotionalTone; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClassicReference {
        /** 出处，如《增删卜易·用神章》 */
        private String source;
        /** 原文节选 */
        private String quote;
        /** 与本卦的关联说明 */
        private String relevance;

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getQuote() { return quote; }
        public void setQuote(String quote) { this.quote = quote; }
        public String getRelevance() { return relevance; }
        public void setRelevance(String relevance) { this.relevance = relevance; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnalysisMetadata {
        private double confidence;
        private String modelUsed;
        private int ragSourceCount;
        private int processingTimeMs;

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getModelUsed() { return modelUsed; }
        public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
        public int getRagSourceCount() { return ragSourceCount; }
        public void setRagSourceCount(int ragSourceCount) { this.ragSourceCount = ragSourceCount; }
        public int getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(int processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    }

    // ---- Root Getters/Setters ----

    public HexagramAnalysis getAnalysis() { return analysis; }
    public void setAnalysis(HexagramAnalysis analysis) { this.analysis = analysis; }
    public AnalysisMetadata getMetadata() { return metadata; }
    public void setMetadata(AnalysisMetadata metadata) { this.metadata = metadata; }
    public List<String> getSmartPrompts() { return smartPrompts; }
    public void setSmartPrompts(List<String> smartPrompts) { this.smartPrompts = smartPrompts; }
}
