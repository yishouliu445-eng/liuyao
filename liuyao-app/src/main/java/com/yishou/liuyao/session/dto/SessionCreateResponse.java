package com.yishou.liuyao.session.dto;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.rule.dto.RuleHitDTO;

import java.util.List;
import java.util.UUID;

/** 创建 Session 的响应 */
public class SessionCreateResponse {

    private UUID sessionId;
    private UUID executionId;
    private String status;
    private ChartSnapshotDTO chartSnapshot;
    private List<RuleHitDTO> ruleHits;
    private AnalysisContextDTO analysisContext;
    private StructuredAnalysisResultDTO structuredResult;
    private AnalysisOutputDTO analysis;
    private List<String> smartPrompts;
    private int messageCount;

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }
    public UUID getExecutionId() { return executionId; }
    public void setExecutionId(UUID executionId) { this.executionId = executionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public ChartSnapshotDTO getChartSnapshot() { return chartSnapshot; }
    public void setChartSnapshot(ChartSnapshotDTO chartSnapshot) { this.chartSnapshot = chartSnapshot; }
    public List<RuleHitDTO> getRuleHits() { return ruleHits; }
    public void setRuleHits(List<RuleHitDTO> ruleHits) { this.ruleHits = ruleHits; }
    public AnalysisContextDTO getAnalysisContext() { return analysisContext; }
    public void setAnalysisContext(AnalysisContextDTO analysisContext) { this.analysisContext = analysisContext; }
    public StructuredAnalysisResultDTO getStructuredResult() { return structuredResult; }
    public void setStructuredResult(StructuredAnalysisResultDTO structuredResult) { this.structuredResult = structuredResult; }
    public AnalysisOutputDTO getAnalysis() { return analysis; }
    public void setAnalysis(AnalysisOutputDTO analysis) { this.analysis = analysis; }
    public List<String> getSmartPrompts() { return smartPrompts; }
    public void setSmartPrompts(List<String> smartPrompts) { this.smartPrompts = smartPrompts; }
    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }
}
