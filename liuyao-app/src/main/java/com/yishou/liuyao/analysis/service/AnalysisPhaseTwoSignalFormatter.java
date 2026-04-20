package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.divination.dto.ShenShaHitDTO;
import com.yishou.liuyao.rule.dto.RuleHitDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AnalysisPhaseTwoSignalFormatter {

    public String renderDerivedHexagrams(ChartSnapshotDTO chartSnapshot) {
        if (chartSnapshot == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        appendIfPresent(parts, "互卦" + chartSnapshot.getMutualHexagram());
        appendIfPresent(parts, "错卦" + chartSnapshot.getOppositeHexagram());
        appendIfPresent(parts, "综卦" + chartSnapshot.getReversedHexagram());
        return String.join("，", parts);
    }

    public String renderPhaseTwoSignals(AnalysisContextDTO context) {
        if (context == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        appendIfPresent(parts, renderFanFuYin(context));
        appendIfPresent(parts, renderTiming(context));
        appendIfPresent(parts, renderShenSha(context.getChartSnapshot()));
        return String.join(" ", parts);
    }

    private String renderFanFuYin(AnalysisContextDTO context) {
        RuleHitDTO fanFuYin = findRuleHit(context, "FAN_FU_YIN");
        if (fanFuYin == null || fanFuYin.getHitReason() == null || fanFuYin.getHitReason().isBlank()) {
            return "";
        }
        return "反伏吟提示：" + stripTerminalPunctuation(fanFuYin.getHitReason());
    }

    private String renderTiming(AnalysisContextDTO context) {
        RuleHitDTO timing = findRuleHit(context, "TIMING_SIGNAL");
        if (timing == null) {
            return "";
        }
        String timingHint = evidenceString(timing.getEvidence(), "timingHint");
        if (timingHint.isBlank()) {
            return "";
        }
        String lead = switch (evidenceString(timing.getEvidence(), "timingBucket")) {
            case "SHORT_TERM" -> "应期参考偏近期";
            case "MONTH" -> "应期参考可先看月内";
            case "DELAYED" -> "应期参考偏后";
            case "LATER" -> "应期参考宜稍后观察";
            default -> "应期参考";
        };
        return lead + "：" + stripTerminalPunctuation(timingHint);
    }

    private String renderShenSha(ChartSnapshotDTO chartSnapshot) {
        if (chartSnapshot == null || chartSnapshot.getShenShaHits() == null || chartSnapshot.getShenShaHits().isEmpty()) {
            return "";
        }
        List<String> names = chartSnapshot.getShenShaHits().stream()
                .map(ShenShaHitDTO::getName)
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .limit(4)
                .toList();
        String joinedNames = String.join("、", names);
        return joinedNames.isBlank() ? "" : "神煞辅助：盘面见" + joinedNames;
    }

    private RuleHitDTO findRuleHit(AnalysisContextDTO context, String ruleCode) {
        if (context.getRuleHits() == null || context.getRuleHits().isEmpty()) {
            return null;
        }
        return context.getRuleHits().stream()
                .filter(Objects::nonNull)
                .filter(hit -> ruleCode.equals(hit.getRuleCode()))
                .findFirst()
                .orElse(null);
    }

    private String evidenceString(Map<String, Object> evidence, String key) {
        if (evidence == null || !evidence.containsKey(key) || evidence.get(key) == null) {
            return "";
        }
        return String.valueOf(evidence.get(key)).trim();
    }

    private String stripTerminalPunctuation(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[。；，,\\s]+$", "").trim();
    }

    private void appendIfPresent(List<String> parts, String content) {
        if (content != null && !content.isBlank()) {
            parts.add(content);
        }
    }
}
