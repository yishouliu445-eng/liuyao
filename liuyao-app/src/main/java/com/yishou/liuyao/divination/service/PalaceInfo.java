package com.yishou.liuyao.divination.service;

public class PalaceInfo {

    private final String palace;
    private final String wuXing;

    public PalaceInfo(String palace, String wuXing) {
        this.palace = palace;
        this.wuXing = wuXing;
    }

    public String getPalace() {
        return palace;
    }

    public String getWuXing() {
        return wuXing;
    }
}
