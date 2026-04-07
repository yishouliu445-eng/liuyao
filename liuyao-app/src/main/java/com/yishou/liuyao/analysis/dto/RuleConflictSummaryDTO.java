package com.yishou.liuyao.analysis.dto;

import java.util.ArrayList;
import java.util.List;

public class RuleConflictSummaryDTO {

    private String category;
    private Integer positiveCount;
    private Integer negativeCount;
    private Integer positiveScore;
    private Integer negativeScore;
    private Integer netScore;
    private String decision;
    private List<String> positiveRules = new ArrayList<>();
    private List<String> negativeRules = new ArrayList<>();
    private List<String> effectiveRules = new ArrayList<>();
    private List<String> suppressedRules = new ArrayList<>();

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getPositiveCount() {
        return positiveCount;
    }

    public void setPositiveCount(Integer positiveCount) {
        this.positiveCount = positiveCount;
    }

    public Integer getNegativeCount() {
        return negativeCount;
    }

    public void setNegativeCount(Integer negativeCount) {
        this.negativeCount = negativeCount;
    }

    public Integer getPositiveScore() {
        return positiveScore;
    }

    public void setPositiveScore(Integer positiveScore) {
        this.positiveScore = positiveScore;
    }

    public Integer getNegativeScore() {
        return negativeScore;
    }

    public void setNegativeScore(Integer negativeScore) {
        this.negativeScore = negativeScore;
    }

    public Integer getNetScore() {
        return netScore;
    }

    public void setNetScore(Integer netScore) {
        this.netScore = netScore;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public List<String> getPositiveRules() {
        return positiveRules;
    }

    public void setPositiveRules(List<String> positiveRules) {
        this.positiveRules = positiveRules;
    }

    public List<String> getNegativeRules() {
        return negativeRules;
    }

    public void setNegativeRules(List<String> negativeRules) {
        this.negativeRules = negativeRules;
    }

    public List<String> getEffectiveRules() {
        return effectiveRules;
    }

    public void setEffectiveRules(List<String> effectiveRules) {
        this.effectiveRules = effectiveRules;
    }

    public List<String> getSuppressedRules() {
        return suppressedRules;
    }

    public void setSuppressedRules(List<String> suppressedRules) {
        this.suppressedRules = suppressedRules;
    }
}
