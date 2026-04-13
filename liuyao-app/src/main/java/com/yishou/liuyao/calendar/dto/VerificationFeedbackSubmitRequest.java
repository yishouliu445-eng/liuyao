package com.yishou.liuyao.calendar.dto;

import java.util.List;

public class VerificationFeedbackSubmitRequest {

    private String accuracy;
    private String actualOutcome;
    private List<String> tags;

    public VerificationFeedbackSubmitRequest() {
    }

    public VerificationFeedbackSubmitRequest(String accuracy, String actualOutcome, List<String> tags) {
        this.accuracy = accuracy;
        this.actualOutcome = actualOutcome;
        this.tags = tags;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public void setActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
