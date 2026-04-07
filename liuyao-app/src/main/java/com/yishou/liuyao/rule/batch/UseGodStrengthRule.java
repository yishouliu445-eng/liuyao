package com.yishou.liuyao.rule.batch;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
            // 这层评分是“工程化第一版”，核心目标是把月建、日辰、空亡、动变影响收束到统一证据里。
            String lineWuXing = line.getWuXing();
            int score = 0;
            boolean kongWang = chart.getKongWang() != null && line.getBranch() != null && chart.getKongWang().contains(line.getBranch());
            boolean monthBreak = line.getBranch() != null && yueBranch != null && UseGodLineLocator.isChong(line.getBranch(), yueBranch);
            boolean dayBreak = line.getBranch() != null && riBranch != null && UseGodLineLocator.isChong(line.getBranch(), riBranch);
            if (lineWuXing != null && yueWuXing != null) {
                if (lineWuXing.equals(yueWuXing) || UseGodLineLocator.generates(yueWuXing, lineWuXing)) score += 2;
                if (UseGodLineLocator.controls(yueWuXing, lineWuXing)) score -= 2;
            }
            if (lineWuXing != null && riWuXing != null) {
                if (lineWuXing.equals(riWuXing) || UseGodLineLocator.generates(riWuXing, lineWuXing)) score += 1;
                if (UseGodLineLocator.controls(riWuXing, lineWuXing)) score -= 1;
            }
            if (Boolean.TRUE.equals(line.getIsMoving())) {
                score += 1;
            }
            if (kongWang) {
                score -= 1;
            }
            if (monthBreak) {
                score -= 2;
            }
            if (dayBreak) {
                score -= 1;
            }
            String level = toLevel(score);
            if (score > bestScore) {
                bestScore = score;
                bestLevel = level;
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("lineIndex", line.getIndex());
            detail.put("liuQin", line.getLiuQin());
            detail.put("branch", line.getBranch() == null ? "" : line.getBranch());
            detail.put("wuXing", lineWuXing == null ? "" : lineWuXing);
            detail.put("moving", Boolean.TRUE.equals(line.getIsMoving()));
            detail.put("kongWang", kongWang);
            detail.put("monthBreak", monthBreak);
            detail.put("dayBreak", dayBreak);
            detail.put("changeWuXingRelation", UseGodLineLocator.relationOf(line.getChangeWuXing(), lineWuXing));
            detail.put("score", score);
            detail.put("level", level);
            details.add(detail);
        }

        hit.setHit(true);
        hit.setImpactLevel("MEDIUM");
        hit.setHitReason("已按月建、日辰对用神做启发式强弱评估。");
        Map<String, Object> evidence = new LinkedHashMap<>(UseGodLineLocator.baseChartEvidence(chart, useGod));
        evidence.put("yueJian", chart.getYueJian());
        evidence.put("yueBranch", yueBranch == null ? "" : yueBranch);
        evidence.put("riChen", chart.getRiChen());
        evidence.put("riBranch", riBranch == null ? "" : riBranch);
        evidence.put("kongWang", chart.getKongWang());
        UseGodLineLocator.putTargets(evidence, targets.stream().map(UseGodLineLocator::summarizeLine).toList());
        evidence.put("bestScore", bestScore);
        evidence.put("bestLevel", bestLevel);
        evidence.put("details", details);
        hit.setEvidence(evidence);
        return hit;
    }

    private String toLevel(int score) {
        if (score >= 2) return "STRONG";
        if (score >= 0) return "MEDIUM";
        return "WEAK";
    }
}
