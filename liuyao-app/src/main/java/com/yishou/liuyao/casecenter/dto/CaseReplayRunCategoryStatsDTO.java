package com.yishou.liuyao.casecenter.dto;

import java.time.LocalDateTime;

public class CaseReplayRunCategoryStatsDTO {

    private String questionCategory;
    private long runCount;
    private long recommendPersistRuns;
    private long observeOnlyRuns;
    private LocalDateTime latestReplayTime;

    public String getQuestionCategory() {
        return questionCategory;
    }

    public void setQuestionCategory(String questionCategory) {
        this.questionCategory = questionCategory;
    }

    public long getRunCount() {
        return runCount;
    }

    public void setRunCount(long runCount) {
        this.runCount = runCount;
    }

    public long getRecommendPersistRuns() {
        return recommendPersistRuns;
    }

    public void setRecommendPersistRuns(long recommendPersistRuns) {
        this.recommendPersistRuns = recommendPersistRuns;
    }

    public long getObserveOnlyRuns() {
        return observeOnlyRuns;
    }

    public void setObserveOnlyRuns(long observeOnlyRuns) {
        this.observeOnlyRuns = observeOnlyRuns;
    }

    public LocalDateTime getLatestReplayTime() {
        return latestReplayTime;
    }

    public void setLatestReplayTime(LocalDateTime latestReplayTime) {
        this.latestReplayTime = latestReplayTime;
    }
}
