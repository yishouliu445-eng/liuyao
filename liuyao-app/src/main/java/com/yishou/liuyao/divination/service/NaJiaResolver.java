package com.yishou.liuyao.divination.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NaJiaResolver {

    // 下三爻纳甲映射，顺序对应初爻、二爻、三爻。
    private static final Map<String, List<String>> LOWER_TRIGRAM_BRANCHES = Map.of(
            "乾", List.of("子", "寅", "辰"),
            "震", List.of("子", "寅", "辰"),
            "坎", List.of("寅", "辰", "午"),
            "艮", List.of("辰", "午", "申"),
            "巽", List.of("丑", "亥", "酉"),
            "离", List.of("卯", "丑", "亥"),
            "坤", List.of("未", "巳", "卯"),
            "兑", List.of("巳", "卯", "丑")
    );

    private static final Map<String, List<String>> UPPER_TRIGRAM_BRANCHES = Map.of(
            "乾", List.of("午", "申", "戌"),
            "震", List.of("午", "申", "戌"),
            "坎", List.of("申", "戌", "子"),
            "艮", List.of("戌", "子", "寅"),
            "巽", List.of("未", "巳", "卯"),
            "离", List.of("酉", "未", "巳"),
            "坤", List.of("丑", "亥", "酉"),
            "兑", List.of("亥", "酉", "未")
    );

    private static final Map<String, String> TRIGRAM_CODE_TO_NAME = Map.of(
            "111", "乾",
            "110", "兑",
            "101", "离",
            "100", "震",
            "011", "巽",
            "010", "坎",
            "001", "艮",
            "000", "坤"
    );

    public List<String> resolve(String mainHexagramName, String mainHexagramCode) {
        if (mainHexagramCode == null || mainHexagramCode.length() != 6) {
            return List.of("子", "丑", "寅", "卯", "辰", "巳");
        }
        // 六位卦码拆成上卦、下卦，再分别取三爻纳甲后拼起来。
        String upper = TRIGRAM_CODE_TO_NAME.getOrDefault(mainHexagramCode.substring(0, 3), "坤");
        String lower = TRIGRAM_CODE_TO_NAME.getOrDefault(mainHexagramCode.substring(3, 6), "坤");
        return java.util.stream.Stream.concat(
                LOWER_TRIGRAM_BRANCHES.getOrDefault(lower, List.of("子", "丑", "寅")).stream(),
                UPPER_TRIGRAM_BRANCHES.getOrDefault(upper, List.of("卯", "辰", "巳")).stream()
        ).toList();
    }
}
