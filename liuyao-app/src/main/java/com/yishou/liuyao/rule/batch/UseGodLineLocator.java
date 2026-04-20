package com.yishou.liuyao.rule.batch;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.service.WuXingSupport;
import com.yishou.liuyao.rule.usegod.UseGodType;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static List<LineInfo> findHiddenUseGodLines(ChartSnapshot chart, String useGod) {
        if (chart == null || chart.getLines() == null || useGod == null || useGod.isBlank()) {
            return List.of();
        }
        return chart.getLines().stream()
                .filter(line -> useGod.equals(line.getFuShenLiuQin()))
                .toList();
    }

    public static List<LineInfo> findCandidates(ChartSnapshot chart, UseGodType useGodType) {
        if (chart == null || chart.getLines() == null || useGodType == null) {
            return List.of();
        }
        return switch (useGodType) {
            case SHI -> chart.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getIsShi())).toList();
            case YING -> chart.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getIsYing())).toList();
            default -> findUseGodLines(chart, useGodType.getDisplayName());
        };
    }

    public static SelectionResult locate(ChartSnapshot chart, UseGodType useGodType) {
        List<LineInfo> candidates = findCandidates(chart, useGodType);
        if (useGodType == null) {
            return SelectionResult.empty();
        }
        if (useGodType == UseGodType.SHI) {
            return singleMatch(candidates, "SELF_ONLY", "直接取世爻为用神。");
        }
        if (useGodType == UseGodType.YING && candidates.size() == 1) {
            return singleMatch(candidates, "SINGLE_MATCH", "直接取应爻为用神。");
        }
        if (candidates.size() == 1) {
            return singleMatch(candidates, "SINGLE_MATCH", "仅有一个候选爻，直接采用。");
        }
        if (!candidates.isEmpty()) {
            return scoreSelection(chart, candidates);
        }
        LineInfo shiLine = chart == null || chart.getLines() == null
                ? null
                : chart.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getIsShi())).findFirst().orElse(null);
        if (shiLine == null) {
            return SelectionResult.empty();
        }
        return new SelectionResult(
                shiLine.getIndex(),
                List.of(shiLine.getIndex()),
                "FALLBACK",
                "未找到匹配候选，回退取世爻。",
                true,
                "USE_SHI_LINE",
                List.of(Map.of("lineIndex", shiLine.getIndex(), "totalScore", 0, "reason", "fallback to shi")),
                Map.of("fallbackTarget", "SHI")
        );
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
        if (line.getFuShenLiuQin() != null && !line.getFuShenLiuQin().isBlank()) {
            summary.put("fuShenLiuQin", defaultValue(line.getFuShenLiuQin()));
            summary.put("fuShenBranch", defaultValue(line.getFuShenBranch()));
            summary.put("fuShenWuXing", defaultValue(line.getFuShenWuXing()));
            summary.put("flyShenLiuQin", defaultValue(line.getFlyShenLiuQin()));
            summary.put("flyShenBranch", defaultValue(line.getFlyShenBranch()));
            summary.put("flyShenWuXing", defaultValue(line.getFlyShenWuXing()));
        }
        if (Boolean.TRUE.equals(line.getIsMoving())) {
            summary.put("changeTo", defaultValue(line.getChangeTo()));
            summary.put("changeBranch", defaultValue(line.getChangeBranch()));
            summary.put("changeWuXing", defaultValue(line.getChangeWuXing()));
            summary.put("changeLiuQin", defaultValue(line.getChangeLiuQin()));
            summary.put("transformTrend", defaultValue(resolveTransformTrend(line.getBranch(), line.getChangeBranch())));
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

    public static String resolveTransformTrend(String sourceBranch, String changeBranch) {
        if (sourceBranch == null || changeBranch == null || sourceBranch.isBlank() || changeBranch.isBlank()) {
            return "";
        }
        if (isAdvanceTransform(sourceBranch, changeBranch)) {
            return "化进";
        }
        if (isRetreatTransform(sourceBranch, changeBranch)) {
            return "化退";
        }
        return "";
    }

    public static boolean isAdvanceTransform(String sourceBranch, String changeBranch) {
        return matchesTransform(sourceBranch, changeBranch, Map.of(
                "寅", "卯",
                "巳", "午",
                "申", "酉",
                "亥", "子",
                "辰", "未",
                "未", "戌",
                "戌", "丑",
                "丑", "辰"
        ));
    }

    public static boolean isRetreatTransform(String sourceBranch, String changeBranch) {
        return matchesTransform(sourceBranch, changeBranch, Map.of(
                "卯", "寅",
                "午", "巳",
                "酉", "申",
                "子", "亥",
                "未", "辰",
                "戌", "未",
                "丑", "戌",
                "辰", "丑"
        ));
    }

    private static String defaultValue(String value) {
        return value == null ? "" : value;
    }

    private static SelectionResult singleMatch(List<LineInfo> candidates, String strategy, String reason) {
        if (candidates == null || candidates.isEmpty()) {
            return SelectionResult.empty();
        }
        LineInfo line = candidates.get(0);
        return new SelectionResult(
                line.getIndex(),
                candidates.stream().map(LineInfo::getIndex).toList(),
                strategy,
                reason,
                false,
                null,
                List.of(Map.of("lineIndex", line.getIndex(), "totalScore", 0, "reason", "single candidate", "stateFlags", List.of("唯一候选"))),
                Map.of("candidateCount", candidates.size())
        );
    }

    private static SelectionResult scoreSelection(ChartSnapshot chart, List<LineInfo> candidates) {
        List<Map<String, Object>> scoreDetails = candidates.stream()
                .map(line -> scoreLine(chart, line))
                .sorted(Comparator.comparingInt(item -> -((Integer) item.get("totalScore"))))
                .toList();
        Integer selectedLineIndex = (Integer) scoreDetails.get(0).get("lineIndex");
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("candidateCount", candidates.size());
        evidence.put("shiIndex", chart == null ? null : chart.getShi());
        return new SelectionResult(
                selectedLineIndex,
                candidates.stream().map(LineInfo::getIndex).sorted().toList(),
                "SCORING",
                "按发动、距离世爻和空亡等因素排序后取最高分候选。",
                false,
                null,
                scoreDetails,
                evidence
        );
    }

    private static Map<String, Object> scoreLine(ChartSnapshot chart, LineInfo line) {
        int totalScore = 5;
        Set<String> stateFlags = new LinkedHashSet<>();
        if (Boolean.TRUE.equals(line.getIsMoving())) {
            totalScore += 2;
            stateFlags.add("动");
        }
        if (Boolean.TRUE.equals(line.getIsShi())) {
            totalScore += 2;
            stateFlags.add("世");
        }
        if (Boolean.TRUE.equals(line.getIsYing())) {
            totalScore += 1;
            stateFlags.add("应");
        }
        if (chart != null && chart.getShi() != null && line.getIndex() != null) {
            int distance = Math.abs(line.getIndex() - chart.getShi());
            if (distance == 0) {
                totalScore += 2;
                stateFlags.add("贴世");
            } else if (distance == 1) {
                totalScore += 1;
                stateFlags.add("近世");
            }
        }
        if (chart != null && chart.getKongWang() != null && line.getBranch() != null) {
            if (chart.getKongWang().contains(line.getBranch())) {
                totalScore -= 2;
                stateFlags.add("空");
            } else {
                totalScore += 1;
                stateFlags.add("不空");
            }
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("lineIndex", line.getIndex());
        detail.put("totalScore", totalScore);
        detail.put("moving", Boolean.TRUE.equals(line.getIsMoving()));
        detail.put("isShi", Boolean.TRUE.equals(line.getIsShi()));
        detail.put("isYing", Boolean.TRUE.equals(line.getIsYing()));
        detail.put("stateFlags", List.copyOf(stateFlags));
        return detail;
    }

    private static boolean matchesTransform(String sourceBranch, String changeBranch, Map<String, String> mapping) {
        return sourceBranch != null
                && changeBranch != null
                && changeBranch.equals(mapping.get(sourceBranch));
    }

    public record SelectionResult(
            Integer selectedLineIndex,
            List<Integer> candidateLineIndexes,
            String selectionStrategy,
            String selectionReason,
            Boolean fallbackApplied,
            String fallbackStrategy,
            List<Map<String, Object>> scoreDetails,
            Map<String, Object> evidence) {
        public static SelectionResult empty() {
            return new SelectionResult(null, List.of(), null, null, false, null, List.of(), Map.of());
        }
    }
}
