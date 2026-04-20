package com.yishou.liuyao.rule.advanced;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.domain.ShenShaHit;
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
@Order(36)
public class ShenShaRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("SHEN_SHA");
        hit.setRuleName("神煞信号");

        if (chart == null || chart.getShenShaHits() == null || chart.getShenShaHits().isEmpty()) {
            hit.setHit(false);
            hit.setImpactLevel("LOW");
            hit.setHitReason("当前盘面未见首批神煞命中。");
            hit.setEvidence(Map.of("hitCount", 0));
            return hit;
        }

        Integer useGodLineIndex = resolveUseGodLineIndex(chart);
        List<Map<String, Object>> matches = new ArrayList<>();
        boolean hasNobleman = false;
        boolean useGodWithNobleman = false;
        boolean hasTravelHorse = false;
        boolean movingWithTravelHorse = false;
        boolean hasPeachBlossom = false;
        boolean useGodWithPeachBlossom = false;
        boolean hasHuaGai = false;
        boolean hasWenChang = false;
        boolean useGodWithWenChang = false;
        boolean hasGeneralStar = false;
        boolean useGodWithGeneralStar = false;
        boolean hasJieSha = false;
        boolean movingWithJieSha = false;
        boolean hasDisasterSha = false;
        boolean movingWithDisasterSha = false;
        for (ShenShaHit shenShaHit : chart.getShenShaHits()) {
            boolean onUseGod = useGodLineIndex != null
                    && shenShaHit.getLineIndexes() != null
                    && shenShaHit.getLineIndexes().contains(useGodLineIndex);
            boolean onMoving = shenShaHit.getLineIndexes() != null
                    && shenShaHit.getLineIndexes().stream()
                    .map(index -> findLine(chart, index))
                    .anyMatch(line -> line != null && Boolean.TRUE.equals(line.getIsMoving()));
            matches.add(Map.of(
                    "code", shenShaHit.getCode() == null ? "" : shenShaHit.getCode(),
                    "name", shenShaHit.getName() == null ? "" : shenShaHit.getName(),
                    "branch", shenShaHit.getBranch() == null ? "" : shenShaHit.getBranch(),
                    "lineIndexes", shenShaHit.getLineIndexes() == null ? List.of() : shenShaHit.getLineIndexes(),
                    "onUseGod", onUseGod,
                    "onMovingLine", onMoving,
                    "summary", shenShaHit.getSummary() == null ? "" : shenShaHit.getSummary()
            ));
            switch (String.valueOf(shenShaHit.getCode())) {
                case "NOBLEMAN" -> {
                    hasNobleman = true;
                    useGodWithNobleman = useGodWithNobleman || onUseGod;
                }
                case "TRAVEL_HORSE" -> {
                    hasTravelHorse = true;
                    movingWithTravelHorse = movingWithTravelHorse || onMoving;
                }
                case "PEACH_BLOSSOM" -> {
                    hasPeachBlossom = true;
                    useGodWithPeachBlossom = useGodWithPeachBlossom || onUseGod;
                }
                case "HUA_GAI" -> hasHuaGai = true;
                case "WEN_CHANG" -> {
                    hasWenChang = true;
                    useGodWithWenChang = useGodWithWenChang || onUseGod;
                }
                case "GENERAL_STAR" -> {
                    hasGeneralStar = true;
                    useGodWithGeneralStar = useGodWithGeneralStar || onUseGod;
                }
                case "JIE_SHA" -> {
                    hasJieSha = true;
                    movingWithJieSha = movingWithJieSha || onMoving;
                }
                case "DISASTER_SHA" -> {
                    hasDisasterSha = true;
                    movingWithDisasterSha = movingWithDisasterSha || onMoving;
                }
                default -> {
                }
            }
        }

        Map<String, Object> evidence = new LinkedHashMap<>(UseGodLineLocator.baseChartEvidence(chart, UseGodLineLocator.extractUseGod(chart)));
        evidence.put("hitCount", matches.size());
        evidence.put("matches", matches);
        evidence.put("hasNobleman", hasNobleman);
        evidence.put("useGodWithNobleman", useGodWithNobleman);
        evidence.put("hasTravelHorse", hasTravelHorse);
        evidence.put("movingWithTravelHorse", movingWithTravelHorse);
        evidence.put("hasPeachBlossom", hasPeachBlossom);
        evidence.put("useGodWithPeachBlossom", useGodWithPeachBlossom);
        evidence.put("hasHuaGai", hasHuaGai);
        evidence.put("hasWenChang", hasWenChang);
        evidence.put("useGodWithWenChang", useGodWithWenChang);
        evidence.put("hasGeneralStar", hasGeneralStar);
        evidence.put("useGodWithGeneralStar", useGodWithGeneralStar);
        evidence.put("hasJieSha", hasJieSha);
        evidence.put("movingWithJieSha", movingWithJieSha);
        evidence.put("hasDisasterSha", hasDisasterSha);
        evidence.put("movingWithDisasterSha", movingWithDisasterSha);

        hit.setHit(true);
        hit.setImpactLevel("LOW");
        hit.setHitReason(resolveReason(
                hasNobleman,
                hasTravelHorse,
                hasPeachBlossom,
                hasHuaGai,
                hasWenChang,
                hasGeneralStar,
                hasJieSha,
                hasDisasterSha
        ));
        hit.setEvidence(evidence);
        return hit;
    }

    private Integer resolveUseGodLineIndex(ChartSnapshot chart) {
        if (chart == null) {
            return null;
        }
        if (chart.getExt() != null && chart.getExt().get("useGodLineIndex") instanceof Number number) {
            return number.intValue();
        }
        String useGod = UseGodLineLocator.extractUseGod(chart);
        return UseGodLineLocator.findUseGodLines(chart, useGod).stream()
                .map(LineInfo::getIndex)
                .findFirst()
                .orElse(null);
    }

    private LineInfo findLine(ChartSnapshot chart, Integer lineIndex) {
        if (chart == null || chart.getLines() == null || lineIndex == null) {
            return null;
        }
        return chart.getLines().stream()
                .filter(line -> lineIndex.equals(line.getIndex()))
                .findFirst()
                .orElse(null);
    }

    private String resolveReason(boolean hasNobleman,
                                 boolean hasTravelHorse,
                                 boolean hasPeachBlossom,
                                 boolean hasHuaGai,
                                 boolean hasWenChang,
                                 boolean hasGeneralStar,
                                 boolean hasJieSha,
                                 boolean hasDisasterSha) {
        List<String> names = new ArrayList<>();
        if (hasNobleman) {
            names.add("贵人");
        }
        if (hasTravelHorse) {
            names.add("驿马");
        }
        if (hasPeachBlossom) {
            names.add("桃花");
        }
        if (hasHuaGai) {
            names.add("华盖");
        }
        if (hasWenChang) {
            names.add("文昌");
        }
        if (hasGeneralStar) {
            names.add("将星");
        }
        if (hasJieSha) {
            names.add("劫煞");
        }
        if (hasDisasterSha) {
            names.add("灾煞");
        }
        return names.isEmpty() ? "当前盘面未见首批神煞命中。" : "盘面出现" + String.join("、", names) + "等神煞信号。";
    }
}
