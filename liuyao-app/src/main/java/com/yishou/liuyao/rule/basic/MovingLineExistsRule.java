package com.yishou.liuyao.rule.basic;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.batch.UseGodLineLocator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(10)
public class MovingLineExistsRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("MOVING_LINE_EXISTS");
        hit.setRuleName("存在动爻");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中没有可用爻信息。");
            hit.setEvidence(Map.of());
            return hit;
        }

        List<LineInfo> movingLines = chart.getLines().stream()
                .filter(line -> Boolean.TRUE.equals(line.getIsMoving()))
                .toList();

        if (movingLines.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前为静卦，无动爻。");
            hit.setEvidence(Map.of("movingLineCount", 0));
            return hit;
        }

        hit.setHit(true);
        hit.setImpactLevel("MEDIUM");
        hit.setHitReason("当前盘面存在动爻，后续需要结合变卦与动爻关系分析。");

        Map<String, Object> evidence = new HashMap<>();
        evidence.put("movingLineCount", movingLines.size());
        evidence.put("movingLineIndexes", movingLines.stream().map(LineInfo::getIndex).toList());
        evidence.put("changeTargets", movingLines.stream().map(UseGodLineLocator::summarizeLine).toList());
        hit.setEvidence(evidence);
        return hit;
    }
}
