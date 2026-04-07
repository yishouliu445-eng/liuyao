package com.yishou.liuyao.rule.batch;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Order(32)
public class MovingLineAffectUseGodRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("MOVING_LINE_AFFECT_USE_GOD");
        hit.setRuleName("动爻影响用神");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中没有爻信息，无法判断动爻是否影响用神。");
            hit.setEvidence(Map.of());
            return hit;
        }

        String useGod = UseGodLineLocator.extractUseGod(chart);
        if (useGod == null || useGod.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未提供用神信息，无法判断动爻是否影响用神。");
            hit.setEvidence(Map.of());
            return hit;
        }

        List<LineInfo> useGodLines = UseGodLineLocator.findUseGodLines(chart, useGod);
        if (useGodLines.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中未找到对应的用神爻。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        String useGodWuXing = useGodLines.get(0).getWuXing();
        if (useGodWuXing == null || useGodWuXing.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("用神五行缺失，无法判断动爻影响。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        List<LineInfo> movingLines = chart.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getIsMoving())).toList();
        if (movingLines.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前无动爻，无法判断对用神的影响。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        List<Map<String, Object>> effects = new ArrayList<>();
        for (LineInfo moving : movingLines) {
            String movingWuXing = moving.getWuXing();
            if (movingWuXing == null || movingWuXing.isBlank()) {
                continue;
            }
            for (LineInfo target : useGodLines) {
                String relation = relationLabel("动爻", movingWuXing, target.getWuXing());
                String changeRelation = relationLabel("变爻", moving.getChangeWuXing(), target.getWuXing());
                boolean sameLine = moving.getIndex() != null && moving.getIndex().equals(target.getIndex());
                String selfTransform = "";
                if (sameLine && moving.getChangeLiuQin() != null && !moving.getChangeLiuQin().isBlank()) {
                    selfTransform = useGod.equals(moving.getChangeLiuQin()) ? "用神发动仍属同类六亲" : "用神发动后转出他亲";
                }
                if (relation != null || changeRelation != null || !selfTransform.isBlank()) {
                    Map<String, Object> effect = UseGodLineLocator.summarizeLine(moving);
                    effect.put("movingWuXing", movingWuXing);
                    effect.put("targetLineIndex", target.getIndex());
                    effect.put("targetWuXing", target.getWuXing() == null ? "" : target.getWuXing());
                    effect.put("targetSummary", UseGodLineLocator.summarizeLine(target));
                    effect.put("useGodWuXing", useGodWuXing);
                    effect.put("relation", relation == null ? "" : relation);
                    effect.put("changeRelation", changeRelation == null ? "" : changeRelation);
                    effect.put("sameLineAsUseGod", sameLine);
                    effect.put("selfTransform", selfTransform);
                    effects.add(effect);
                }
            }
        }

        if (effects.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前动爻对用神未形成明显生克。");
            hit.setEvidence(Map.of("useGod", useGod, "useGodWuXing", useGodWuXing));
            return hit;
        }

        hit.setHit(true);
        hit.setImpactLevel("MEDIUM");
        hit.setHitReason("已识别部分动爻对用神形成生克影响。");
        Map<String, Object> evidence = new java.util.LinkedHashMap<>(UseGodLineLocator.baseChartEvidence(chart, useGod));
        evidence.put("useGodWuXing", useGodWuXing);
        UseGodLineLocator.putTargets(evidence, useGodLines.stream().map(UseGodLineLocator::summarizeLine).toList());
        evidence.put("useGodLines", useGodLines.stream().map(UseGodLineLocator::summarizeLine).toList());
        evidence.put("effects", effects);
        hit.setEvidence(evidence);
        return hit;
    }

    private String relationLabel(String prefix, String sourceWuXing, String targetWuXing) {
        if (sourceWuXing == null || sourceWuXing.isBlank() || targetWuXing == null || targetWuXing.isBlank()) {
            return null;
        }
        if (UseGodLineLocator.generates(sourceWuXing, targetWuXing)) {
            return prefix + "生用神";
        }
        if (UseGodLineLocator.controls(sourceWuXing, targetWuXing)) {
            return prefix + "克用神";
        }
        if (sourceWuXing.equals(targetWuXing)) {
            return prefix + "同用神五行";
        }
        return null;
    }
}
