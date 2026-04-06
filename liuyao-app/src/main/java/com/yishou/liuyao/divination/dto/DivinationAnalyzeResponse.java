package com.yishou.liuyao.divination.dto;

import com.yishou.liuyao.rule.dto.RuleHitDTO;

import java.util.List;

public class DivinationAnalyzeResponse {

    private ChartSnapshotDTO chartSnapshot;
    private List<RuleHitDTO> ruleHits;
    private String analysis;

    public DivinationAnalyzeResponse() {
    }

    public DivinationAnalyzeResponse(ChartSnapshotDTO chartSnapshot, List<RuleHitDTO> ruleHits, String analysis) {
        this.chartSnapshot = chartSnapshot;
        this.ruleHits = ruleHits;
        this.analysis = analysis;
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
}
