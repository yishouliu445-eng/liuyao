package com.yishou.liuyao.divination.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LiuQinResolver {

    // 地支先映射到五行，再与卦宫五行比较得出六亲。
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

    public String resolve(String palaceWuXing, String branch) {
        String lineWuXing = BRANCH_WU_XING.get(branch);
        if (palaceWuXing == null || lineWuXing == null) {
            return "待定";
        }
        // 这里以“宫五行为我”来判断同我、生我、我生、我克、克我。
        if (palaceWuXing.equals(lineWuXing)) {
            return "兄弟";
        }
        if (generates(palaceWuXing, lineWuXing)) {
            return "子孙";
        }
        if (generates(lineWuXing, palaceWuXing)) {
            return "父母";
        }
        if (controls(palaceWuXing, lineWuXing)) {
            return "妻财";
        }
        if (controls(lineWuXing, palaceWuXing)) {
            return "官鬼";
        }
        return "待定";
    }

    private boolean generates(String from, String to) {
        return ("木".equals(from) && "火".equals(to))
                || ("火".equals(from) && "土".equals(to))
                || ("土".equals(from) && "金".equals(to))
                || ("金".equals(from) && "水".equals(to))
                || ("水".equals(from) && "木".equals(to));
    }

    private boolean controls(String from, String to) {
        return ("木".equals(from) && "土".equals(to))
                || ("土".equals(from) && "水".equals(to))
                || ("水".equals(from) && "火".equals(to))
                || ("火".equals(from) && "金".equals(to))
                || ("金".equals(from) && "木".equals(to));
    }
}
