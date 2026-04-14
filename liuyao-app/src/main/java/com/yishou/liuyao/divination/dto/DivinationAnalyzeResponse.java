package com.yishou.liuyao.divination.dto;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.rule.dto.RuleHitDTO;

import java.util.List;
import java.util.UUID;

public class DivinationAnalyzeResponse {

    private UUID executionId;
    private ChartSnapshotDTO chartSnapshot;
    private List<RuleHitDTO> ruleHits;
    private String analysis;
    private AnalysisContextDTO analysisContext;
    private StructuredAnalysisResultDTO structuredResult;

    public DivinationAnalyzeResponse() {
    }

    public DivinationAnalyzeResponse(ChartSnapshotDTO chartSnapshot,
                                     List<RuleHitDTO> ruleHits,
                                     String analysis,
                                     AnalysisContextDTO analysisContext,
                                     StructuredAnalysisResultDTO structuredResult) {
        this.chartSnapshot = chartSnapshot;
        this.ruleHits = ruleHits;
        this.analysis = analysis;
        this.analysisContext = analysisContext;
        this.structuredResult = structuredResult;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public ChartSnapshotDTO getChartSnapshot() {
        return chartSnapshot;
    }

    public void setChartSnapshot(ChartSnapshotDTO chartSnapshot) {
        this.chartSnapshot = chartSnapshot;
    }

    public List<RuleHitDTO> getRuleHits() {
        return ruleHits;
    }

    public void setRuleHits(List<RuleHitDTO> ruleHits) {
        this.ruleHits = ruleHits;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public AnalysisContextDTO getAnalysisContext() {
        return analysisContext;
    }

    public void setAnalysisContext(AnalysisContextDTO analysisContext) {
        this.analysisContext = analysisContext;
    }

    public StructuredAnalysisResultDTO getStructuredResult() {
        return structuredResult;
    }

    public void setStructuredResult(StructuredAnalysisResultDTO structuredResult) {
        this.structuredResult = structuredResult;
    }
}
