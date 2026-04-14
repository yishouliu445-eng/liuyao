package com.yishou.liuyao.evaluation.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "evaluation_run")
public class EvaluationRun extends BaseEntity {

    @Column(name = "dataset_type", nullable = false)
    private String datasetType;

    @Column(name = "scenario_type", nullable = false)
    private String scenarioType;

    @Column(name = "scenario_id", nullable = false)
    private String scenarioId;

    @Column(name = "question_category")
    private String questionCategory;

    @Column(name = "passed", nullable = false)
    private Boolean passed;

    @Column(name = "summary")
    private String summary;

    @Column(name = "score_card_json")
    private String scoreCardJson;

    @Column(name = "selected_citation_rate")
    private Double selectedCitationRate;

    @Column(name = "citation_mismatch_rate")
    private Double citationMismatchRate;

    public String getDatasetType() {
        return datasetType;
    }

    public void setDatasetType(String datasetType) {
        this.datasetType = datasetType;
    }

    public String getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(String scenarioType) {
        this.scenarioType = scenarioType;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getQuestionCategory() {
        return questionCategory;
    }

    public void setQuestionCategory(String questionCategory) {
        this.questionCategory = questionCategory;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getScoreCardJson() {
        return scoreCardJson;
    }

    public void setScoreCardJson(String scoreCardJson) {
        this.scoreCardJson = scoreCardJson;
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
