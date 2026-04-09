package com.yishou.liuyao.rule.dto;

import java.time.LocalDateTime;

public class RuleCandidateDTO {
    private Long id;
    private Long bookChunkId;
    private Long taskId;
    private String ruleTitle;
    private String category;
    private String conditionDesc;
    private String effectDirection;
    private String sourceBook;
    private String evidenceText;
    private Double confidence;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookChunkId() {
        return bookChunkId;
    }

    public void setBookChunkId(Long bookChunkId) {
        this.bookChunkId = bookChunkId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getRuleTitle() {
        return ruleTitle;
    }

    public void setRuleTitle(String ruleTitle) {
        this.ruleTitle = ruleTitle;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getConditionDesc() {
        return conditionDesc;
    }

    public void setConditionDesc(String conditionDesc) {
        this.conditionDesc = conditionDesc;
    }

    public String getEffectDirection() {
        return effectDirection;
    }

    public void setEffectDirection(String effectDirection) {
        this.effectDirection = effectDirection;
    }

    public String getSourceBook() {
        return sourceBook;
    }

    public void setSourceBook(String sourceBook) {
        this.sourceBook = sourceBook;
    }

    public String getEvidenceText() {
        return evidenceText;
    }

    public void setEvidenceText(String evidenceText) {
        this.evidenceText = evidenceText;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
