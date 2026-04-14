package com.yishou.liuyao.analysis.validation;

import java.util.ArrayList;
import java.util.List;

public class AnalysisValidationResult {

    private List<AnalysisValidationIssue> issues = new ArrayList<>();

    public List<AnalysisValidationIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<AnalysisValidationIssue> issues) {
        this.issues = issues;
    }

    public boolean isValid() {
        return issues == null || issues.isEmpty();
    }

    public boolean hasIssueCode(String code) {
        return issues != null && issues.stream().anyMatch(issue -> issue != null && code.equals(issue.getCode()));
    }
}
