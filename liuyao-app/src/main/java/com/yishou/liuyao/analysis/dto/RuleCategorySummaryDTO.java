package com.yishou.liuyao.analysis.dto;

public class RuleCategorySummaryDTO {

    private String category;
    private Integer hitCount;
    private Integer score;
    private Integer effectiveHitCount;
    private Integer effectiveScore;
    private Integer stageOrder;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getEffectiveHitCount() {
        return effectiveHitCount;
    }

    public void setEffectiveHitCount(Integer effectiveHitCount) {
        this.effectiveHitCount = effectiveHitCount;
    }

    public Integer getEffectiveScore() {
        return effectiveScore;
    }

    public void setEffectiveScore(Integer effectiveScore) {
        this.effectiveScore = effectiveScore;
    }

    public Integer getStageOrder() {
        return stageOrder;
    }

    public void setStageOrder(Integer stageOrder) {
        this.stageOrder = stageOrder;
    }
}
