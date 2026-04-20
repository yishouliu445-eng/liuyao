package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.batch.UseGodLineLocator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Order(20)
public class ShiYingRelationRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("SHI_YING_RELATION");
        hit.setRuleName("世应关系");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中没有爻信息，无法分析世应关系。");
            hit.setEvidence(Map.of());
            return hit;
        }

        Optional<LineInfo> shiOpt = chart.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getIsShi())).findFirst();
        Optional<LineInfo> yingOpt = chart.getLines().stream().filter(line -> Boolean.TRUE.equals(line.getIsYing())).findFirst();

        if (shiOpt.isEmpty() || yingOpt.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("世爻或应爻缺失，无法分析世应关系。");
            hit.setEvidence(Map.of("hasShi", shiOpt.isPresent(), "hasYing", yingOpt.isPresent()));
            return hit;
        }

        LineInfo shi = shiOpt.get();
        LineInfo ying = yingOpt.get();
        String shiWuXing = shi.getWuXing();
        String yingWuXing = ying.getWuXing();

        if (shiWuXing == null || yingWuXing == null || shiWuXing.isBlank() || yingWuXing.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("世应五行信息缺失，无法分析生克关系。");
            hit.setEvidence(Map.of("shiWuXing", shiWuXing, "yingWuXing", yingWuXing));
            return hit;
        }

        Map<String, Object> evidence = new LinkedHashMap<>(UseGodLineLocator.baseChartEvidence(chart, null));
        java.util.List<Map<String, Object>> targets = java.util.List.of(
                UseGodLineLocator.summarizeLine(shi),
                UseGodLineLocator.summarizeLine(ying)
        );
        evidence.put("shiLine", UseGodLineLocator.summarizeLine(shi));
        evidence.put("yingLine", UseGodLineLocator.summarizeLine(ying));
        UseGodLineLocator.putTargets(evidence, targets);
        evidence.put("shiIndex", shi.getIndex());
        evidence.put("yingIndex", ying.getIndex());
        evidence.put("shiWuXing", shiWuXing);
        evidence.put("yingWuXing", yingWuXing);
        evidence.put("relation", resolveRelation(shiWuXing, yingWuXing));
        evidence.put("distance", Math.abs(shi.getIndex() - ying.getIndex()));
        evidence.put("shiMoving", Boolean.TRUE.equals(shi.getIsMoving()));
        evidence.put("yingMoving", Boolean.TRUE.equals(ying.getIsMoving()));

        hit.setHit(true);
        hit.setImpactLevel("MEDIUM");
        hit.setHitReason("已识别世应之间的五行关系，可供后续解释层使用。");
        hit.setEvidence(evidence);
        return hit;
    }

    private String resolveRelation(String shiWuXing, String yingWuXing) {
        if (shiWuXing.equals(yingWuXing)) return "比和";
        if (UseGodLineLocator.generates(shiWuXing, yingWuXing)) return "世生应";
        if (UseGodLineLocator.generates(yingWuXing, shiWuXing)) return "应生世";
        if (UseGodLineLocator.controls(shiWuXing, yingWuXing)) return "世克应";
        if (UseGodLineLocator.controls(yingWuXing, shiWuXing)) return "应克世";
        return "未知";
    }
}
