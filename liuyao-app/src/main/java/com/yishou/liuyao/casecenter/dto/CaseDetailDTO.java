package com.yishou.liuyao.casecenter.dto;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.rule.dto.RuleHitDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CaseDetailDTO {

    private Long caseId;
    private String questionText;
    private String questionCategory;
    private LocalDateTime divinationTime;
    private String status;
    private ChartSnapshotDTO chartSnapshot;
    private List<RuleHitDTO> ruleHits = new ArrayList<>();
    private String analysis;
    private AnalysisContextDTO analysisContext;
    private StructuredAnalysisResultDTO structuredResult;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionCategory() {
        return questionCategory;
    }

    public void setQuestionCategory(String questionCategory) {
        this.questionCategory = questionCategory;
    }

    public LocalDateTime getDivinationTime() {
        return divinationTime;
    }

    public void setDivinationTime(LocalDateTime divinationTime) {
        this.divinationTime = divinationTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
