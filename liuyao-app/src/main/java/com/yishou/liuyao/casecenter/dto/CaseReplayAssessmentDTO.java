package com.yishou.liuyao.casecenter.dto;

import java.time.LocalDateTime;

public class CaseReplayAssessmentDTO {

    private Long caseId;
    private String questionText;
    private String questionCategory;
    private LocalDateTime divinationTime;
    private Boolean recommendPersistReplay;
    private String persistenceAssessment;
    private String ruleBundleVersion;
    private String replayRuleVersion;
    private Integer scoreDelta;
    private Integer effectiveScoreDelta;

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

    public LocalDateTime getDivinationTime() {
        return divinationTime;
    }

    public void setDivinationTime(LocalDateTime divinationTime) {
        this.divinationTime = divinationTime;
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

    public String getRuleBundleVersion() {
        return ruleBundleVersion;
    }

    public void setRuleBundleVersion(String ruleBundleVersion) {
        this.ruleBundleVersion = ruleBundleVersion;
    }

    public String getReplayRuleVersion() {
        return replayRuleVersion;
    }

    public void setReplayRuleVersion(String replayRuleVersion) {
        this.replayRuleVersion = replayRuleVersion;
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
}
