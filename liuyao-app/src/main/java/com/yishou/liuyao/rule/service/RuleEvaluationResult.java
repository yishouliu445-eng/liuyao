package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.rule.RuleHit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RuleEvaluationResult {

    private List<RuleHit> hits = new ArrayList<>();
    private Integer score;
    private String resultLevel;
    private Integer effectiveScore;
    private String effectiveResultLevel;
    private List<String> tags = new ArrayList<>();
    private List<String> effectiveRuleCodes = new ArrayList<>();
    private List<String> suppressedRuleCodes = new ArrayList<>();
    private String summary;
    private List<Map<String, Object>> categorySummaries = new ArrayList<>();
    private List<Map<String, Object>> conflictSummaries = new ArrayList<>();

    public List<RuleHit> getHits() {
        return hits;
    }

    public void setHits(List<RuleHit> hits) {
        this.hits = hits;
    }

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

    public List<Map<String, Object>> getCategorySummaries() {
        return categorySummaries;
    }

    public void setCategorySummaries(List<Map<String, Object>> categorySummaries) {
        this.categorySummaries = categorySummaries;
    }

    public List<Map<String, Object>> getConflictSummaries() {
        return conflictSummaries;
    }

    public void setConflictSummaries(List<Map<String, Object>> conflictSummaries) {
        this.conflictSummaries = conflictSummaries;
    }
}
