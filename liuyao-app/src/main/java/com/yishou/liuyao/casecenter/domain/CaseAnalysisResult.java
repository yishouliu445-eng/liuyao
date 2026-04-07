package com.yishou.liuyao.casecenter.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "case_analysis_result")
public class CaseAnalysisResult extends BaseEntity {

    @Column(name = "case_id")
    private Long caseId;

    private String provider;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "analysis_text")
    private String analysisText;

    private Integer score;

    @Column(name = "result_level")
    private String resultLevel;

    @Column(name = "structured_result_json")
    private String structuredResultJson;

    @Column(name = "analysis_context_json")
    private String analysisContextJson;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getAnalysisText() {
        return analysisText;
    }

    public void setAnalysisText(String analysisText) {
        this.analysisText = analysisText;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getResultLevel() {
        return resultLevel;
    }

    public void setResultLevel(String resultLevel) {
        this.resultLevel = resultLevel;
    }

    public String getStructuredResultJson() {
        return structuredResultJson;
    }

    public void setStructuredResultJson(String structuredResultJson) {
        this.structuredResultJson = structuredResultJson;
    }

    public String getAnalysisContextJson() {
        return analysisContextJson;
    }

    public void setAnalysisContextJson(String analysisContextJson) {
        this.analysisContextJson = analysisContextJson;
    }
}
