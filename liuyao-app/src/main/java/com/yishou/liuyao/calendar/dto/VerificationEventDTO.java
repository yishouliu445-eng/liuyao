package com.yishou.liuyao.calendar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class VerificationEventDTO {

    private UUID eventId;
    private UUID sessionId;
    private Long userId;
    private LocalDate predictedDate;
    private String predictedPrecision;
    private String predictionSummary;
    private String questionCategory;
    private String status;
    private LocalDateTime reminderSentAt;
    private LocalDateTime createdAt;
    private boolean feedbackSubmitted;
    private String feedbackAccuracy;

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean getFeedbackSubmitted() {
        return feedbackSubmitted;
    }

    public void setFeedbackSubmitted(boolean feedbackSubmitted) {
        this.feedbackSubmitted = feedbackSubmitted;
    }

    public String getFeedbackAccuracy() {
        return feedbackAccuracy;
    }

    public void setFeedbackAccuracy(String feedbackAccuracy) {
        this.feedbackAccuracy = feedbackAccuracy;
    }
}
