package com.yishou.liuyao.divination.service;

public class ShiYingPosition {

    private final int shiIndex;
    private final int yingIndex;

    public ShiYingPosition(int shiIndex, int yingIndex) {
        this.shiIndex = shiIndex;
        this.yingIndex = yingIndex;
    }

    public int getShiIndex() {
        return shiIndex;
    }

    public int getYingIndex() {
        return yingIndex;
    }
}
