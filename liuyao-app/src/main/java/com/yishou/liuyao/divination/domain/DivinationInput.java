package com.yishou.liuyao.divination.domain;

import java.time.LocalDateTime;
import java.util.List;

public class DivinationInput {

    private String question;
    private String questionCategory;
    private List<String> rawLines;
    private List<Integer> movingLines;
    private LocalDateTime divinationTime;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getQuestionCategory() {
        return questionCategory;
    }

    public void setQuestionCategory(String questionCategory) {
        this.questionCategory = questionCategory;
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

    public LocalDateTime getDivinationTime() {
        return divinationTime;
    }

    public void setDivinationTime(LocalDateTime divinationTime) {
        this.divinationTime = divinationTime;
    }
}
