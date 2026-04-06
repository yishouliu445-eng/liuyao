package com.yishou.liuyao.divination.service;

public class HexagramResult {

    private final String mainHexagramName;
    private final String changedHexagramName;
    private final String mainHexagramCode;
    private final String changedHexagramCode;

    public HexagramResult(String mainHexagramName,
                          String changedHexagramName,
                          String mainHexagramCode,
                          String changedHexagramCode) {
        this.mainHexagramName = mainHexagramName;
        this.changedHexagramName = changedHexagramName;
        this.mainHexagramCode = mainHexagramCode;
        this.changedHexagramCode = changedHexagramCode;
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
}
