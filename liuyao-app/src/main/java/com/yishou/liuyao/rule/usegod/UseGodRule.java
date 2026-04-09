package com.yishou.liuyao.rule.usegod;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(0)
public class UseGodRule implements Rule {

    private final UseGodSelector useGodSelector;

    public UseGodRule(UseGodSelector useGodSelector) {
        this.useGodSelector = useGodSelector;
    }

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        UseGodSelection selection = useGodSelector.select(chart);
        RuleHit hit = new RuleHit();
        hit.setRuleCode("USE_GOD_SELECTION");
        hit.setRuleName("用神选择");

        if (selection.getUseGod() == null) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("未识别出明确问题意图，暂不自动选择用神。");
            hit.setEvidence(Map.of("intent", selection.getIntent().name(), "configVersion", selection.getConfigVersion()));
            return hit;
        }

        hit.setHit(true);
        hit.setImpactLevel("HIGH");
        hit.setHitReason("根据问题意图选择当前优先观察对象。");
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("intent", selection.getIntent().name());
        evidence.put("useGod", selection.getUseGod().getDisplayName());
        evidence.put("priority", selection.getPriority());
        evidence.put("scenario", selection.getScenario());
        evidence.put("note", selection.getNote());
        evidence.put("configVersion", selection.getConfigVersion());
        evidence.put("selectedLineIndex", selection.getSelectedLineIndex());
        evidence.put("candidateLineIndexes", selection.getCandidateLineIndexes());
        evidence.put("selectionStrategy", selection.getSelectionStrategy());
        evidence.put("selectionReason", selection.getSelectionReason());
        evidence.put("fallbackApplied", Boolean.TRUE.equals(selection.getFallbackApplied()));
        evidence.put("fallbackStrategy", selection.getFallbackStrategy());
        evidence.put("scoreDetails", selection.getScoreDetails());
        hit.setEvidence(evidence);
        chart.setUseGod(selection.getUseGod().getDisplayName());
        chart.getExt().put("useGod", selection.getUseGod().getDisplayName());
        chart.getExt().put("useGodLineIndex", selection.getSelectedLineIndex());
        return hit;
    }
}
