package com.yishou.liuyao.analysis.dto;

import java.util.ArrayList;
import java.util.List;

public class StructuredAnalysisResultDTO {

    private Integer score;
    private String resultLevel;
    private Integer effectiveScore;
    private String effectiveResultLevel;
    private List<String> tags = new ArrayList<>();
    private List<String> effectiveRuleCodes = new ArrayList<>();
    private List<String> suppressedRuleCodes = new ArrayList<>();
    private String summary;
    private List<RuleCategorySummaryDTO> categorySummaries = new ArrayList<>();
    private List<RuleConflictSummaryDTO> conflictSummaries = new ArrayList<>();

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getResultLevel() {
        return resultLevel;
    }

    public void setResultLevel(String resultLevel) {
        this.resultLevel = resultLevel;
    }

    public Integer getEffectiveScore() {
        return effectiveScore;
    }

    public void setEffectiveScore(Integer effectiveScore) {
        this.effectiveScore = effectiveScore;
    }

    public String getEffectiveResultLevel() {
        return effectiveResultLevel;
    }

    public void setEffectiveResultLevel(String effectiveResultLevel) {
        this.effectiveResultLevel = effectiveResultLevel;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getEffectiveRuleCodes() {
        return effectiveRuleCodes;
    }

    public void setEffectiveRuleCodes(List<String> effectiveRuleCodes) {
        this.effectiveRuleCodes = effectiveRuleCodes;
    }

    public List<String> getSuppressedRuleCodes() {
        return suppressedRuleCodes;
    }

    public void setSuppressedRuleCodes(List<String> suppressedRuleCodes) {
        this.suppressedRuleCodes = suppressedRuleCodes;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<RuleCategorySummaryDTO> getCategorySummaries() {
        return categorySummaries;
    }

    public void setCategorySummaries(List<RuleCategorySummaryDTO> categorySummaries) {
        this.categorySummaries = categorySummaries;
    }

    public List<RuleConflictSummaryDTO> getConflictSummaries() {
        return conflictSummaries;
    }

    public void setConflictSummaries(List<RuleConflictSummaryDTO> conflictSummaries) {
        this.conflictSummaries = conflictSummaries;
    }
}
