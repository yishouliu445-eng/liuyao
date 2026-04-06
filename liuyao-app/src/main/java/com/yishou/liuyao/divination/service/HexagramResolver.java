package com.yishou.liuyao.divination.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HexagramResolver {

    // 三位编码按“阳=1、阴=0”表示八卦。
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

    private static final Map<String, String> HEXAGRAM_NAMES = buildHexagramNames();

    public HexagramResult resolve(List<String> rawLines) {
        if (rawLines == null || rawLines.size() != 6) {
            return new HexagramResult("待定本卦", "待定变卦", "000000", "000000");
        }

        // 本卦按当前六爻直接编码，变卦则把老阴老阳翻转后再编码。
        String mainCode = toHexagramCode(rawLines, false);
        String changedCode = toHexagramCode(rawLines, true);
        return new HexagramResult(
                HEXAGRAM_NAMES.getOrDefault(mainCode, "未知卦"),
                HEXAGRAM_NAMES.getOrDefault(changedCode, "未知卦"),
                mainCode,
                changedCode
        );
    }

    private String toHexagramCode(List<String> rawLines, boolean changed) {
        StringBuilder lower = new StringBuilder();
        StringBuilder upper = new StringBuilder();
        for (int index = 0; index < rawLines.size(); index++) {
            char value = resolveLine(rawLines.get(index), changed);
            if (index < 3) {
                lower.append(value);
            } else {
                upper.append(value);
            }
        }
        // 结果统一按“上卦 + 下卦”输出六位编码。
        return upper + lower.toString();
    }

    private char resolveLine(String rawLine, boolean changed) {
        return switch (normalize(rawLine)) {
            case "少阳" -> '1';
            case "少阴" -> '0';
            case "老阳" -> changed ? '0' : '1';
            case "老阴" -> changed ? '1' : '0';
            default -> '0';
        };
    }

    private String normalize(String rawLine) {
        return rawLine == null ? "" : rawLine.trim();
    }

    private static Map<String, String> buildHexagramNames() {
        Map<String, String> map = new HashMap<>();
        putRow(map, "乾", List.of("乾为天", "天泽履", "天火同人", "天雷无妄", "天风姤", "天水讼", "天山遁", "天地否"));
        putRow(map, "兑", List.of("泽天夬", "兑为泽", "泽火革", "泽雷随", "泽风大过", "泽水困", "泽山咸", "泽地萃"));
        putRow(map, "离", List.of("火天大有", "火泽睽", "离为火", "火雷噬嗑", "火风鼎", "火水未济", "火山旅", "火地晋"));
        putRow(map, "震", List.of("雷天大壮", "雷泽归妹", "雷火丰", "震为雷", "雷风恒", "雷水解", "雷山小过", "雷地豫"));
        putRow(map, "巽", List.of("风天小畜", "风泽中孚", "风火家人", "风雷益", "巽为风", "风水涣", "风山渐", "风地观"));
        putRow(map, "坎", List.of("水天需", "水泽节", "水火既济", "水雷屯", "水风井", "坎为水", "水山蹇", "水地比"));
        putRow(map, "艮", List.of("山天大畜", "山泽损", "山火贲", "山雷颐", "山风蛊", "山水蒙", "艮为山", "山地剥"));
        putRow(map, "坤", List.of("地天泰", "地泽临", "地火明夷", "地雷复", "地风升", "地水师", "地山谦", "坤为地"));
        return map;
    }

    private static void putRow(Map<String, String> map, String upperName, List<String> names) {
        List<String> lowerOrder = List.of("乾", "兑", "离", "震", "巽", "坎", "艮", "坤");
        for (int index = 0; index < lowerOrder.size(); index++) {
            String code = trigramCode(upperName) + trigramCode(lowerOrder.get(index));
            map.put(code, names.get(index));
        }
    }

    private static String trigramCode(String trigramName) {
        return TRIGRAM_CODE_TO_NAME.entrySet().stream()
                .filter(entry -> entry.getValue().equals(trigramName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("000");
    }
}
