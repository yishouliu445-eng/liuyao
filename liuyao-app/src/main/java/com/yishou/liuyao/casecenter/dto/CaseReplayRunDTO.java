package com.yishou.liuyao.casecenter.dto;

import java.time.LocalDateTime;

public class CaseReplayRunDTO {

    private Long replayRunId;
    private Long caseId;
    private String questionText;
    private String questionCategory;
    private String ruleBundleVersion;
    private String ruleDefinitionsVersion;
    private String useGodRulesVersion;
    private String baselineRuleVersion;
    private String replayRuleVersion;
    private String baselineUseGodConfigVersion;
    private String replayUseGodConfigVersion;
    private Boolean recommendPersistReplay;
    private String persistenceAssessment;
    private Integer scoreDelta;
    private Integer effectiveScoreDelta;
    private Boolean resultLevelChanged;
    private Boolean summaryChanged;
    private Boolean analysisChanged;
    private String payloadJson;
    private LocalDateTime createdAt;

    public Long getReplayRunId() {
        return replayRunId;
    }

    public void setReplayRunId(Long replayRunId) {
        this.replayRunId = replayRunId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
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

    public String getRuleBundleVersion() {
        return ruleBundleVersion;
    }

    public void setRuleBundleVersion(String ruleBundleVersion) {
        this.ruleBundleVersion = ruleBundleVersion;
    }

    public String getRuleDefinitionsVersion() {
        return ruleDefinitionsVersion;
    }

    public void setRuleDefinitionsVersion(String ruleDefinitionsVersion) {
        this.ruleDefinitionsVersion = ruleDefinitionsVersion;
    }

    public String getUseGodRulesVersion() {
        return useGodRulesVersion;
    }

    public void setUseGodRulesVersion(String useGodRulesVersion) {
        this.useGodRulesVersion = useGodRulesVersion;
    }

    public String getBaselineRuleVersion() {
        return baselineRuleVersion;
    }

    public void setBaselineRuleVersion(String baselineRuleVersion) {
        this.baselineRuleVersion = baselineRuleVersion;
    }

    public String getReplayRuleVersion() {
        return replayRuleVersion;
    }

    public void setReplayRuleVersion(String replayRuleVersion) {
        this.replayRuleVersion = replayRuleVersion;
    }

    public String getBaselineUseGodConfigVersion() {
        return baselineUseGodConfigVersion;
    }

    public void setBaselineUseGodConfigVersion(String baselineUseGodConfigVersion) {
        this.baselineUseGodConfigVersion = baselineUseGodConfigVersion;
    }

    public String getReplayUseGodConfigVersion() {
        return replayUseGodConfigVersion;
    }

    public void setReplayUseGodConfigVersion(String replayUseGodConfigVersion) {
        this.replayUseGodConfigVersion = replayUseGodConfigVersion;
    }

    public Boolean getRecommendPersistReplay() {
        return recommendPersistReplay;
    }

    public void setRecommendPersistReplay(Boolean recommendPersistReplay) {
        this.recommendPersistReplay = recommendPersistReplay;
    }

    public String getPersistenceAssessment() {
        return persistenceAssessment;
    }

    public void setPersistenceAssessment(String persistenceAssessment) {
        this.persistenceAssessment = persistenceAssessment;
    }

    public Integer getScoreDelta() {
        return scoreDelta;
    }

    public void setScoreDelta(Integer scoreDelta) {
        this.scoreDelta = scoreDelta;
    }

    public Integer getEffectiveScoreDelta() {
        return effectiveScoreDelta;
    }

    public void setEffectiveScoreDelta(Integer effectiveScoreDelta) {
        this.effectiveScoreDelta = effectiveScoreDelta;
    }

    public Boolean getResultLevelChanged() {
        return resultLevelChanged;
    }

    public void setResultLevelChanged(Boolean resultLevelChanged) {
        this.resultLevelChanged = resultLevelChanged;
    }

    public Boolean getSummaryChanged() {
        return summaryChanged;
    }

    public void setSummaryChanged(Boolean summaryChanged) {
        this.summaryChanged = summaryChanged;
    }

    public Boolean getAnalysisChanged() {
        return analysisChanged;
    }

    public void setAnalysisChanged(Boolean analysisChanged) {
        this.analysisChanged = analysisChanged;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
