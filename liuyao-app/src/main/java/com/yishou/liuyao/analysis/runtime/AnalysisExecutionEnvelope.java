package com.yishou.liuyao.analysis.runtime;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import com.yishou.liuyao.rule.RuleHit;

import java.util.List;
import java.util.UUID;

public class AnalysisExecutionEnvelope {

    private UUID executionId;
    private AnalysisExecutionMode mode;
    private ChartSnapshot chartSnapshot;
    private List<RuleHit> ruleHits;
    private AnalysisContextDTO analysisContext;
    private StructuredAnalysisResultDTO structuredResult;
    private AnalysisOutputDTO analysisOutput;
    private String legacyAnalysisText;
    private List<String> knowledgeSnippets;
    private List<EvidenceHit> evidenceHits;
    private AnalysisExecutionDegradation degradation;
    private AnalysisExecutionVersions versions;

    public UUID getExecutionId() {
        return executionId;
    }

    public void setExecutionId(UUID executionId) {
        this.executionId = executionId;
    }

    public AnalysisExecutionMode getMode() {
        return mode;
    }

    public void setMode(AnalysisExecutionMode mode) {
        this.mode = mode;
    }

    public ChartSnapshot getChartSnapshot() {
        return chartSnapshot;
    }

    public void setChartSnapshot(ChartSnapshot chartSnapshot) {
        this.chartSnapshot = chartSnapshot;
    }

    public List<RuleHit> getRuleHits() {
        return ruleHits;
    }

    public void setRuleHits(List<RuleHit> ruleHits) {
        this.ruleHits = ruleHits;
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

    public AnalysisOutputDTO getAnalysisOutput() {
        return analysisOutput;
    }

    public void setAnalysisOutput(AnalysisOutputDTO analysisOutput) {
        this.analysisOutput = analysisOutput;
    }

    public String getLegacyAnalysisText() {
        return legacyAnalysisText;
    }

    public void setLegacyAnalysisText(String legacyAnalysisText) {
        this.legacyAnalysisText = legacyAnalysisText;
    }

    public List<String> getKnowledgeSnippets() {
        return knowledgeSnippets;
    }

    public void setKnowledgeSnippets(List<String> knowledgeSnippets) {
        this.knowledgeSnippets = knowledgeSnippets;
    }

    public List<EvidenceHit> getEvidenceHits() {
        return evidenceHits;
    }

    public void setEvidenceHits(List<EvidenceHit> evidenceHits) {
        this.evidenceHits = evidenceHits;
    }

    public AnalysisExecutionDegradation getDegradation() {
        return degradation;
    }

    public void setDegradation(AnalysisExecutionDegradation degradation) {
        this.degradation = degradation;
    }

    public AnalysisExecutionVersions getVersions() {
        return versions;
    }

    public void setVersions(AnalysisExecutionVersions versions) {
        this.versions = versions;
    }
}
