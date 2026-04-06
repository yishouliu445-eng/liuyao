package com.yishou.liuyao.divination.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LiuShenResolver {

    // 六神固定轮转顺序，从日干对应的起点顺排六爻。
    private static final List<String> ORDER = List.of("青龙", "朱雀", "勾陈", "螣蛇", "白虎", "玄武");

    public String resolve(String riChen, int lineIndex) {
        String dayGan = extractDayGan(riChen);
        // 不同日干对应不同起神位置，这里先按常用简化表处理。
        int startIndex = switch (dayGan) {
            case "甲", "乙" -> 0;
            case "丙", "丁" -> 1;
            case "戊" -> 2;
            case "己" -> 3;
            case "庚", "辛" -> 4;
            case "壬", "癸" -> 5;
            default -> 0;
        };
        return ORDER.get((startIndex + lineIndex - 1) % ORDER.size());
    }

    private String extractDayGan(String riChen) {
        if (riChen == null || riChen.isBlank()) {
            return "";
        }
        return riChen.substring(0, 1);
    }
}
