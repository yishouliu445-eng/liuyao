package com.yishou.liuyao.divination.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChartSnapshot {

    // 原始问题文本，后续规则选择与分析提示都会用到。
    private String question;
    // 问题分类，主要用于业务归档和场景兜底。
    private String questionCategory;
    private String divinationMethod;
    private LocalDateTime divinationTime;
    // 本卦名称，例如“乾为天”。
    private String mainHexagram;
    // 变卦名称，没有动爻时通常与本卦相同。
    private String changedHexagram;
    // 六位阴阳编码，便于规则层和持久化层稳定识别卦象。
    private String mainHexagramCode;
    private String changedHexagramCode;
    // 上下卦便于联调时核对本卦、变卦拆解过程。
    private String mainUpperTrigram;
    private String mainLowerTrigram;
    private String changedUpperTrigram;
    private String changedLowerTrigram;
    // 卦宫与宫五行会参与六亲判断和后续解释。
    private String palace;
    private String palaceWuXing;
    // 世应位置是很多判断的基础索引。
    private Integer shi;
    private Integer ying;
    // 显式保存当前用神，逐步替代 ext 中的松散存储。
    private String useGod;
    private String riChen;
    private String yueJian;
    private List<String> kongWang = new ArrayList<>();
    private List<LineInfo> lines = new ArrayList<>();
    private Map<String, Object> ext = new LinkedHashMap<>();
    private String snapshotVersion;
    private String calendarVersion;

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

    public List<String> getKongWang() {
        return kongWang;
    }

    public void setKongWang(List<String> kongWang) {
        this.kongWang = kongWang;
    }

    public List<LineInfo> getLines() {
        return lines;
    }

    public void setLines(List<LineInfo> lines) {
        this.lines = lines;
    }

    public Map<String, Object> getExt() {
        return ext;
    }

    public void setExt(Map<String, Object> ext) {
        this.ext = ext;
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
}
