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
            String relation = null;
            if (UseGodLineLocator.generates(movingWuXing, useGodWuXing)) {
                relation = "动爻生用神";
            } else if (UseGodLineLocator.controls(movingWuXing, useGodWuXing)) {
                relation = "动爻克用神";
            }
            String changeRelation = null;
            if (moving.getChangeWuXing() != null && !moving.getChangeWuXing().isBlank()) {
                if (UseGodLineLocator.generates(moving.getChangeWuXing(), useGodWuXing)) {
                    changeRelation = "变爻生用神";
                } else if (UseGodLineLocator.controls(moving.getChangeWuXing(), useGodWuXing)) {
                    changeRelation = "变爻克用神";
                }
            }
            if (relation != null || changeRelation != null) {
                Map<String, Object> effect = UseGodLineLocator.summarizeLine(moving);
                effect.put("movingWuXing", movingWuXing);
                effect.put("useGodWuXing", useGodWuXing);
                effect.put("relation", relation == null ? "" : relation);
                effect.put("changeRelation", changeRelation == null ? "" : changeRelation);
                effects.add(effect);
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
        hit.setEvidence(Map.of(
                "useGod", useGod,
                "useGodWuXing", useGodWuXing,
                "useGodLines", useGodLines.stream().map(UseGodLineLocator::summarizeLine).toList(),
                "effects", effects
        ));
        return hit;
    }
}
