package com.yishou.liuyao.rule.batch;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;

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
        if (branch == null || branch.isBlank()) {
            return null;
        }
        return switch (branch) {
            case "寅", "卯" -> "木";
            case "巳", "午" -> "火";
            case "辰", "戌", "丑", "未" -> "土";
            case "申", "酉" -> "金";
            case "亥", "子" -> "水";
            default -> null;
        };
    }

    public static boolean generates(String from, String to) {
        return ("木".equals(from) && "火".equals(to))
                || ("火".equals(from) && "土".equals(to))
                || ("土".equals(from) && "金".equals(to))
                || ("金".equals(from) && "水".equals(to))
                || ("水".equals(from) && "木".equals(to));
    }

    public static boolean controls(String from, String to) {
        return ("木".equals(from) && "土".equals(to))
                || ("土".equals(from) && "水".equals(to))
                || ("水".equals(from) && "火".equals(to))
                || ("火".equals(from) && "金".equals(to))
                || ("金".equals(from) && "木".equals(to));
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

    private static String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
