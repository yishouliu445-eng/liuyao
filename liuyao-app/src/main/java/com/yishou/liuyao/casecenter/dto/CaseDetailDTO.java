package com.yishou.liuyao.casecenter.dto;

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
}
