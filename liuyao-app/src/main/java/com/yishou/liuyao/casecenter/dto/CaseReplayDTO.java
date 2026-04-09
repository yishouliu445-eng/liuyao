package com.yishou.liuyao.casecenter.dto;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.rule.dto.RuleHitDTO;

import java.util.ArrayList;
import java.util.List;

public class CaseReplayDTO {

    private Long caseId;
    private List<String> baselineRuleCodes = new ArrayList<>();
    private List<String> replayRuleCodes = new ArrayList<>();
    private List<String> addedRuleCodes = new ArrayList<>();
    private List<String> removedRuleCodes = new ArrayList<>();
    private List<String> baselineEffectiveRuleCodes = new ArrayList<>();
    private List<String> replayEffectiveRuleCodes = new ArrayList<>();
    private List<String> addedEffectiveRuleCodes = new ArrayList<>();
    private List<String> removedEffectiveRuleCodes = new ArrayList<>();
    private List<String> baselineSuppressedRuleCodes = new ArrayList<>();
    private List<String> replaySuppressedRuleCodes = new ArrayList<>();
    private List<String> addedSuppressedRuleCodes = new ArrayList<>();
    private List<String> removedSuppressedRuleCodes = new ArrayList<>();
    private List<String> baselineTags = new ArrayList<>();
    private List<String> replayTags = new ArrayList<>();
    private List<String> addedTags = new ArrayList<>();
    private List<String> removedTags = new ArrayList<>();
    private Integer baselineScore;
    private Integer replayScore;
    private Integer scoreDelta;
    private Integer baselineEffectiveScore;
    private Integer replayEffectiveScore;
    private Integer effectiveScoreDelta;
    private String ruleBundleVersion;
    private String ruleDefinitionsVersion;
    private String useGodRulesVersion;
    private String baselineRuleVersion;
    private String replayRuleVersion;
    private String baselineUseGodConfigVersion;
    private String replayUseGodConfigVersion;
    private String baselineResultLevel;
    private String replayResultLevel;
    private Boolean resultLevelChanged;
    private String baselineSummary;
    private String replaySummary;
    private Boolean summaryChanged;
    private String baselineAnalysis;
    private String replayAnalysis;
    private Boolean analysisChanged;
    private Boolean recommendPersistReplay;
    private String persistenceAssessment;
    private AnalysisContextDTO baselineAnalysisContext;
    private AnalysisContextDTO replayAnalysisContext;
    private StructuredAnalysisResultDTO baselineStructuredResult;
    private StructuredAnalysisResultDTO replayStructuredResult;
    private List<RuleHitDTO> baselineRuleHits = new ArrayList<>();
    private List<RuleHitDTO> replayRuleHits = new ArrayList<>();

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public List<String> getBaselineRuleCodes() {
        return baselineRuleCodes;
    }

    public void setBaselineRuleCodes(List<String> baselineRuleCodes) {
        this.baselineRuleCodes = baselineRuleCodes;
    }

    public List<String> getReplayRuleCodes() {
        return replayRuleCodes;
    }

    public void setReplayRuleCodes(List<String> replayRuleCodes) {
        this.replayRuleCodes = replayRuleCodes;
    }

    public List<String> getAddedRuleCodes() {
        return addedRuleCodes;
    }

    public void setAddedRuleCodes(List<String> addedRuleCodes) {
        this.addedRuleCodes = addedRuleCodes;
    }

    public List<String> getRemovedRuleCodes() {
        return removedRuleCodes;
    }

    public void setRemovedRuleCodes(List<String> removedRuleCodes) {
        this.removedRuleCodes = removedRuleCodes;
    }

    public Integer getBaselineScore() {
        return baselineScore;
    }

    public void setBaselineScore(Integer baselineScore) {
        this.baselineScore = baselineScore;
    }

    public Integer getReplayScore() {
        return replayScore;
    }

    public void setReplayScore(Integer replayScore) {
        this.replayScore = replayScore;
    }

    public Integer getScoreDelta() {
        return scoreDelta;
    }

    public void setScoreDelta(Integer scoreDelta) {
        this.scoreDelta = scoreDelta;
    }

    public Integer getBaselineEffectiveScore() {
        return baselineEffectiveScore;
    }

    public void setBaselineEffectiveScore(Integer baselineEffectiveScore) {
        this.baselineEffectiveScore = baselineEffectiveScore;
    }

    public Integer getReplayEffectiveScore() {
        return replayEffectiveScore;
    }

    public void setReplayEffectiveScore(Integer replayEffectiveScore) {
        this.replayEffectiveScore = replayEffectiveScore;
    }

    public Integer getEffectiveScoreDelta() {
        return effectiveScoreDelta;
    }

