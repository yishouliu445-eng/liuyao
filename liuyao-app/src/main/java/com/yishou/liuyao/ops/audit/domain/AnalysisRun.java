package com.yishou.liuyao.ops.audit.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "analysis_run")
public class AnalysisRun extends BaseEntity {

    @Column(name = "execution_id", nullable = false, unique = true)
    private UUID executionId;

    @Column(name = "execution_mode", nullable = false)
    private String executionMode;

    @Column(name = "prompt_version")
    private String promptVersion;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "degradation_level")
    private String degradationLevel;

    @Column(name = "question_category")
    private String questionCategory;

    @Column(name = "question_text")
    private String questionText;

    @Column(name = "rag_source_count")
    private Integer ragSourceCount;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "payload_json")
    private String payloadJson;

    @Column(name = "use_god")
    private String useGod;

    @Column(name = "main_hexagram")
    private String mainHexagram;

    @Column(name = "changed_hexagram")
    private String changedHexagram;

    @Column(name = "degradation_reasons")
    private String degradationReasons;

    @Column(name = "validation_issue_count")
    private Integer validationIssueCount;

    @Column(name = "citation_count")
    private Integer citationCount;

    @Column(name = "analysis_conclusion")
    private String analysisConclusion;

    @Column(name = "legacy_analysis_text")
    private String legacyAnalysisText;

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionCategory() {
        return questionCategory;
    }

    public void setQuestionCategory(String questionCategory) {
        this.questionCategory = questionCategory;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getDegradationLevel() {
        return degradationLevel;
    }

    public void setDegradationLevel(String degradationLevel) {
        this.degradationLevel = degradationLevel;
    }

    public Integer getRagSourceCount() {
        return ragSourceCount;
    }

    public void setRagSourceCount(Integer ragSourceCount) {
        this.ragSourceCount = ragSourceCount;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getUseGod() {
        return useGod;
    }

    public void setUseGod(String useGod) {
        this.useGod = useGod;
    }

    public String getMainHexagram() {
        return mainHexagram;
    }

    public void setMainHexagram(String mainHexagram) {
        this.mainHexagram = mainHexagram;
    }

    public String getChangedHexagram() {
        return changedHexagram;
    }

    public void setChangedHexagram(String changedHexagram) {
        this.changedHexagram = changedHexagram;
    }

    public String getDegradationReasons() {
        return degradationReasons;
    }

    public void setDegradationReasons(String degradationReasons) {
        this.degradationReasons = degradationReasons;
    }

    public Integer getValidationIssueCount() {
        return validationIssueCount;
    }

    public void setValidationIssueCount(Integer validationIssueCount) {
        this.validationIssueCount = validationIssueCount;
    }

    public Integer getCitationCount() {
        return citationCount;
    }

    public void setCitationCount(Integer citationCount) {
        this.citationCount = citationCount;
    }

    public String getAnalysisConclusion() {
        return analysisConclusion;
    }

    public void setAnalysisConclusion(String analysisConclusion) {
        this.analysisConclusion = analysisConclusion;
    }

    public String getLegacyAnalysisText() {
        return legacyAnalysisText;
    }

    public void setLegacyAnalysisText(String legacyAnalysisText) {
        this.legacyAnalysisText = legacyAnalysisText;
    }
}
