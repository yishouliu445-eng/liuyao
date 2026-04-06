package com.yishou.liuyao.divination.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ShiYingResolver {

    // 每宫 8 卦的世爻顺序采用固定表驱动，避免运行时再推导。
    private static final int[] SHI_ORDER = {6, 1, 2, 3, 4, 5, 4, 3};
    private static final Map<String, ShiYingPosition> POSITION_MAP = buildPositionMap();
    private static final Map<String, String> PALACE_WU_XING_MAP = buildPalaceWuXingMap();

    public ShiYingPosition resolve(String hexagramName) {
        return POSITION_MAP.getOrDefault(hexagramName, new ShiYingPosition(3, 6));
    }

    public String resolvePalaceWuXing(String hexagramName) {
        return PALACE_WU_XING_MAP.getOrDefault(hexagramName, "土");
    }

    private static Map<String, ShiYingPosition> buildPositionMap() {
        Map<String, ShiYingPosition> map = new HashMap<>();
        registerPalace(map, List.of("乾为天", "天风姤", "天山遁", "天地否", "风地观", "山地剥", "火地晋", "火天大有"));
        registerPalace(map, List.of("兑为泽", "泽水困", "泽地萃", "泽山咸", "水山蹇", "地山谦", "雷山小过", "雷泽归妹"));
        registerPalace(map, List.of("离为火", "火山旅", "火风鼎", "火水未济", "山水蒙", "风水涣", "天水讼", "天火同人"));
        registerPalace(map, List.of("震为雷", "雷地豫", "雷水解", "雷风恒", "地风升", "水风井", "泽风大过", "泽雷随"));
        registerPalace(map, List.of("巽为风", "风天小畜", "风火家人", "风雷益", "天雷无妄", "火雷噬嗑", "山雷颐", "山风蛊"));
        registerPalace(map, List.of("坎为水", "水泽节", "水雷屯", "水火既济", "泽火革", "雷火丰", "地火明夷", "地水师"));
        registerPalace(map, List.of("艮为山", "山火贲", "山天大畜", "山泽损", "火泽睽", "天泽履", "风泽中孚", "风山渐"));
        registerPalace(map, List.of("坤为地", "地雷复", "地泽临", "地天泰", "雷天大壮", "泽天夬", "水天需", "水地比"));
        return map;
    }

    private static void registerPalace(Map<String, ShiYingPosition> map, List<String> hexagrams) {
        for (int index = 0; index < hexagrams.size(); index++) {
            int shiIndex = SHI_ORDER[index];
            // 应爻与世爻相隔三位，因此直接用 opposite 计算。
            map.put(hexagrams.get(index), new ShiYingPosition(shiIndex, opposite(shiIndex)));
        }
    }

    private static Map<String, String> buildPalaceWuXingMap() {
        Map<String, String> map = new HashMap<>();
        registerPalaceWuXing(map, List.of("乾为天", "天风姤", "天山遁", "天地否", "风地观", "山地剥", "火地晋", "火天大有"), "金");
        registerPalaceWuXing(map, List.of("兑为泽", "泽水困", "泽地萃", "泽山咸", "水山蹇", "地山谦", "雷山小过", "雷泽归妹"), "金");
        registerPalaceWuXing(map, List.of("离为火", "火山旅", "火风鼎", "火水未济", "山水蒙", "风水涣", "天水讼", "天火同人"), "火");
        registerPalaceWuXing(map, List.of("震为雷", "雷地豫", "雷水解", "雷风恒", "地风升", "水风井", "泽风大过", "泽雷随"), "木");
        registerPalaceWuXing(map, List.of("巽为风", "风天小畜", "风火家人", "风雷益", "天雷无妄", "火雷噬嗑", "山雷颐", "山风蛊"), "木");
        registerPalaceWuXing(map, List.of("坎为水", "水泽节", "水雷屯", "水火既济", "泽火革", "雷火丰", "地火明夷", "地水师"), "水");
        registerPalaceWuXing(map, List.of("艮为山", "山火贲", "山天大畜", "山泽损", "火泽睽", "天泽履", "风泽中孚", "风山渐"), "土");
        registerPalaceWuXing(map, List.of("坤为地", "地雷复", "地泽临", "地天泰", "雷天大壮", "泽天夬", "水天需", "水地比"), "土");
        return map;
    }

    private static void registerPalaceWuXing(Map<String, String> map, List<String> hexagrams, String wuXing) {
        for (String hexagram : hexagrams) {
            map.put(hexagram, wuXing);
        }
    }

    private static int opposite(int shiIndex) {
        return shiIndex > 3 ? shiIndex - 3 : shiIndex + 3;
    }
}