    public void setEffectiveScoreDelta(Integer effectiveScoreDelta) {
        this.effectiveScoreDelta = effectiveScoreDelta;
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

    public List<String> getBaselineEffectiveRuleCodes() {
        return baselineEffectiveRuleCodes;
    }

    public void setBaselineEffectiveRuleCodes(List<String> baselineEffectiveRuleCodes) {
        this.baselineEffectiveRuleCodes = baselineEffectiveRuleCodes;
    }

    public List<String> getReplayEffectiveRuleCodes() {
        return replayEffectiveRuleCodes;
    }

    public void setReplayEffectiveRuleCodes(List<String> replayEffectiveRuleCodes) {
        this.replayEffectiveRuleCodes = replayEffectiveRuleCodes;
    }

    public List<String> getAddedEffectiveRuleCodes() {
        return addedEffectiveRuleCodes;
    }

    public void setAddedEffectiveRuleCodes(List<String> addedEffectiveRuleCodes) {
        this.addedEffectiveRuleCodes = addedEffectiveRuleCodes;
    }

    public List<String> getRemovedEffectiveRuleCodes() {
        return removedEffectiveRuleCodes;
    }

    public void setRemovedEffectiveRuleCodes(List<String> removedEffectiveRuleCodes) {
        this.removedEffectiveRuleCodes = removedEffectiveRuleCodes;
    }

    public List<String> getBaselineSuppressedRuleCodes() {
        return baselineSuppressedRuleCodes;
    }

    public void setBaselineSuppressedRuleCodes(List<String> baselineSuppressedRuleCodes) {
        this.baselineSuppressedRuleCodes = baselineSuppressedRuleCodes;
    }

    public List<String> getReplaySuppressedRuleCodes() {
        return replaySuppressedRuleCodes;
    }

    public void setReplaySuppressedRuleCodes(List<String> replaySuppressedRuleCodes) {
        this.replaySuppressedRuleCodes = replaySuppressedRuleCodes;
    }

    public List<String> getAddedSuppressedRuleCodes() {
        return addedSuppressedRuleCodes;
    }

    public void setAddedSuppressedRuleCodes(List<String> addedSuppressedRuleCodes) {
        this.addedSuppressedRuleCodes = addedSuppressedRuleCodes;
    }

    public List<String> getRemovedSuppressedRuleCodes() {
        return removedSuppressedRuleCodes;
    }

    public void setRemovedSuppressedRuleCodes(List<String> removedSuppressedRuleCodes) {
        this.removedSuppressedRuleCodes = removedSuppressedRuleCodes;
    }

    public List<String> getBaselineTags() {
        return baselineTags;
    }

    public void setBaselineTags(List<String> baselineTags) {
        this.baselineTags = baselineTags;
    }

    public List<String> getReplayTags() {
        return replayTags;
    }

    public void setReplayTags(List<String> replayTags) {
        this.replayTags = replayTags;
    }

    public List<String> getAddedTags() {
        return addedTags;
    }

    public void setAddedTags(List<String> addedTags) {
        this.addedTags = addedTags;
    }

    public List<String> getRemovedTags() {
        return removedTags;
    }

    public void setRemovedTags(List<String> removedTags) {
        this.removedTags = removedTags;
    }

    public String getBaselineResultLevel() {
        return baselineResultLevel;
    }

    public void setBaselineResultLevel(String baselineResultLevel) {
        this.baselineResultLevel = baselineResultLevel;
    }

    public String getReplayResultLevel() {
        return replayResultLevel;
    }

    public void setReplayResultLevel(String replayResultLevel) {
        this.replayResultLevel = replayResultLevel;
    }

    public Boolean getResultLevelChanged() {
        return resultLevelChanged;
    }

    public void setResultLevelChanged(Boolean resultLevelChanged) {
        this.resultLevelChanged = resultLevelChanged;
    }

    public String getBaselineSummary() {
        return baselineSummary;
    }

    public void setBaselineSummary(String baselineSummary) {
        this.baselineSummary = baselineSummary;
    }

    public String getReplaySummary() {
        return replaySummary;
    }

    public void setReplaySummary(String replaySummary) {
        this.replaySummary = replaySummary;
    }

    public Boolean getSummaryChanged() {
        return summaryChanged;
    }

    public void setSummaryChanged(Boolean summaryChanged) {
        this.summaryChanged = summaryChanged;
    }

    public String getBaselineAnalysis() {
        return baselineAnalysis;
    }

    public void setBaselineAnalysis(String baselineAnalysis) {
        this.baselineAnalysis = baselineAnalysis;
    }

    public String getReplayAnalysis() {
        return replayAnalysis;
    }

    public void setReplayAnalysis(String replayAnalysis) {
        this.replayAnalysis = replayAnalysis;
    }

    public Boolean getAnalysisChanged() {
        return analysisChanged;
    }

    public void setAnalysisChanged(Boolean analysisChanged) {
        this.analysisChanged = analysisChanged;
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

    public AnalysisContextDTO getBaselineAnalysisContext() {
        return baselineAnalysisContext;
    }

    public void setBaselineAnalysisContext(AnalysisContextDTO baselineAnalysisContext) {
        this.baselineAnalysisContext = baselineAnalysisContext;
    }

    public AnalysisContextDTO getReplayAnalysisContext() {
        return replayAnalysisContext;
    }

    public void setReplayAnalysisContext(AnalysisContextDTO replayAnalysisContext) {
        this.replayAnalysisContext = replayAnalysisContext;
    }

    public StructuredAnalysisResultDTO getReplayStructuredResult() {
        return replayStructuredResult;
    }

    public void setReplayStructuredResult(StructuredAnalysisResultDTO replayStructuredResult) {
        this.replayStructuredResult = replayStructuredResult;
    }

    public StructuredAnalysisResultDTO getBaselineStructuredResult() {
        return baselineStructuredResult;
    }

    public void setBaselineStructuredResult(StructuredAnalysisResultDTO baselineStructuredResult) {
        this.baselineStructuredResult = baselineStructuredResult;
    }

    public List<RuleHitDTO> getBaselineRuleHits() {
        return baselineRuleHits;
    }

    public void setBaselineRuleHits(List<RuleHitDTO> baselineRuleHits) {
        this.baselineRuleHits = baselineRuleHits;
    }

    public List<RuleHitDTO> getReplayRuleHits() {
        return replayRuleHits;
    }

    public void setReplayRuleHits(List<RuleHitDTO> replayRuleHits) {
        this.replayRuleHits = replayRuleHits;
    }
}
