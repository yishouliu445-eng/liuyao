package com.yishou.liuyao.divination.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChartSnapshotDTO {

    // 面向接口输出的轻量盘面，保留前端和分析层常用字段。
    private String question;
    private String questionCategory;
    private String divinationMethod;
    private LocalDateTime divinationTime;
    private String mainHexagram;
    private String changedHexagram;
    private String mainHexagramCode;
    private String changedHexagramCode;
    private String mainUpperTrigram;
    private String mainLowerTrigram;
    private String changedUpperTrigram;
    private String changedLowerTrigram;
    private String mutualHexagram;
    private String mutualHexagramCode;
    private String oppositeHexagram;
    private String oppositeHexagramCode;
    private String reversedHexagram;
    private String reversedHexagramCode;
    private String palace;
    private String palaceWuXing;
    private Integer shi;
    private Integer ying;
    private String useGod;
    private String riChen;
    private String yueJian;
    private String snapshotVersion;
    private String calendarVersion;
    private List<String> kongWang = new ArrayList<>();
    private List<ShenShaHitDTO> shenShaHits = new ArrayList<>();
    private List<LineInfoDTO> lines = new ArrayList<>();

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

    public String getDivinationMethod() {
        return divinationMethod;
    }

    public void setDivinationMethod(String divinationMethod) {
        this.divinationMethod = divinationMethod;
    }

    public LocalDateTime getDivinationTime() {
        return divinationTime;
    }

    public void setDivinationTime(LocalDateTime divinationTime) {
        this.divinationTime = divinationTime;
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

    public String getMainHexagramCode() {
        return mainHexagramCode;
    }

    public void setMainHexagramCode(String mainHexagramCode) {
        this.mainHexagramCode = mainHexagramCode;
    }

    public String getChangedHexagramCode() {
        return changedHexagramCode;
    }

    public void setChangedHexagramCode(String changedHexagramCode) {
        this.changedHexagramCode = changedHexagramCode;
    }

    public String getMainUpperTrigram() {
        return mainUpperTrigram;
    }

    public void setMainUpperTrigram(String mainUpperTrigram) {
        this.mainUpperTrigram = mainUpperTrigram;
    }

    public String getMainLowerTrigram() {
        return mainLowerTrigram;
    }

    public void setMainLowerTrigram(String mainLowerTrigram) {
        this.mainLowerTrigram = mainLowerTrigram;
    }

    public String getChangedUpperTrigram() {
        return changedUpperTrigram;
    }

    public void setChangedUpperTrigram(String changedUpperTrigram) {
        this.changedUpperTrigram = changedUpperTrigram;
    }

    public String getChangedLowerTrigram() {
        return changedLowerTrigram;
    }

    public void setChangedLowerTrigram(String changedLowerTrigram) {
        this.changedLowerTrigram = changedLowerTrigram;
    }

    public String getMutualHexagram() {
        return mutualHexagram;
    }

    public void setMutualHexagram(String mutualHexagram) {
        this.mutualHexagram = mutualHexagram;
    }

    public String getMutualHexagramCode() {
        return mutualHexagramCode;
    }

    public void setMutualHexagramCode(String mutualHexagramCode) {
        this.mutualHexagramCode = mutualHexagramCode;
    }

    public String getOppositeHexagram() {
        return oppositeHexagram;
    }

    public void setOppositeHexagram(String oppositeHexagram) {
        this.oppositeHexagram = oppositeHexagram;
    }

    public String getOppositeHexagramCode() {
        return oppositeHexagramCode;
    }

    public void setOppositeHexagramCode(String oppositeHexagramCode) {
        this.oppositeHexagramCode = oppositeHexagramCode;
    }

    public String getReversedHexagram() {
        return reversedHexagram;
    }

    public void setReversedHexagram(String reversedHexagram) {
        this.reversedHexagram = reversedHexagram;
    }

    public String getReversedHexagramCode() {
        return reversedHexagramCode;
    }

    public void setReversedHexagramCode(String reversedHexagramCode) {
        this.reversedHexagramCode = reversedHexagramCode;
    }

    public String getPalace() {
        return palace;
    }

    public void setPalace(String palace) {
        this.palace = palace;
    }

    public String getPalaceWuXing() {
        return palaceWuXing;
    }

    public void setPalaceWuXing(String palaceWuXing) {
        this.palaceWuXing = palaceWuXing;
    }

    public Integer getShi() {
        return shi;
    }

    public void setShi(Integer shi) {
        this.shi = shi;
    }

    public Integer getYing() {
        return ying;
    }

    public void setYing(Integer ying) {
        this.ying = ying;
    }

    public String getUseGod() {
        return useGod;
    }

    public void setUseGod(String useGod) {
        this.useGod = useGod;
    }

    public String getRiChen() {
        return riChen;
    }

    public void setRiChen(String riChen) {
        this.riChen = riChen;
    }

    public String getYueJian() {
        return yueJian;
    }

    public void setYueJian(String yueJian) {
        this.yueJian = yueJian;
    }

    public String getSnapshotVersion() {
        return snapshotVersion;
    }

    public void setSnapshotVersion(String snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public String getCalendarVersion() {
        return calendarVersion;
    }

    public void setCalendarVersion(String calendarVersion) {
        this.calendarVersion = calendarVersion;
    }

    public List<String> getKongWang() {
        return kongWang;
    }

    public void setKongWang(List<String> kongWang) {
        this.kongWang = kongWang;
    }

    public List<ShenShaHitDTO> getShenShaHits() {
        return shenShaHits;
    }

    public void setShenShaHits(List<ShenShaHitDTO> shenShaHits) {
        this.shenShaHits = shenShaHits;
    }

    public List<LineInfoDTO> getLines() {
        return lines;
    }

    public void setLines(List<LineInfoDTO> lines) {
        this.lines = lines;
    }
}
