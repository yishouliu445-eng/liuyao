package com.yishou.liuyao.divination.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public class DivinationAnalyzeRequest {

    @NotBlank
    private String questionText;

    private String questionCategory;

    @NotNull
    private LocalDateTime divinationTime;

    private String divinationMethod;
    private List<String> rawLines;
    private List<Integer> movingLines;

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

    public String getDivinationMethod() {
        return divinationMethod;
    }

    public void setDivinationMethod(String divinationMethod) {
        this.divinationMethod = divinationMethod;
    }

    public List<String> getRawLines() {
        return rawLines;
    }

    public void setRawLines(List<String> rawLines) {
        this.rawLines = rawLines;
    }

    public List<Integer> getMovingLines() {
        return movingLines;
    }

    public void setMovingLines(List<Integer> movingLines) {
        this.movingLines = movingLines;
    }
}
