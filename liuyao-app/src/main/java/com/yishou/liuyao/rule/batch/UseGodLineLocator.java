package com.yishou.liuyao.rule.batch;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.service.WuXingSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UseGodLineLocator {

    private static final String[] BRANCHES = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};

    private UseGodLineLocator() {
    }

    public static String extractUseGod(ChartSnapshot chart) {
        // 新链路优先读显式字段，老数据仍兼容 ext。
        if (chart == null) {
            return null;
        }
        if (chart.getUseGod() != null && !chart.getUseGod().isBlank()) {
            return chart.getUseGod();
        }
        if (chart.getExt() == null) {
            return null;
        }
        Object value = chart.getExt().get("useGod");
        return value == null ? null : String.valueOf(value);
    }

    public static List<LineInfo> findUseGodLines(ChartSnapshot chart, String useGod) {
        if (chart == null || chart.getLines() == null || useGod == null || useGod.isBlank()) {
            return List.of();
        }
        return chart.getLines().stream()
                .filter(line -> useGod.equals(line.getLiuQin()))
                .toList();
    }

    public static String extractBranch(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        for (String branch : BRANCHES) {
            if (source.contains(branch)) {
                return branch;
            }
        }
        return null;
    }

    public static boolean isChong(String a, String b) {
        return ("子".equals(a) && "午".equals(b)) || ("午".equals(a) && "子".equals(b))
                || ("丑".equals(a) && "未".equals(b)) || ("未".equals(a) && "丑".equals(b))
                || ("寅".equals(a) && "申".equals(b)) || ("申".equals(a) && "寅".equals(b))
                || ("卯".equals(a) && "酉".equals(b)) || ("酉".equals(a) && "卯".equals(b))
                || ("辰".equals(a) && "戌".equals(b)) || ("戌".equals(a) && "辰".equals(b))
                || ("巳".equals(a) && "亥".equals(b)) || ("亥".equals(a) && "巳".equals(b));
    }

    public static String branchToWuXing(String branch) {
        return WuXingSupport.branchToWuXing(branch);
    }

    public static boolean generates(String from, String to) {
        return WuXingSupport.generates(from, to);
    }

    public static boolean controls(String from, String to) {
        return WuXingSupport.controls(from, to);
    }

    public static String relationOf(String from, String to) {
        return WuXingSupport.relationOf(from, to);
    }

    public static Map<String, Object> summarizeLine(LineInfo line) {
        Map<String, Object> summary = new LinkedHashMap<>();
        // 规则证据统一走这套摘要，避免各规则返回结构不一致。
        summary.put("lineIndex", line.getIndex());
        summary.put("liuQin", defaultValue(line.getLiuQin()));
        summary.put("branch", defaultValue(line.getBranch()));
        summary.put("wuXing", defaultValue(line.getWuXing()));
        summary.put("moving", Boolean.TRUE.equals(line.getIsMoving()));
        if (Boolean.TRUE.equals(line.getIsMoving())) {
            summary.put("changeTo", defaultValue(line.getChangeTo()));
            summary.put("changeBranch", defaultValue(line.getChangeBranch()));
            summary.put("changeWuXing", defaultValue(line.getChangeWuXing()));
            summary.put("changeLiuQin", defaultValue(line.getChangeLiuQin()));
        }
        return summary;
    }

    public static Map<String, Object> baseChartEvidence(ChartSnapshot chart, String useGod) {
        // 规则层先挂一层稳定的盘面上下文，后续各条规则只补自己特有的证据字段。
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("useGod", defaultValue(useGod));
        if (chart == null) {
            return evidence;
        }
        evidence.put("mainHexagram", defaultValue(chart.getMainHexagram()));
        evidence.put("changedHexagram", defaultValue(chart.getChangedHexagram()));
        evidence.put("palace", defaultValue(chart.getPalace()));
        evidence.put("palaceWuXing", defaultValue(chart.getPalaceWuXing()));
        evidence.put("mainUpperTrigram", defaultValue(chart.getMainUpperTrigram()));
        evidence.put("mainLowerTrigram", defaultValue(chart.getMainLowerTrigram()));
        evidence.put("changedUpperTrigram", defaultValue(chart.getChangedUpperTrigram()));
        evidence.put("changedLowerTrigram", defaultValue(chart.getChangedLowerTrigram()));
        return evidence;
    }

    public static void putTargets(Map<String, Object> evidence, List<Map<String, Object>> targets) {
        List<Map<String, Object>> safeTargets = targets == null ? List.of() : targets;
        evidence.put("targetCount", safeTargets.size());
        evidence.put("targetSummary", safeTargets);
        evidence.put("targets", safeTargets);
    }

    private static String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
