package com.yishou.liuyao.session.dto;

import java.util.List;

/** 创建 Session（起卦）请求 */
public class SessionCreateRequest {

    private String questionText;
    private String questionCategory;
    private String userSelectedDirection;
    private String finalDirection;
    private String divinationMethod;
    private String divinationTime;
    private List<String> rawLines;
    private List<Integer> movingLines;
    private Long userId;

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getQuestionCategory() { return questionCategory; }
    public void setQuestionCategory(String questionCategory) { this.questionCategory = questionCategory; }
    public String getUserSelectedDirection() { return userSelectedDirection; }
    public void setUserSelectedDirection(String userSelectedDirection) { this.userSelectedDirection = userSelectedDirection; }
    public String getFinalDirection() { return finalDirection; }
    public void setFinalDirection(String finalDirection) { this.finalDirection = finalDirection; }
    public String getDivinationMethod() { return divinationMethod; }
    public void setDivinationMethod(String divinationMethod) { this.divinationMethod = divinationMethod; }
    public String getDivinationTime() { return divinationTime; }
    public void setDivinationTime(String divinationTime) { this.divinationTime = divinationTime; }
    public List<String> getRawLines() { return rawLines; }
    public void setRawLines(List<String> rawLines) { this.rawLines = rawLines; }
    public List<Integer> getMovingLines() { return movingLines; }
    public void setMovingLines(List<Integer> movingLines) { this.movingLines = movingLines; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
