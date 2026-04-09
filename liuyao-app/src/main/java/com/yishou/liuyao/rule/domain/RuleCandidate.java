package com.yishou.liuyao.rule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "rule_candidate")
public class RuleCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_chunk_id", nullable = false)
    private Long bookChunkId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "rule_title", nullable = false)
    private String ruleTitle;

    @Column(name = "category")
    private String category;

    @Column(name = "condition_desc", columnDefinition = "TEXT")
    private String conditionDesc;

    @Column(name = "effect_direction", length = 50)
    private String effectDirection;

    @Column(name = "source_book")
    private String sourceBook;

    @Column(name = "evidence_text", columnDefinition = "TEXT")
    private String evidenceText;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, REJECTED, PROMOTED

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public RuleCandidate() {
    }

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
