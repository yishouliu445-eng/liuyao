package com.yishou.liuyao.analysis.runtime;

import com.yishou.liuyao.analysis.validation.AnalysisValidationResult;
import org.springframework.stereotype.Component;

@Component
public class DegradationResolver {

    public AnalysisExecutionDegradation resolve(AnalysisValidationResult validationResult) {
        AnalysisExecutionDegradation degradation = new AnalysisExecutionDegradation();
        if (validationResult == null || validationResult.isValid()) {
            return degradation;
        }
        degradation.setReasons(validationResult.getIssues().stream()
                .map(issue -> issue == null ? null : issue.getCode())
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList());
        boolean blocked = validationResult.getIssues().stream()
                .filter(issue -> issue != null)
                .anyMatch(issue -> "BLOCK".equals(issue.getSeverity()) || "SAFETY_SENSITIVE_ABSOLUTE".equals(issue.getCode()));
        degradation.setLevel(blocked ? "BLOCK" : "WARN");
        return degradation;
    }
}
