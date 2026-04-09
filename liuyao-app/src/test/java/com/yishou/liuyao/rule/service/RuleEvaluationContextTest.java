package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEvaluationContextTest {

    @Test
    void shouldUseSelectedUseGodLineIndexFromChartExt() {
        ChartSnapshot chartSnapshot = buildChart();
        chartSnapshot.getExt().put("useGod", "官鬼");
        chartSnapshot.getExt().put("useGodLineIndex", 4);

        RuleEvaluationContext context = RuleEvaluationContext.from(chartSnapshot, List.of());

        assertEquals(4, context.getUseGodLineIndex());
        assertEquals(2, context.getUseGodLineCount());
        assertEquals(1, context.getUseGodDistanceToShi());
        assertTrue(Boolean.TRUE.equals(context.getUseGodMoving()));
        assertFalse(Boolean.TRUE.equals(context.getUseGodEmpty()));
    }

    @Test
    void shouldResolveRelationAgainstSelectedUseGodLineInsteadOfFirstMatch() {
        ChartSnapshot chartSnapshot = buildChart();
        chartSnapshot.getExt().put("useGod", "官鬼");
        chartSnapshot.getExt().put("useGodLineIndex", 4);

        RuleEvaluationContext context = RuleEvaluationContext.from(chartSnapshot, List.of(
                hit("USE_GOD_STRENGTH", Map.of("bestLevel", "STRONG", "bestScore", 8))
        ));

        assertEquals("生", context.getUseGodToShiRelation());
        assertEquals("STRONG", context.getYongshenState());
        assertEquals(8, context.getUseGodBestScore());
    }

    @Test
    void shouldDeriveUseGodBreakAndShiYingFacts() {
        ChartSnapshot chartSnapshot = buildChart();
        chartSnapshot.setRiChen("午");
        chartSnapshot.setYueJian("午");
        chartSnapshot.getExt().put("useGod", "官鬼");
        chartSnapshot.getExt().put("useGodLineIndex", 4);

        RuleEvaluationContext context = RuleEvaluationContext.from(chartSnapshot, List.of(
                hit("USE_GOD_MONTH_BREAK", Map.of("yueBranch", "午")),
                hit("USE_GOD_DAY_BREAK", Map.of("riBranch", "午"))
        ));

        assertTrue(Boolean.TRUE.equals(context.getUseGodMonthBreak()));
        assertTrue(Boolean.TRUE.equals(context.getUseGodDayBreak()));
        assertTrue(Boolean.TRUE.equals(context.getShiYingExists()));
        assertEquals(3, context.getShiYingDistance());
    }

    @Test
    void shouldDeriveMovingLineAffectFactsAgainstSelectedUseGodAndShi() {
        ChartSnapshot chartSnapshot = buildChart();
        chartSnapshot.getExt().put("useGod", "官鬼");
        chartSnapshot.getExt().put("useGodLineIndex", 4);
        chartSnapshot.setLines(new java.util.ArrayList<>(List.of(
                line(2, "兄弟", true, false, false, "午"),
                line(3, "兄弟", false, true, false, "寅"),
                line(4, "官鬼", true, false, false, "子"),
                line(6, "父母", false, false, true, "午")
        )));

        RuleEvaluationContext context = RuleEvaluationContext.from(chartSnapshot, List.of(
                hit("MOVING_LINE_AFFECT_USE_GOD", Map.of(
                        "effects", List.of(
                                Map.of("relation", "动爻生用神", "changeRelation", "变爻克用神"),
                                Map.of("relation", "动爻冲用神")
                        )
                ))
        ));

        assertTrue(Boolean.TRUE.equals(context.getHasMovingShengUseGod()));
        assertTrue(Boolean.TRUE.equals(context.getHasChangedKeUseGod()));
        assertTrue(Boolean.TRUE.equals(context.getHasMovingChongUseGod()));
        assertFalse(Boolean.TRUE.equals(context.getHasMovingChongShi()));
    }

    @Test
    void shouldDeriveUseGodRuMuFactFromSelectedLine() {
        ChartSnapshot chartSnapshot = buildChart();
        chartSnapshot.getExt().put("useGod", "官鬼");
        chartSnapshot.getExt().put("useGodLineIndex", 4);
        chartSnapshot.setLines(new java.util.ArrayList<>(List.of(
                line(2, "官鬼", false, false, false, "申"),
                line(3, "兄弟", false, true, false, "寅"),
                line(4, "官鬼", true, false, false, "辰"),
                line(6, "父母", false, false, true, "午")
        )));

        RuleEvaluationContext context = RuleEvaluationContext.from(chartSnapshot, List.of());

        assertTrue(Boolean.TRUE.equals(context.getUseGodRuMu()));
    }

    @Test
    void shouldDeriveUseGodChongKaiWhenEmptyAndBroken() {
        ChartSnapshot chartSnapshot = buildChart();
        chartSnapshot.setKongWang(List.of("子"));
        chartSnapshot.getExt().put("useGod", "官鬼");
        chartSnapshot.getExt().put("useGodLineIndex", 4);

        RuleEvaluationContext context = RuleEvaluationContext.from(chartSnapshot, List.of(
                hit("USE_GOD_DAY_BREAK", Map.of("riBranch", "午"))
        ));

        assertTrue(Boolean.TRUE.equals(context.getUseGodChongKai()));
        assertFalse(Boolean.TRUE.equals(context.getUseGodChongSan()));
    }

    @Test
    void shouldDeriveUseGodChongSanWhenStableLineGetsBroken() {
        ChartSnapshot chartSnapshot = buildChart();
        chartSnapshot.getExt().put("useGod", "官鬼");
        chartSnapshot.getExt().put("useGodLineIndex", 4);

        RuleEvaluationContext context = RuleEvaluationContext.from(chartSnapshot, List.of(
                hit("USE_GOD_DAY_BREAK", Map.of("riBranch", "午"))
        ));

        assertFalse(Boolean.TRUE.equals(context.getUseGodChongKai()));
        assertTrue(Boolean.TRUE.equals(context.getUseGodChongSan()));
    }

    private ChartSnapshot buildChart() {
        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestionCategory("求职");
        chartSnapshot.setShi(3);
        chartSnapshot.setYing(6);
        chartSnapshot.setKongWang(List.of("戌"));
        chartSnapshot.setLines(List.of(
                line(2, "官鬼", false, false, false, "申"),
                line(3, "兄弟", false, true, false, "寅"),
                line(4, "官鬼", true, false, false, "子"),
                line(6, "父母", false, false, true, "午")
        ));
        chartSnapshot.setExt(new LinkedHashMap<>());
        return chartSnapshot;
    }

    private RuleHit hit(String ruleCode, Map<String, Object> evidence) {
        RuleHit hit = new RuleHit();
        hit.setRuleCode(ruleCode);
        hit.setHit(true);
        hit.setEvidence(evidence);
        return hit;
    }

    private LineInfo line(int index, String liuQin, boolean moving, boolean shi, boolean ying, String branch) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setLiuQin(liuQin);
        line.setMoving(moving);
        line.setShi(shi);
        line.setYing(ying);
        line.setBranch(branch);
        line.setWuXing(switch (branch) {
            case "申" -> "金";
            case "寅" -> "木";
            case "子" -> "水";
            case "午" -> "火";
            default -> "土";
        });
        return line;
    }
}
