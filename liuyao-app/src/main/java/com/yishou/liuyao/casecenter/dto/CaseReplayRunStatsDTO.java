package com.yishou.liuyao.casecenter.dto;

import java.util.ArrayList;
import java.util.List;

public class CaseReplayRunStatsDTO {

    private long totalRuns;
    private long recommendPersistRuns;
    private long observeOnlyRuns;
    private List<CaseReplayRunCategoryStatsDTO> categoryStats = new ArrayList<>();

    public long getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(long totalRuns) {
        this.totalRuns = totalRuns;
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

    public List<CaseReplayRunCategoryStatsDTO> getCategoryStats() {
        return categoryStats;
    }

    public void setCategoryStats(List<CaseReplayRunCategoryStatsDTO> categoryStats) {
        this.categoryStats = categoryStats;
    }
}
