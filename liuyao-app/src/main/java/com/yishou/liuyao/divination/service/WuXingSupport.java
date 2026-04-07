package com.yishou.liuyao.divination.service;

import java.util.Map;

public final class WuXingSupport {

    private static final Map<String, String> BRANCH_WU_XING = Map.ofEntries(
            Map.entry("子", "水"),
            Map.entry("亥", "水"),
            Map.entry("寅", "木"),
            Map.entry("卯", "木"),
            Map.entry("巳", "火"),
            Map.entry("午", "火"),
            Map.entry("申", "金"),
            Map.entry("酉", "金"),
            Map.entry("辰", "土"),
            Map.entry("戌", "土"),
            Map.entry("丑", "土"),
            Map.entry("未", "土")
    );

    private WuXingSupport() {
    }

    // 统一维护地支对应五行，避免排盘层和规则层各写一套。
    public static String branchToWuXing(String branch) {
        if (branch == null || branch.isBlank()) {
            return null;
        }
        return BRANCH_WU_XING.get(branch);
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

    public static String relationOf(String from, String to) {
        if (from == null || to == null) {
            return "";
        }
        if (from.equals(to)) {
            return "同五行";
        }
        if (generates(from, to)) {
            return "生";
        }
        if (generates(to, from)) {
            return "被生";
        }
        if (controls(from, to)) {
            return "克";
        }
        if (controls(to, from)) {
            return "被克";
        }
        return "";
    }
}
