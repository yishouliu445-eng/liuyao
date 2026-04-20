package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.batch.UseGodLineLocator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(34)
public class FanFuYinRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("FAN_FU_YIN");
        hit.setRuleName("反吟伏吟");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中没有爻信息，无法判断反吟伏吟。");
            hit.setEvidence(Map.of());
            return hit;
        }

        List<LineInfo> movingLines = chart.getLines().stream()
                .filter(line -> Boolean.TRUE.equals(line.getIsMoving()))
                .toList();
        if (movingLines.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前无动爻，无法判断反吟伏吟。");
            hit.setEvidence(Map.of());
            return hit;
        }

        List<Map<String, Object>> fuYinLines = new ArrayList<>();
        List<Map<String, Object>> fanYinLines = new ArrayList<>();
        for (LineInfo line : movingLines) {
            if (line.getBranch() == null || line.getBranch().isBlank()
                    || line.getChangeBranch() == null || line.getChangeBranch().isBlank()) {
                continue;
            }
            if (line.getBranch().equals(line.getChangeBranch())) {
                fuYinLines.add(UseGodLineLocator.summarizeLine(line));
            }
            if (UseGodLineLocator.isChong(line.getBranch(), line.getChangeBranch())) {
                fanYinLines.add(UseGodLineLocator.summarizeLine(line));
            }
        }

        boolean hasFuYin = !fuYinLines.isEmpty();
        boolean hasFanYin = !fanYinLines.isEmpty();
        boolean chartFuYin = hasFuYin && fuYinLines.size() == movingLines.size();
        boolean chartFanYin = hasFanYin && fanYinLines.size() == movingLines.size();

        if (!hasFuYin && !hasFanYin) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前动爻未形成反吟或伏吟。");
            hit.setEvidence(Map.of(
                    "movingLineCount", movingLines.size(),
                    "hasFuYin", false,
                    "hasFanYin", false
            ));
            return hit;
        }

        Map<String, Object> evidence = new LinkedHashMap<>(UseGodLineLocator.baseChartEvidence(chart, UseGodLineLocator.extractUseGod(chart)));
        evidence.put("movingLineCount", movingLines.size());
        evidence.put("hasFuYin", hasFuYin);
        evidence.put("chartFuYin", chartFuYin);
        evidence.put("hasFanYin", hasFanYin);
        evidence.put("chartFanYin", chartFanYin);
        evidence.put("fuYinLines", fuYinLines);
        evidence.put("fanYinLines", fanYinLines);
        evidence.put("mutualHexagram", chart.getMutualHexagram() == null ? "" : chart.getMutualHexagram());
        evidence.put("oppositeHexagram", chart.getOppositeHexagram() == null ? "" : chart.getOppositeHexagram());
        evidence.put("reversedHexagram", chart.getReversedHexagram() == null ? "" : chart.getReversedHexagram());

        hit.setHit(true);
        hit.setImpactLevel(chartFanYin || chartFuYin ? "HIGH" : "MEDIUM");
        hit.setHitReason(resolveReason(chartFuYin, chartFanYin, hasFuYin, hasFanYin));
        hit.setEvidence(evidence);
        return hit;
    }

    private String resolveReason(boolean chartFuYin, boolean chartFanYin, boolean hasFuYin, boolean hasFanYin) {
        if (chartFuYin && chartFanYin) {
            return "动爻整体同时出现伏吟与反吟特征，局势既停滞又反复。";
        }
        if (chartFuYin) {
            return "动爻整体伏吟，事情更容易原地盘桓。";
        }
        if (chartFanYin) {
            return "动爻整体反吟，事情更容易来回折腾。";
        }
        if (hasFuYin && hasFanYin) {
            return "局部动爻同时出现伏吟与反吟，推进中夹杂停滞和反复。";
        }
        if (hasFuYin) {
            return "局部动爻伏吟，事情推进偏慢。";
        }
        return "局部动爻反吟，事情更易反复。";
    }
}
