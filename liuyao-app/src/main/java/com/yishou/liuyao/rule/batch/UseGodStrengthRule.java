package com.yishou.liuyao.rule.batch;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Integer selectedLineIndex = null;
        List<String> bestStateFlags = List.of();

        for (LineInfo line : targets) {
            String lineWuXing = line.getWuXing();
            int score = 0;
            boolean kongWang = chart.getKongWang() != null && line.getBranch() != null && chart.getKongWang().contains(line.getBranch());
            boolean monthBreak = line.getBranch() != null && yueBranch != null && UseGodLineLocator.isChong(line.getBranch(), yueBranch);
            boolean dayBreak = line.getBranch() != null && riBranch != null && UseGodLineLocator.isChong(line.getBranch(), riBranch);
            String seasonState = resolveSeasonState(yueWuXing, lineWuXing);
            Set<String> stateFlags = new LinkedHashSet<>();
            if (!seasonState.isBlank()) {
                stateFlags.add(seasonState);
            }
            if (lineWuXing != null && yueWuXing != null) {
                if (lineWuXing.equals(yueWuXing)) {
                    score += 2;
                } else if (UseGodLineLocator.generates(yueWuXing, lineWuXing)) {
                    score += 1;
                } else if (UseGodLineLocator.controls(yueWuXing, lineWuXing)) {
                    score -= 2;
                } else if (UseGodLineLocator.generates(lineWuXing, yueWuXing)) {
                    score -= 1;
                }
            }
            if (lineWuXing != null && riWuXing != null) {
                if (lineWuXing.equals(riWuXing) || UseGodLineLocator.generates(riWuXing, lineWuXing)) {
                    score += 1;
                }
                if (UseGodLineLocator.controls(riWuXing, lineWuXing)) {
                    score -= 1;
                }
            }
            if (Boolean.TRUE.equals(line.getIsMoving())) {
                score += 1;
                stateFlags.add("动");
            }
            if (kongWang) {
                score -= 1;
                stateFlags.add("空");
            }
            if (monthBreak) {
                score -= 2;
                stateFlags.add("月破");
            }
            if (dayBreak) {
                score -= 1;
                stateFlags.add("日破");
            }
            if (line.getChangeBranch() != null && !line.getChangeBranch().isBlank()) {
                stateFlags.add("变");
            }
            String level = toLevel(score);
            if (score > bestScore) {
                bestScore = score;
                bestLevel = level;
                selectedLineIndex = line.getIndex();
                bestStateFlags = List.copyOf(stateFlags);
            }
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("lineIndex", line.getIndex());
            detail.put("liuQin", line.getLiuQin());
            detail.put("branch", line.getBranch() == null ? "" : line.getBranch());
            detail.put("wuXing", lineWuXing == null ? "" : lineWuXing);
            detail.put("seasonState", seasonState);
            detail.put("stateFlags", List.copyOf(stateFlags));
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
        evidence.put("selectedLineIndex", selectedLineIndex);
        evidence.put("bestScore", bestScore);
        evidence.put("bestLevel", bestLevel);
        evidence.put("bestStateFlags", bestStateFlags);
        evidence.put("details", details);
        hit.setEvidence(evidence);
        return hit;
    }

    private String resolveSeasonState(String yueWuXing, String lineWuXing) {
        if (yueWuXing == null || yueWuXing.isBlank() || lineWuXing == null || lineWuXing.isBlank()) {
            return "";
        }
        if (lineWuXing.equals(yueWuXing)) {
            return "旺";
        }
        if (UseGodLineLocator.generates(yueWuXing, lineWuXing)) {
            return "相";
        }
        if (UseGodLineLocator.generates(lineWuXing, yueWuXing)) {
            return "休";
        }
        if (UseGodLineLocator.controls(yueWuXing, lineWuXing)) {
            return "囚";
        }
        return "";
    }

    private String toLevel(int score) {
        if (score >= 2) return "STRONG";
        if (score >= 0) return "MEDIUM";
        return "WEAK";
    }
}
