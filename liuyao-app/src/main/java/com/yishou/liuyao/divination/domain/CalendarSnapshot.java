package com.yishou.liuyao.divination.domain;

import java.util.List;

public class CalendarSnapshot {

    private String riChen;
    private String yueJian;
    private List<String> kongWang;

    public CalendarSnapshot(String riChen, String yueJian, List<String> kongWang) {
        this.riChen = riChen;
        this.yueJian = yueJian;
        this.kongWang = kongWang;
    }

    public String getRiChen() {
        return riChen;
    }

    public String getYueJian() {
        return yueJian;
    }

    public List<String> getKongWang() {
        return kongWang;
    }
}
