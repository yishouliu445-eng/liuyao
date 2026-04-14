package com.yishou.liuyao.analysis.validation;

public class AnalysisValidationIssue {

    private String code;
    private String message;
    private String severity;

    public AnalysisValidationIssue() {
    }

    public AnalysisValidationIssue(String code, String message) {
        this(code, message, "WARN");
    }

    public AnalysisValidationIssue(String code, String message, String severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
