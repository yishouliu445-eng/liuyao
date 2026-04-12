package com.yishou.liuyao.calendar.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 应验事件实体。
 *
 * <p>对应一次预测结果的应验跟踪记录，记录预测日期、精度、摘要和反馈状态。</p>
 */
@Entity
@Table(name = "verification_event")
public class VerificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
    private UUID sessionId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "predicted_date", nullable = false)
    private LocalDate predictedDate;

    @Column(name = "predicted_precision", nullable = false, length = 20)
    private String predictedPrecision = "MONTH";

    @Column(name = "prediction_summary", nullable = false)
    private String predictionSummary;

    @Column(name = "question_category", length = 100)
    private String questionCategory;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "event", fetch = FetchType.LAZY)
    private VerificationFeedback feedback;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---- Factory Methods ----

    public static VerificationEvent create(UUID sessionId, Long userId, LocalDate predictedDate,
                                            String predictedPrecision, String predictionSummary,
                                            String questionCategory) {
        VerificationEvent event = new VerificationEvent();
        event.sessionId = sessionId;
        event.userId = userId;
        event.predictedDate = predictedDate;
        event.predictedPrecision = predictedPrecision;
        event.predictionSummary = predictionSummary;
        event.questionCategory = questionCategory;
        return event;
    }

    // ---- Business Methods ----

    public boolean isPending() {
        return "PENDING".equals(this.status);
    }

    public boolean hasFeedback() {
        return this.feedback != null;
    }

    public void markReminderSent() {
        this.reminderSentAt = LocalDateTime.now();
    }

    public void markStatus(String status) {
        this.status = status;
    }

    public void attachFeedback(VerificationFeedback feedback) {
        this.feedback = feedback;
        if (feedback != null && feedback.getEvent() != this) {
            feedback.setEvent(this);
        }
    }

    // ---- Getters & Setters ----

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getPredictedDate() {
        return predictedDate;
    }

    public void setPredictedDate(LocalDate predictedDate) {
        this.predictedDate = predictedDate;
    }

    public String getPredictedPrecision() {
        return predictedPrecision;
    }

    public void setPredictedPrecision(String predictedPrecision) {
        this.predictedPrecision = predictedPrecision;
    }

    public String getPredictionSummary() {
        return predictionSummary;
    }

    public void setPredictionSummary(String predictionSummary) {
        this.predictionSummary = predictionSummary;
    }

    public String getQuestionCategory() {
        return questionCategory;
    }

    public void setQuestionCategory(String questionCategory) {
        this.questionCategory = questionCategory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }

    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public VerificationFeedback getFeedback() {
        return feedback;
    }

    public void setFeedback(VerificationFeedback feedback) {
        this.feedback = feedback;
    }
}
