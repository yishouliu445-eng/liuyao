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
@Order(31)
public class UseGodStrengthRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("USE_GOD_STRENGTH");
        hit.setRuleName("用神强弱");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中没有爻信息，无法判断用神强弱。");
            hit.setEvidence(Map.of());
            return hit;
        }

        String useGod = UseGodLineLocator.extractUseGod(chart);
        if (useGod == null || useGod.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未提供用神信息，无法判断用神强弱。");
            hit.setEvidence(Map.of());
            return hit;
        }

        List<LineInfo> targets = UseGodLineLocator.findUseGodLines(chart, useGod);
        if (targets.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中未找到对应的用神爻。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        String yueBranch = UseGodLineLocator.extractBranch(chart.getYueJian());
        String riBranch = UseGodLineLocator.extractBranch(chart.getRiChen());
        String yueWuXing = UseGodLineLocator.branchToWuXing(yueBranch);
        String riWuXing = UseGodLineLocator.branchToWuXing(riBranch);

        List<Map<String, Object>> details = new ArrayList<>();
        int bestScore = Integer.MIN_VALUE;
        String bestLevel = "UNKNOWN";

        for (LineInfo line : targets) {
            String lineWuXing = line.getWuXing();
            int score = 0;
            if (lineWuXing != null && yueWuXing != null) {
                if (lineWuXing.equals(yueWuXing) || UseGodLineLocator.generates(yueWuXing, lineWuXing)) score += 2;
                if (UseGodLineLocator.controls(yueWuXing, lineWuXing)) score -= 2;
            }
            if (lineWuXing != null && riWuXing != null) {
                if (lineWuXing.equals(riWuXing) || UseGodLineLocator.generates(riWuXing, lineWuXing)) score += 1;
                if (UseGodLineLocator.controls(riWuXing, lineWuXing)) score -= 1;
            }
            String level = toLevel(score);
            if (score > bestScore) {
                bestScore = score;
                bestLevel = level;
            }
            details.add(Map.of(
                    "lineIndex", line.getIndex(),
                    "liuQin", line.getLiuQin(),
                    "branch", line.getBranch() == null ? "" : line.getBranch(),
                    "wuXing", lineWuXing == null ? "" : lineWuXing,
                    "score", score,
                    "level", level
            ));
        }

        hit.setHit(true);
        hit.setImpactLevel("MEDIUM");
        hit.setHitReason("已按月建、日辰对用神做启发式强弱评估。");
        hit.setEvidence(Map.of(
                "useGod", useGod,
                "yueJian", chart.getYueJian(),
                "yueBranch", yueBranch == null ? "" : yueBranch,
                "riChen", chart.getRiChen(),
                "riBranch", riBranch == null ? "" : riBranch,
                "bestScore", bestScore,
                "bestLevel", bestLevel,
                "details", details
        ));
        return hit;
    }

    private String toLevel(int score) {
        if (score >= 2) return "STRONG";
        if (score >= 0) return "MEDIUM";
        return "WEAK";
    }
}
