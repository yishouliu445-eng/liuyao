package com.yishou.liuyao.divination.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LineInfo {

    private Integer index;
    private String yinYang;
    private Boolean moving;
    private String changeTo;
    private String liuQin;
    private String liuShen;
    private String branch;
    private String wuXing;
    private String changeBranch;
    private String changeWuXing;
    private String changeLiuQin;
    private String fuShenBranch;
    private String fuShenWuXing;
    private String fuShenLiuQin;
    private String flyShenBranch;
    private String flyShenWuXing;
    private String flyShenLiuQin;
    private Boolean shi;
    private Boolean ying;

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getYinYang() {
        return yinYang;
    }

    public void setYinYang(String yinYang) {
        this.yinYang = yinYang;
    }

    public Boolean getMoving() {
        return moving;
    }

    public void setMoving(Boolean moving) {
        this.moving = moving;
    }

    public String getChangeTo() {
        return changeTo;
    }

    public void setChangeTo(String changeTo) {
        this.changeTo = changeTo;
    }

    public String getLiuQin() {
        return liuQin;
    }

    public void setLiuQin(String liuQin) {
        this.liuQin = liuQin;
    }

    public String getLiuShen() {
        return liuShen;
    }

    public void setLiuShen(String liuShen) {
        this.liuShen = liuShen;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getWuXing() {
        return wuXing;
    }

    public void setWuXing(String wuXing) {
        this.wuXing = wuXing;
    }

    public String getChangeBranch() {
        return changeBranch;
    }

    public void setChangeBranch(String changeBranch) {
        this.changeBranch = changeBranch;
    }

    public String getChangeWuXing() {
        return changeWuXing;
    }

    public void setChangeWuXing(String changeWuXing) {
        this.changeWuXing = changeWuXing;
    }

    public String getChangeLiuQin() {
        return changeLiuQin;
    }

    public void setChangeLiuQin(String changeLiuQin) {
        this.changeLiuQin = changeLiuQin;
    }

    public String getFuShenBranch() {
        return fuShenBranch;
    }

    public void setFuShenBranch(String fuShenBranch) {
        this.fuShenBranch = fuShenBranch;
    }

    public String getFuShenWuXing() {
        return fuShenWuXing;
    }

    public void setFuShenWuXing(String fuShenWuXing) {
        this.fuShenWuXing = fuShenWuXing;
    }

    public String getFuShenLiuQin() {
        return fuShenLiuQin;
    }

    public void setFuShenLiuQin(String fuShenLiuQin) {
        this.fuShenLiuQin = fuShenLiuQin;
    }

    public String getFlyShenBranch() {
        return flyShenBranch;
    }

    public void setFlyShenBranch(String flyShenBranch) {
        this.flyShenBranch = flyShenBranch;
    }

    public String getFlyShenWuXing() {
        return flyShenWuXing;
    }

    public void setFlyShenWuXing(String flyShenWuXing) {
        this.flyShenWuXing = flyShenWuXing;
    }

    public String getFlyShenLiuQin() {
        return flyShenLiuQin;
    }

    public void setFlyShenLiuQin(String flyShenLiuQin) {
        this.flyShenLiuQin = flyShenLiuQin;
    }

    public Boolean getShi() {
        return shi;
    }

    public void setShi(Boolean shi) {
        this.shi = shi;
    }

    public Boolean getYing() {
        return ying;
    }

    public void setYing(Boolean ying) {
        this.ying = ying;
    }

    public Boolean getIsMoving() {
        return moving;
    }

    public Boolean getIsShi() {
        return shi;
    }

    public Boolean getIsYing() {
        return ying;
    }
}
