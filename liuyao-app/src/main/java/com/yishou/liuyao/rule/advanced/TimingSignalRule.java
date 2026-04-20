package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.batch.UseGodLineLocator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(35)
public class TimingSignalRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("TIMING_SIGNAL");
        hit.setRuleName("应期信号");

        if (chart == null || chart.getLines() == null || chart.getLines().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中没有爻信息，无法判断应期信号。");
            hit.setEvidence(Map.of());
            return hit;
        }

        String useGod = UseGodLineLocator.extractUseGod(chart);
        if (useGod == null || useGod.isBlank()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未提供用神信息，无法判断应期信号。");
            hit.setEvidence(Map.of());
            return hit;
        }

        boolean resolvedFromHidden = false;
        LineInfo target = resolveVisibleUseGodLine(chart, useGod);
        if (target == null) {
            target = UseGodLineLocator.findHiddenUseGodLines(chart, useGod).stream().findFirst().orElse(null);
            resolvedFromHidden = target != null;
        }
        if (target == null) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("盘面中未找到可用于判断应期的用神线索。");
            hit.setEvidence(Map.of("useGod", useGod));
            return hit;
        }

        String branch = resolvedFromHidden ? target.getFuShenBranch() : target.getBranch();
        String wuXing = resolvedFromHidden ? target.getFuShenWuXing() : target.getWuXing();
        String yueBranch = UseGodLineLocator.extractBranch(chart.getYueJian());
        String riBranch = UseGodLineLocator.extractBranch(chart.getRiChen());
        String yueWuXing = UseGodLineLocator.branchToWuXing(yueBranch);
        String riWuXing = UseGodLineLocator.branchToWuXing(riBranch);

        List<String> drivers = new ArrayList<>();
        String timingBucket;
        String timingHint;
        if (branch != null && chart.getKongWang() != null && chart.getKongWang().contains(branch)) {
            timingBucket = "DELAYED";
            timingHint = "用神落空，待出空后再看应期。";
            drivers.add("空亡");
        } else if (branch != null && (UseGodLineLocator.isChong(branch, yueBranch) || UseGodLineLocator.isChong(branch, riBranch))) {
            timingBucket = "DELAYED";
            timingHint = "用神受冲，宜待冲破之势过去后再看应。";
            drivers.add("冲破");
        } else if (resolvedFromHidden && UseGodLineLocator.controls(target.getFlyShenWuXing(), wuXing)) {
            timingBucket = "DELAYED";
            timingHint = "用神伏而受制，应期偏后，宜待压伏之势松动。";
            drivers.add("伏神受制");
        } else if (!resolvedFromHidden && "化进".equals(UseGodLineLocator.resolveTransformTrend(target.getBranch(), target.getChangeBranch()))) {
            timingBucket = "SHORT_TERM";
            timingHint = "用神化进，近期可见推进或消息。";
            drivers.add("化进");
        } else if (!resolvedFromHidden && Boolean.TRUE.equals(target.getIsMoving())) {
            timingBucket = "SHORT_TERM";
            timingHint = "用神发动，近日更容易出现动静。";
            drivers.add("发动");
        } else if (isSupported(yueWuXing, wuXing) || isSupported(riWuXing, wuXing)) {
            timingBucket = "MONTH";
            timingHint = "用神得月日扶助，可先以月内为观察窗口。";
            drivers.add("月日扶助");
        } else if (resolvedFromHidden && (isSupported(yueWuXing, wuXing)
                || isSupported(riWuXing, wuXing)
                || UseGodLineLocator.generates(target.getFlyShenWuXing(), wuXing))) {
            timingBucket = "MONTH";
            timingHint = "伏神虽隐但尚有根气，可先以月内留意应期。";
            drivers.add("伏神得扶");
        } else {
            timingBucket = "LATER";
            timingHint = "当前时机不算很快，更适合按稍后渐应来观察。";
            drivers.add("静候");
        }

        Map<String, Object> evidence = new LinkedHashMap<>(UseGodLineLocator.baseChartEvidence(chart, useGod));
        evidence.put("targetLineIndex", target.getIndex());
        evidence.put("resolvedFromHiddenUseGod", resolvedFromHidden);
        evidence.put("timingBucket", timingBucket);
        evidence.put("timingHint", timingHint);
        evidence.put("drivers", drivers);
        evidence.put("targetLine", resolvedFromHidden ? Map.of(
                "lineIndex", target.getIndex(),
                "branch", branch == null ? "" : branch,
                "wuXing", wuXing == null ? "" : wuXing,
                "liuQin", target.getFuShenLiuQin() == null ? "" : target.getFuShenLiuQin()
        ) : UseGodLineLocator.summarizeLine(target));
        hit.setHit(true);
        hit.setImpactLevel("LOW");
        hit.setHitReason("已给出第一版确定性应期窗口。");
        hit.setEvidence(evidence);
        return hit;
    }

    private LineInfo resolveVisibleUseGodLine(ChartSnapshot chart, String useGod) {
        Integer useGodLineIndex = null;
        if (chart.getExt() != null && chart.getExt().get("useGodLineIndex") instanceof Number number) {
            useGodLineIndex = number.intValue();
        }
        if (useGodLineIndex != null) {
            Integer finalUseGodLineIndex = useGodLineIndex;
            return UseGodLineLocator.findUseGodLines(chart, useGod).stream()
                    .filter(line -> finalUseGodLineIndex.equals(line.getIndex()))
                    .findFirst()
                    .orElse(null);
        }
        return UseGodLineLocator.findUseGodLines(chart, useGod).stream().findFirst().orElse(null);
    }

    private boolean isSupported(String sourceWuXing, String targetWuXing) {
        if (sourceWuXing == null || sourceWuXing.isBlank() || targetWuXing == null || targetWuXing.isBlank()) {
            return false;
        }
        return sourceWuXing.equals(targetWuXing) || UseGodLineLocator.generates(sourceWuXing, targetWuXing);
    }
}
