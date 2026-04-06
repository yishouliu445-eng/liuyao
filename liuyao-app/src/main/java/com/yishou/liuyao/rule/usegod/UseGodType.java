package com.yishou.liuyao.rule.usegod;

public enum UseGodType {
    GUAN_GUI("官鬼"),
    QI_CAI("妻财"),
    ZI_SUN("子孙"),
    XIONG_DI("兄弟"),
    FU_MU("父母"),
    SHI("世爻"),
    YING("应爻");

    private final String displayName;

    UseGodType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
