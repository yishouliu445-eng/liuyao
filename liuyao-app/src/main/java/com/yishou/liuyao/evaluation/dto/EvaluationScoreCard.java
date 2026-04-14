package com.yishou.liuyao.evaluation.dto;

public class EvaluationScoreCard {

    private String scenarioId;
    private String datasetType;
    private String summary;
    private Boolean passed;
    private Integer hitCount;
    private Integer citationCount;
    private Integer matchedCitationCount;
    private Double selectedCitationRate;
    private Double citationMismatchRate;

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

    public Integer getCitationCount() {
        return citationCount;
    }

    public void setCitationCount(Integer citationCount) {
        this.citationCount = citationCount;
    }

    public Integer getMatchedCitationCount() {
        return matchedCitationCount;
    }

    public void setMatchedCitationCount(Integer matchedCitationCount) {
        this.matchedCitationCount = matchedCitationCount;
    }

    public Double getSelectedCitationRate() {
        return selectedCitationRate;
    }

    public void setSelectedCitationRate(Double selectedCitationRate) {
        this.selectedCitationRate = selectedCitationRate;
    }

    public Double getCitationMismatchRate() {
        return citationMismatchRate;
    }

    public void setCitationMismatchRate(Double citationMismatchRate) {
        this.citationMismatchRate = citationMismatchRate;
    }
}
