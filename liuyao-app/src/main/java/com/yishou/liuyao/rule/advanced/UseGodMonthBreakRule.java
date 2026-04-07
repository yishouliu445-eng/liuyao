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
@Order(22)
public class UseGodMonthBreakRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("USE_GOD_MONTH_BREAK");
        hit.setRuleName("用神月破");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中没有爻信息，无法判断月破。");
            hit.setEvidence(Map.of());
            return hit;
        }

        String useGod = UseGodLineLocator.extractUseGod(chart);
        String yueJian = chart.getYueJian();
        if (useGod == null || useGod.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未提供用神信息，无法判断月破。");
            hit.setEvidence(Map.of());
            return hit;
        }
        if (yueJian == null || yueJian.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未提供月建信息，无法判断月破。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        List<LineInfo> useGodLines = UseGodLineLocator.findUseGodLines(chart, useGod);
        if (useGodLines.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中未找到对应的用神爻。");
            Map<String, Object> evidence = UseGodLineLocator.baseChartEvidence(chart, useGod);
            evidence.put("yueJian", yueJian);
            evidence.put("yueBranch", UseGodLineLocator.extractBranch(yueJian));
            hit.setEvidence(evidence);
            return hit;
        }

        // 月破除了是否相冲，也把月建地支回写到 evidence，便于接口层直接展示。
        String yueBranch = UseGodLineLocator.extractBranch(yueJian);
        List<Map<String, Object>> brokenTargets = new ArrayList<>();
        for (LineInfo line : useGodLines) {
            String branch = line.getBranch();
            if (branch != null && UseGodLineLocator.isChong(yueJian, branch)) {
                // 月破命中时保留目标爻摘要，并显式带出月建地支，方便前端直接展示。
                Map<String, Object> target = UseGodLineLocator.summarizeLine(line);
                target.put("monthBranch", yueBranch == null ? "" : yueBranch);
                brokenTargets.add(target);
            }
        }

        if (brokenTargets.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前用神未见月破。");
            Map<String, Object> evidence = UseGodLineLocator.baseChartEvidence(chart, useGod);
            evidence.put("yueJian", yueJian);
            evidence.put("yueBranch", yueBranch == null ? "" : yueBranch);
            UseGodLineLocator.putTargets(evidence, brokenTargets);
            hit.setEvidence(evidence);
            return hit;
        }

        hit.setHit(true);
        hit.setImpactLevel("HIGH");
        hit.setHitReason("当前用神受月建相冲，存在月破信号。");
        Map<String, Object> evidence = UseGodLineLocator.baseChartEvidence(chart, useGod);
        evidence.put("yueJian", yueJian);
        evidence.put("yueBranch", yueBranch == null ? "" : yueBranch);
        UseGodLineLocator.putTargets(evidence, brokenTargets);
        hit.setEvidence(evidence);
        return hit;
    }
}
