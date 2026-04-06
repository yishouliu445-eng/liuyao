package com.yishou.liuyao.divination.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PalaceResolver {

    // 卦宫与宫五行采用固定表驱动，方便排盘和规则层统一复用。
    private static final Map<String, PalaceInfo> PALACE_MAP = buildPalaceMap();

    public PalaceInfo resolve(String hexagramName) {
        return PALACE_MAP.getOrDefault(hexagramName, new PalaceInfo("未知", "未知"));
    }

    private static Map<String, PalaceInfo> buildPalaceMap() {
        Map<String, PalaceInfo> map = new LinkedHashMap<>();
        register(map, "乾", "金", List.of("乾为天", "天风姤", "天山遁", "天地否", "风地观", "山地剥", "火地晋", "火天大有"));
        register(map, "兑", "金", List.of("兑为泽", "泽水困", "泽地萃", "泽山咸", "水山蹇", "地山谦", "雷山小过", "雷泽归妹"));
        register(map, "离", "火", List.of("离为火", "火山旅", "火风鼎", "火水未济", "山水蒙", "风水涣", "天水讼", "天火同人"));
        register(map, "震", "木", List.of("震为雷", "雷地豫", "雷水解", "雷风恒", "地风升", "水风井", "泽风大过", "泽雷随"));
        register(map, "巽", "木", List.of("巽为风", "风天小畜", "风火家人", "风雷益", "天雷无妄", "火雷噬嗑", "山雷颐", "山风蛊"));
        register(map, "坎", "水", List.of("坎为水", "水泽节", "水雷屯", "水火既济", "泽火革", "雷火丰", "地火明夷", "地水师"));
        register(map, "艮", "土", List.of("艮为山", "山火贲", "山天大畜", "山泽损", "火泽睽", "天泽履", "风泽中孚", "风山渐"));
        register(map, "坤", "土", List.of("坤为地", "地雷复", "地泽临", "地天泰", "雷天大壮", "泽天夬", "水天需", "水地比"));
        return map;
    }

    private static void register(Map<String, PalaceInfo> map, String palace, String wuXing, List<String> hexagrams) {
        PalaceInfo info = new PalaceInfo(palace, wuXing);
        for (String hexagram : hexagrams) {
            map.put(hexagram, info);
        }
    }
}
