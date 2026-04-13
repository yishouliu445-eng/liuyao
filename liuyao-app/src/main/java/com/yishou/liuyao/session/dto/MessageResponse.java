package com.yishou.liuyao.session.dto;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** 追问响应 */
public class MessageResponse {

    private UUID messageId;
    private UUID sessionId;
    private AnalysisOutputDTO analysis;
    private List<String> smartPrompts;
    private LocalDateTime createdAt;
    private int sessionMessageCount;

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public AnalysisOutputDTO getAnalysis() { return analysis; }
    public void setAnalysis(AnalysisOutputDTO analysis) { this.analysis = analysis; }
    public List<String> getSmartPrompts() { return smartPrompts; }
    public void setSmartPrompts(List<String> smartPrompts) { this.smartPrompts = smartPrompts; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getSessionMessageCount() { return sessionMessageCount; }
    public void setSessionMessageCount(int sessionMessageCount) { this.sessionMessageCount = sessionMessageCount; }
}
