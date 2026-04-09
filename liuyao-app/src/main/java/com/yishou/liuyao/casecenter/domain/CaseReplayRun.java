package com.yishou.liuyao.casecenter.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "case_replay_run")
public class CaseReplayRun extends BaseEntity {

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "question_text")
    private String questionText;

    @Column(name = "question_category")
    private String questionCategory;

    @Column(name = "rule_bundle_version")
    private String ruleBundleVersion;

    @Column(name = "rule_definitions_version")
    private String ruleDefinitionsVersion;

    @Column(name = "use_god_rules_version")
    private String useGodRulesVersion;

    @Column(name = "baseline_rule_version")
    private String baselineRuleVersion;

    @Column(name = "replay_rule_version")
    private String replayRuleVersion;

    @Column(name = "baseline_use_god_config_version")
    private String baselineUseGodConfigVersion;

    @Column(name = "replay_use_god_config_version")
    private String replayUseGodConfigVersion;

    @Column(name = "recommend_persist_replay")
    private Boolean recommendPersistReplay;

    @Column(name = "persistence_assessment")
    private String persistenceAssessment;

    @Column(name = "score_delta")
    private Integer scoreDelta;

    @Column(name = "effective_score_delta")
    private Integer effectiveScoreDelta;

    @Column(name = "result_level_changed")
    private Boolean resultLevelChanged;

    @Column(name = "summary_changed")
    private Boolean summaryChanged;

    @Column(name = "analysis_changed")
    private Boolean analysisChanged;

    @Column(name = "payload_json")
    private String payloadJson;

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
}
