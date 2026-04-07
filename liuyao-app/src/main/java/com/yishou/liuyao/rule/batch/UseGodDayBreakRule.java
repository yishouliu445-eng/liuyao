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
@Order(30)
public class UseGodDayBreakRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("USE_GOD_DAY_BREAK");
        hit.setRuleName("用神日破");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中没有爻信息，无法判断日破。");
            hit.setEvidence(Map.of());
            return hit;
        }

        String useGod = UseGodLineLocator.extractUseGod(chart);
        String riChen = chart.getRiChen();
        if (useGod == null || useGod.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未提供用神信息，无法判断日破。");
            hit.setEvidence(Map.of());
            return hit;
        }
        if (riChen == null || riChen.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未提供日辰信息，无法判断日破。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        String riBranch = UseGodLineLocator.extractBranch(riChen);
        if (riBranch == null) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("无法从日辰中提取地支，无法判断日破。");
            hit.setEvidence(Map.of("riChen", riChen));
            return hit;
        }

        List<LineInfo> useGodLines = UseGodLineLocator.findUseGodLines(chart, useGod);
        List<Map<String, Object>> brokenTargets = new ArrayList<>();
        for (LineInfo line : useGodLines) {
            if (line.getBranch() != null && UseGodLineLocator.isChong(riBranch, line.getBranch())) {
                // 目标爻摘要统一保留行信息，再补本规则专属的日辰地支。
                Map<String, Object> target = UseGodLineLocator.summarizeLine(line);
                target.put("riBranch", riBranch);
                brokenTargets.add(target);
            }
        }

        if (brokenTargets.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前用神未见日破。");
            Map<String, Object> evidence = UseGodLineLocator.baseChartEvidence(chart, useGod);
            evidence.put("riChen", riChen);
            evidence.put("riBranch", riBranch);
            UseGodLineLocator.putTargets(evidence, brokenTargets);
            hit.setEvidence(evidence);
            return hit;
        }

        hit.setHit(true);
        hit.setImpactLevel("HIGH");
        hit.setHitReason("当前用神受日辰相冲，存在日破信号。");
        // 证据层保留整张盘的关键上下文，再叠加本规则特有的日辰信息。
        Map<String, Object> evidence = UseGodLineLocator.baseChartEvidence(chart, useGod);
        evidence.put("riChen", riChen);
        evidence.put("riBranch", riBranch);
        UseGodLineLocator.putTargets(evidence, brokenTargets);
        hit.setEvidence(evidence);
        return hit;
    }

}
