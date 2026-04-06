package com.yishou.liuyao.casecenter.dto;

import java.time.LocalDateTime;

public class CaseSummaryDTO {

    private Long caseId;
    private String questionText;
    private String questionCategory;
    private LocalDateTime divinationTime;
    private String status;
    private String mainHexagram;
    private String changedHexagram;
    private String palace;
    private String useGod;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionCategory() {
        return questionCategory;
    }

    public void setQuestionCategory(String questionCategory) {
        this.questionCategory = questionCategory;
    }

    public LocalDateTime getDivinationTime() {
        return divinationTime;
    }

    public void setDivinationTime(LocalDateTime divinationTime) {
        this.divinationTime = divinationTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMainHexagram() {
        return mainHexagram;
    }

    public void setMainHexagram(String mainHexagram) {
        this.mainHexagram = mainHexagram;
    }

    public String getChangedHexagram() {
        return changedHexagram;
    }

    public void setChangedHexagram(String changedHexagram) {
        this.changedHexagram = changedHexagram;
    }

    public String getPalace() {
        return palace;
    }

    public void setPalace(String palace) {
        this.palace = palace;
    }

    public String getUseGod() {
        return useGod;
    }

    public void setUseGod(String useGod) {
        this.useGod = useGod;
    }
}
