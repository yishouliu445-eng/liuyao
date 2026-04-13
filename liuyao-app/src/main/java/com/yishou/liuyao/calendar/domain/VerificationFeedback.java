package com.yishou.liuyao.calendar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 应验反馈实体。
 *
 * <p>对应一次应验事件的用户反馈，记录准确度、实际结果和标签。</p>
 */
@Entity
@Table(name = "verification_feedback")
public class VerificationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private VerificationEvent event;

    @Column(name = "accuracy", nullable = false, length = 30)
    private String accuracy;

    @Column(name = "actual_outcome")
    private String actualOutcome;

    @Column(name = "tags_json")
    private String tagsJson;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }

    // ---- Factory Methods ----

    public static VerificationFeedback create(VerificationEvent event, String accuracy,
                                              String actualOutcome, String tagsJson) {
        VerificationFeedback feedback = new VerificationFeedback();
        feedback.event = event;
        feedback.accuracy = accuracy;
        feedback.actualOutcome = actualOutcome;
        feedback.tagsJson = tagsJson;
        if (event != null && event.getFeedback() != feedback) {
            event.setFeedback(feedback);
        }
        return feedback;
    }

    // ---- Getters & Setters ----

    public UUID getId() {
        return id;
    }

    public VerificationEvent getEvent() {
        return event;
    }

    public void setEvent(VerificationEvent event) {
        this.event = event;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public void setActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
