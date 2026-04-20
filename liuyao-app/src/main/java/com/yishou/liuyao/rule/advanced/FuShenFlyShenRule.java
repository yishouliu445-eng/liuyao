package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.batch.UseGodLineLocator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Component
@Order(33)
public class FuShenFlyShenRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("FU_SHEN_FLY_SHEN");
        hit.setRuleName("伏神飞神");

        String useGod = UseGodLineLocator.extractUseGod(chart);
        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty() || useGod == null || useGod.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("缺少盘面或用神信息，无法判断伏神飞神。");
            hit.setEvidence(Map.of());
            return hit;
        }

        if (!UseGodLineLocator.findUseGodLines(chart, useGod).isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("用神已经上卦，无需转伏神。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        List<LineInfo> hiddenLines = UseGodLineLocator.findHiddenUseGodLines(chart, useGod);
        if (hiddenLines.isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("用神不上卦，且盘面未找到对应伏神。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        LineInfo hiddenLine = hiddenLines.get(0);
        Map<String, Object> evidence = new LinkedHashMap<>(UseGodLineLocator.baseChartEvidence(chart, useGod));
        evidence.put("resolvedUseGodSource", useGod);
        evidence.put("hiddenUseGodLineIndex", hiddenLine.getIndex());
        evidence.put("hiddenLine", Map.of(
                "liuQin", safe(hiddenLine.getFuShenLiuQin()),
                "branch", safe(hiddenLine.getFuShenBranch()),
                "wuXing", safe(hiddenLine.getFuShenWuXing())
        ));
        evidence.put("flyLine", Map.of(
                "liuQin", safe(hiddenLine.getFlyShenLiuQin()),
                "branch", safe(hiddenLine.getFlyShenBranch()),
                "wuXing", safe(hiddenLine.getFlyShenWuXing())
        ));
        String flyShenRelation = resolveFlyShenRelation(hiddenLine);
        String hiddenBranch = hiddenLine.getFuShenBranch();
        String hiddenWuXing = hiddenLine.getFuShenWuXing();
        String yueBranch = UseGodLineLocator.extractBranch(chart.getYueJian());
        String riBranch = UseGodLineLocator.extractBranch(chart.getRiChen());
        String yueWuXing = UseGodLineLocator.branchToWuXing(yueBranch);
        String riWuXing = UseGodLineLocator.branchToWuXing(riBranch);
        List<String> supportSources = new ArrayList<>();
        if (supportsHidden(yueWuXing, hiddenWuXing)) {
            supportSources.add("月建");
        }
        if (supportsHidden(riWuXing, hiddenWuXing)) {
            supportSources.add("日辰");
        }
        List<String> breakSources = new ArrayList<>();
        if (isBrokenBy(yueBranch, hiddenBranch)) {
            breakSources.add("月建");
        }
        if (isBrokenBy(riBranch, hiddenBranch)) {
            breakSources.add("日辰");
        }
        evidence.put("hiddenUseGodFound", true);
        evidence.put("flyShenRelation", flyShenRelation);
        evidence.put("flyShenSuppress", "飞神克伏".equals(flyShenRelation));
        evidence.put("hiddenUseGodSupported", !supportSources.isEmpty());
        evidence.put("hiddenUseGodBroken", !breakSources.isEmpty());
        evidence.put("supportSources", supportSources);
        evidence.put("breakSources", breakSources);
        hit.setHit(true);
        hit.setImpactLevel("MEDIUM");
        hit.setHitReason("用神不上卦，已转入伏神并给出飞伏关系。");
        hit.setEvidence(evidence);
        return hit;
    }

    private String resolveFlyShenRelation(LineInfo line) {
        String flyWuXing = line.getFlyShenWuXing();
        String fuWuXing = line.getFuShenWuXing();
        if (flyWuXing == null || fuWuXing == null || flyWuXing.isBlank() || fuWuXing.isBlank()) {
            return "";
        }
        if (UseGodLineLocator.generates(flyWuXing, fuWuXing)) {
            return "飞神生伏";
        }
        if (UseGodLineLocator.controls(flyWuXing, fuWuXing)) {
            return "飞神克伏";
        }
        if (UseGodLineLocator.generates(fuWuXing, flyWuXing)) {
            return "伏神生飞";
        }
        if (UseGodLineLocator.controls(fuWuXing, flyWuXing)) {
            return "伏神克飞";
        }
        return "飞伏同气";
    }

    private boolean supportsHidden(String sourceWuXing, String hiddenWuXing) {
        if (sourceWuXing == null || sourceWuXing.isBlank() || hiddenWuXing == null || hiddenWuXing.isBlank()) {
            return false;
        }
        return hiddenWuXing.equals(sourceWuXing) || UseGodLineLocator.generates(sourceWuXing, hiddenWuXing);
    }

    private boolean isBrokenBy(String sourceBranch, String hiddenBranch) {
        if (sourceBranch == null || sourceBranch.isBlank() || hiddenBranch == null || hiddenBranch.isBlank()) {
            return false;
        }
        return UseGodLineLocator.isChong(sourceBranch, hiddenBranch);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
