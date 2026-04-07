package com.yishou.liuyao.divination.service;

public class HexagramResult {

    private final String mainHexagramName;
    private final String changedHexagramName;
    private final String mainHexagramCode;
    private final String changedHexagramCode;
    private final String mainUpperTrigram;
    private final String mainLowerTrigram;
    private final String changedUpperTrigram;
    private final String changedLowerTrigram;

    public HexagramResult(String mainHexagramName,
                          String changedHexagramName,
                          String mainHexagramCode,
                          String changedHexagramCode,
                          String mainUpperTrigram,
                          String mainLowerTrigram,
                          String changedUpperTrigram,
                          String changedLowerTrigram) {
        this.mainHexagramName = mainHexagramName;
        this.changedHexagramName = changedHexagramName;
        this.mainHexagramCode = mainHexagramCode;
        this.changedHexagramCode = changedHexagramCode;
        this.mainUpperTrigram = mainUpperTrigram;
        this.mainLowerTrigram = mainLowerTrigram;
        this.changedUpperTrigram = changedUpperTrigram;
        this.changedLowerTrigram = changedLowerTrigram;
    }

    public String getMainHexagramName() {
        return mainHexagramName;
    }

    public String getChangedHexagramName() {
        return changedHexagramName;
    }

    public String getMainHexagramCode() {
        return mainHexagramCode;
    }

    public String getChangedHexagramCode() {
        return changedHexagramCode;
    }

    public String getMainUpperTrigram() {
        return mainUpperTrigram;
    }

    public String getMainLowerTrigram() {
        return mainLowerTrigram;
    }

    public String getChangedUpperTrigram() {
        return changedUpperTrigram;
    }

    public String getChangedLowerTrigram() {
        return changedLowerTrigram;
    }
}
