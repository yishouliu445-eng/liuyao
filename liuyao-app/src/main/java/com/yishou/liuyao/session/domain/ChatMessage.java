package com.yishou.liuyao.session.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 会话消息实体。
 *
 * <p>每条用户发言或AI回复均为一条消息记录。
 * role: USER / ASSISTANT / SYSTEM</p>
 */
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "session_id", nullable = false, columnDefinition = "uuid")
    private UUID sessionId;

    /**
     * 消息角色: USER / ASSISTANT / SYSTEM
     */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    /**
     * 消息的文本内容（USER消息为原文，ASSISTANT消息为结论文本）
     */
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * ASSISTANT消息的结构化JSON输出（对应 AnalysisOutputDTO 序列化结果）
     */
    @Column(name = "structured_json")
    private String structuredJson;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "processing_ms")
    private Integer processingMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---- Factory Methods ----

    public static ChatMessage userMessage(UUID sessionId, String content) {
        ChatMessage msg = new ChatMessage();
        msg.sessionId = sessionId;
        msg.role = "USER";
        msg.content = content;
        return msg;
    }

    public static ChatMessage assistantMessage(UUID sessionId, String content,
                                               String structuredJson, String modelUsed,
                                               int tokenCount, int processingMs) {
        ChatMessage msg = new ChatMessage();
        msg.sessionId = sessionId;
        msg.role = "ASSISTANT";
        msg.content = content;
        msg.structuredJson = structuredJson;
        msg.modelUsed = modelUsed;
        msg.tokenCount = tokenCount;
        msg.processingMs = processingMs;
        return msg;
    }

    // ---- Getters ----

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStructuredJson() { return structuredJson; }
    public void setStructuredJson(String structuredJson) { this.structuredJson = structuredJson; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }
    public Integer getProcessingMs() { return processingMs; }
    public void setProcessingMs(Integer processingMs) { this.processingMs = processingMs; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
