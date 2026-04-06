package com.yishou.liuyao.rule.basic;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(11)
public class ShiYingExistsRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("SHI_YING_EXISTS");
        hit.setRuleName("世应定位存在");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("HIGH");
            hit.setHitReason("盘面中没有可用爻信息，无法校验世应。");
            hit.setEvidence(Map.of());
            return hit;
        }

        List<LineInfo> shiLines = chart.getLines().stream()
                .filter(line -> Boolean.TRUE.equals(line.getIsShi()))
                .toList();
        List<LineInfo> yingLines = chart.getLines().stream()
                .filter(line -> Boolean.TRUE.equals(line.getIsYing()))
                .toList();

        Map<String, Object> evidence = new HashMap<>();
        evidence.put("shiIndexes", shiLines.stream().map(LineInfo::getIndex).toList());
        evidence.put("yingIndexes", yingLines.stream().map(LineInfo::getIndex).toList());

        if (shiLines.size() != 1 || yingLines.size() != 1) {
            hit.setHit(false);
            hit.setImpactLevel("HIGH");
            hit.setHitReason("世应定位数量异常，正常应各出现 1 次。");
            hit.setEvidence(evidence);
            return hit;
        }

        int shiIndex = shiLines.get(0).getIndex();
        int yingIndex = yingLines.get(0).getIndex();
        evidence.put("shiIndex", shiIndex);
        evidence.put("yingIndex", yingIndex);
        evidence.put("distance", Math.abs(shiIndex - yingIndex));

        hit.setHit(true);
        hit.setImpactLevel("MEDIUM");
        hit.setHitReason("世应定位完整，可作为后续规则与分析的基础。");
        hit.setEvidence(evidence);
        return hit;
    }
}
