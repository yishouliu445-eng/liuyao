package com.yishou.liuyao.analysis.runtime;

import java.util.ArrayList;
import java.util.List;

public class AnalysisExecutionDegradation {

    private String level = "NONE";
    private List<String> reasons = new ArrayList<>();

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }
}
