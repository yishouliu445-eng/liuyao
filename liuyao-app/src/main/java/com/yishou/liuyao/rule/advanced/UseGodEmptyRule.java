package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.batch.UseGodLineLocator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Order(21)
public class UseGodEmptyRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("USE_GOD_EMPTY");
        hit.setRuleName("用神逢空");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中没有爻信息，无法判断用神空亡。");
            hit.setEvidence(Map.of());
            return hit;
        }

        String useGod = UseGodLineLocator.extractUseGod(chart);
        List<String> kongWang = chart.getKongWang();
        if (useGod == null || useGod.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未提供用神信息，无法判断是否空亡。");
            hit.setEvidence(Map.of());
            return hit;
        }
        if (kongWang == null || kongWang.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未提供空亡信息，无法判断用神是否落空。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        List<LineInfo> useGodLines = UseGodLineLocator.findUseGodLines(chart, useGod);
        if (useGodLines.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中未找到对应的用神爻。");
            Map<String, Object> evidence = UseGodLineLocator.baseChartEvidence(chart, useGod);
            evidence.put("kongWang", kongWang);
            hit.setEvidence(evidence);
            return hit;
        }

        List<Map<String, Object>> emptyTargets = new ArrayList<>();
        for (LineInfo line : useGodLines) {
            String branch = line.getBranch();
            if (branch != null && kongWang.contains(branch)) {
                // 目标爻证据保持统一摘要结构，避免接口层再做字段拼装。
                emptyTargets.add(UseGodLineLocator.summarizeLine(line));
            }
        }

        if (emptyTargets.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前用神未落空亡。");
            Map<String, Object> evidence = UseGodLineLocator.baseChartEvidence(chart, useGod);
            evidence.put("kongWang", kongWang);
            UseGodLineLocator.putTargets(evidence, emptyTargets);
            hit.setEvidence(evidence);
            return hit;
        }

        hit.setHit(true);
        hit.setImpactLevel("HIGH");
        hit.setHitReason("当前用神落空亡，后续应结合冲空、填实与动变继续分析。");
        Map<String, Object> evidence = UseGodLineLocator.baseChartEvidence(chart, useGod);
        evidence.put("kongWang", kongWang);
        UseGodLineLocator.putTargets(evidence, emptyTargets);
        hit.setEvidence(evidence);
        return hit;
    }
}
