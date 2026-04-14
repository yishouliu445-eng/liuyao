package com.yishou.liuyao.ops.audit.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "analysis_run_issue")
public class AnalysisRunIssue extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_run_id", insertable = false, updatable = false)
    private AnalysisRun analysisRun;

    @Column(name = "analysis_run_id", nullable = false)
    private Long analysisRunId;

    @Column(name = "issue_code", nullable = false)
    private String issueCode;

    @Column(name = "issue_message")
    private String issueMessage;

    @Column(name = "severity")
    private String severity;

    public Long getAnalysisRunId() {
        return analysisRunId;
    }

    public void setAnalysisRunId(Long analysisRunId) {
        this.analysisRunId = analysisRunId;
    }

    public AnalysisRun getAnalysisRun() {
        return analysisRun;
    }

    public void setAnalysisRun(AnalysisRun analysisRun) {
        this.analysisRun = analysisRun;
        this.analysisRunId = analysisRun == null ? null : analysisRun.getId();
    }

    public String getIssueCode() {
        return issueCode;
    }

    public void setIssueCode(String issueCode) {
        this.issueCode = issueCode;
    }

    public String getIssueMessage() {
        return issueMessage;
    }

    public void setIssueMessage(String issueMessage) {
        this.issueMessage = issueMessage;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
