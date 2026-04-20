package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.domain.ShenShaHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ShenShaResolver {

    public List<ShenShaHit> resolve(String riChen, List<LineInfo> lines) {
        if (riChen == null || riChen.isBlank() || lines == null || lines.isEmpty()) {
            return List.of();
        }
        String dayBranch = extractBranch(riChen);
        String dayGan = extractDayGan(riChen);
        List<ShenShaHit> hits = new ArrayList<>();

        addBranchHit(hits, "TRAVEL_HORSE", "驿马", resolveTravelHorse(dayBranch), "riChen", riChen, lines);
        addBranchHit(hits, "PEACH_BLOSSOM", "桃花", resolvePeachBlossom(dayBranch), "riChen", riChen, lines);
        addBranchHit(hits, "HUA_GAI", "华盖", resolveHuaGai(dayBranch), "riChen", riChen, lines);
        addBranchHit(hits, "GENERAL_STAR", "将星", resolveGeneralStar(dayBranch), "riChen", riChen, lines);
        addBranchHit(hits, "JIE_SHA", "劫煞", resolveJieSha(dayBranch), "riChen", riChen, lines);
        addBranchHit(hits, "DISASTER_SHA", "灾煞", resolveDisasterSha(dayBranch), "riChen", riChen, lines);
        addBranchHit(hits, "WEN_CHANG", "文昌", resolveWenChang(dayGan), "riChen", riChen, lines);
        for (String branch : resolveNobleman(dayGan)) {
            addBranchHit(hits, "NOBLEMAN", "天乙贵人", branch, "riChen", riChen, lines);
        }
        return hits;
    }

    private void addBranchHit(List<ShenShaHit> hits,
                              String code,
                              String name,
                              String targetBranch,
                              String matchedBy,
                              String sourceValue,
                              List<LineInfo> lines) {
        if (targetBranch == null || targetBranch.isBlank()) {
            return;
        }
        List<Integer> lineIndexes = lines.stream()
                .filter(line -> targetBranch.equals(line.getBranch()))
                .map(LineInfo::getIndex)
                .toList();
        if (lineIndexes.isEmpty()) {
            return;
        }
        ShenShaHit hit = new ShenShaHit();
        hit.setCode(code);
        hit.setName(name);
        hit.setScope("line");
        hit.setBranch(targetBranch);
        hit.setMatchedBy(matchedBy);
        hit.setLineIndexes(lineIndexes);
        hit.setSummary(name + "落" + targetBranch + "，见于第" + renderLineIndexes(lineIndexes) + "爻。");
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("targetBranch", targetBranch);
        evidence.put("matchedBy", matchedBy);
        evidence.put("sourceValue", sourceValue);
        evidence.put("lineCount", lineIndexes.size());
        hit.setEvidence(evidence);
        hits.add(hit);
    }

    private String renderLineIndexes(List<Integer> lineIndexes) {
        return lineIndexes.stream().map(String::valueOf).collect(Collectors.joining("、"));
    }

    private String resolveTravelHorse(String dayBranch) {
        if (dayBranch == null) {
            return null;
        }
        if ("申".equals(dayBranch) || "子".equals(dayBranch) || "辰".equals(dayBranch)) {
            return "寅";
        }
        if ("寅".equals(dayBranch) || "午".equals(dayBranch) || "戌".equals(dayBranch)) {
            return "申";
        }
        if ("巳".equals(dayBranch) || "酉".equals(dayBranch) || "丑".equals(dayBranch)) {
            return "亥";
        }
        if ("亥".equals(dayBranch) || "卯".equals(dayBranch) || "未".equals(dayBranch)) {
            return "巳";
        }
        return null;
    }

    private String resolvePeachBlossom(String dayBranch) {
        if (dayBranch == null) {
            return null;
        }
        if ("申".equals(dayBranch) || "子".equals(dayBranch) || "辰".equals(dayBranch)) {
            return "酉";
        }
        if ("寅".equals(dayBranch) || "午".equals(dayBranch) || "戌".equals(dayBranch)) {
            return "卯";
        }
        if ("巳".equals(dayBranch) || "酉".equals(dayBranch) || "丑".equals(dayBranch)) {
            return "午";
        }
        if ("亥".equals(dayBranch) || "卯".equals(dayBranch) || "未".equals(dayBranch)) {
            return "子";
        }
        return null;
    }

    private String resolveHuaGai(String dayBranch) {
        if (dayBranch == null) {
            return null;
        }
        if ("申".equals(dayBranch) || "子".equals(dayBranch) || "辰".equals(dayBranch)) {
            return "辰";
        }
        if ("寅".equals(dayBranch) || "午".equals(dayBranch) || "戌".equals(dayBranch)) {
            return "戌";
        }
        if ("巳".equals(dayBranch) || "酉".equals(dayBranch) || "丑".equals(dayBranch)) {
            return "丑";
        }
        if ("亥".equals(dayBranch) || "卯".equals(dayBranch) || "未".equals(dayBranch)) {
            return "未";
        }
        return null;
    }

    private String resolveGeneralStar(String dayBranch) {
        if (dayBranch == null) {
            return null;
        }
        if ("申".equals(dayBranch) || "子".equals(dayBranch) || "辰".equals(dayBranch)) {
            return "子";
        }
        if ("寅".equals(dayBranch) || "午".equals(dayBranch) || "戌".equals(dayBranch)) {
            return "午";
        }
        if ("巳".equals(dayBranch) || "酉".equals(dayBranch) || "丑".equals(dayBranch)) {
            return "酉";
        }
        if ("亥".equals(dayBranch) || "卯".equals(dayBranch) || "未".equals(dayBranch)) {
            return "卯";
        }
        return null;
    }

    private String resolveJieSha(String dayBranch) {
        if (dayBranch == null) {
            return null;
        }
        if ("申".equals(dayBranch) || "子".equals(dayBranch) || "辰".equals(dayBranch)) {
            return "巳";
        }
        if ("寅".equals(dayBranch) || "午".equals(dayBranch) || "戌".equals(dayBranch)) {
            return "亥";
        }
        if ("巳".equals(dayBranch) || "酉".equals(dayBranch) || "丑".equals(dayBranch)) {
            return "寅";
        }
        if ("亥".equals(dayBranch) || "卯".equals(dayBranch) || "未".equals(dayBranch)) {
            return "申";
        }
        return null;
    }

    private String resolveDisasterSha(String dayBranch) {
        if (dayBranch == null) {
            return null;
        }
        if ("申".equals(dayBranch) || "子".equals(dayBranch) || "辰".equals(dayBranch)) {
            return "午";
        }
        if ("寅".equals(dayBranch) || "午".equals(dayBranch) || "戌".equals(dayBranch)) {
            return "子";
        }
        if ("巳".equals(dayBranch) || "酉".equals(dayBranch) || "丑".equals(dayBranch)) {
            return "卯";
        }
        if ("亥".equals(dayBranch) || "卯".equals(dayBranch) || "未".equals(dayBranch)) {
            return "酉";
        }
        return null;
    }

    private String resolveWenChang(String dayGan) {
        if (dayGan == null || dayGan.isBlank()) {
            return null;
        }
        return switch (dayGan) {
            case "甲" -> "巳";
            case "乙" -> "午";
            case "丙", "戊" -> "申";
            case "丁", "己" -> "酉";
            case "庚" -> "亥";
            case "辛" -> "子";
            case "壬" -> "寅";
            case "癸" -> "卯";
            default -> null;
        };
    }

    private List<String> resolveNobleman(String dayGan) {
        if (dayGan == null || dayGan.isBlank()) {
            return List.of();
        }
        return switch (dayGan) {
            case "甲", "戊", "庚" -> List.of("丑", "未");
            case "乙", "己" -> List.of("子", "申");
            case "丙", "丁" -> List.of("亥", "酉");
            case "辛" -> List.of("寅", "午");
            case "壬", "癸" -> List.of("卯", "巳");
            default -> List.of();
        };
    }

    private String extractDayGan(String riChen) {
        if (riChen == null || riChen.isBlank()) {
            return null;
        }
        return riChen.substring(0, 1);
    }

    private String extractBranch(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        for (String branch : List.of("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")) {
            if (source.contains(branch)) {
                return branch;
            }
        }
        return null;
    }
}
