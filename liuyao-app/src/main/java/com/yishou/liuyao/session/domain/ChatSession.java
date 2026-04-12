package com.yishou.liuyao.session.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 对话会话实体。
 *
 * <p>一个 Session 对应用户围绕同一卦象进行的一次完整对话。
 * 起卦后自动创建，用户可多轮追问，24小时无交互后自动关闭。</p>
 */
@Entity
@Table(name = "chat_session")
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "chart_snapshot_id")
    private Long chartSnapshotId;

    @Column(name = "original_question", nullable = false)
    private String originalQuestion;

    @Column(name = "question_category")
    private String questionCategory;

    /**
     * Session状态: ACTIVE / PAUSED / CLOSED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "message_count", nullable = false)
    private int messageCount = 0;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "metadata_json")
    private String metadataJson;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.lastActiveAt = now;
    }

    // ---- Factory Methods ----

    public static ChatSession create(Long userId, Long caseId, Long chartSnapshotId,
                                     String question, String category) {
        ChatSession session = new ChatSession();
        session.userId = userId;
        session.caseId = caseId;
        session.chartSnapshotId = chartSnapshotId;
        session.originalQuestion = question;
        session.questionCategory = category;
        return session;
    }

    // ---- Business Methods ----

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    public void close() {
        this.status = "CLOSED";
        this.closedAt = LocalDateTime.now();
    }

    public void refreshActivity() {
        this.lastActiveAt = LocalDateTime.now();
    }

    public void incrementMessage(int tokens) {
        this.messageCount++;
        this.totalTokens += tokens;
        this.lastActiveAt = LocalDateTime.now();
    }

    // ---- Getters & Setters ----

    public UUID getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }
    public Long getChartSnapshotId() { return chartSnapshotId; }
    public void setChartSnapshotId(Long chartSnapshotId) { this.chartSnapshotId = chartSnapshotId; }
    public String getOriginalQuestion() { return originalQuestion; }
    public void setOriginalQuestion(String originalQuestion) { this.originalQuestion = originalQuestion; }
    public String getQuestionCategory() { return questionCategory; }
    public void setQuestionCategory(String questionCategory) { this.questionCategory = questionCategory; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(LocalDateTime lastActiveAt) { this.lastActiveAt = lastActiveAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
