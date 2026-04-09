package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.service.CalendarFacade;
import com.yishou.liuyao.divination.service.ChartBuilderService;
import com.yishou.liuyao.divination.service.HexagramResolver;
import com.yishou.liuyao.divination.service.LiuQinResolver;
import com.yishou.liuyao.divination.service.LiuShenResolver;
import com.yishou.liuyao.divination.service.NaJiaResolver;
import com.yishou.liuyao.divination.service.PalaceResolver;
import com.yishou.liuyao.divination.service.ShiYingResolver;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.advanced.UseGodMonthBreakRule;
import com.yishou.liuyao.rule.basic.MovingLineExistsRule;
import com.yishou.liuyao.rule.batch.MovingLineAffectUseGodRule;
import com.yishou.liuyao.rule.batch.UseGodDayBreakRule;
import com.yishou.liuyao.rule.batch.UseGodStrengthRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineRealChartRegressionTest {

    @Test
    void shouldEvaluateRulesAgainstBuiltChartSnapshot() {
        ChartBuilderService chartBuilderService = new ChartBuilderService(
                new CalendarFacade(),
                new HexagramResolver(),
                new ShiYingResolver(),
                new LiuShenResolver(),
                new PalaceResolver(),
                new NaJiaResolver(),
                new LiuQinResolver()
        );

        DivinationInput input = new DivinationInput();
        input.setQuestion("财运如何");
        input.setQuestionCategory("财富");
        input.setDivinationTime(LocalDateTime.of(1986, 5, 29, 0, 0));
        input.setRawLines(List.of("老阳", "少阳", "少阳", "少阳", "少阳", "少阳"));

        ChartSnapshot chartSnapshot = chartBuilderService.buildChart(input);
        chartSnapshot.setExt(new LinkedHashMap<>(chartSnapshot.getExt()));
        chartSnapshot.getExt().put("useGod", "妻财");

        RuleEngineService ruleEngineService = new RuleEngineService(List.of(
                new MovingLineExistsRule(),
                new UseGodStrengthRule(),
                new MovingLineAffectUseGodRule(),
                new UseGodMonthBreakRule()
        ));

        List<RuleHit> hits = ruleEngineService.evaluate(chartSnapshot);

        assertEquals(3, hits.size());
        assertTrue(hits.stream().anyMatch(hit -> "MOVING_LINE_EXISTS".equals(hit.getRuleCode())));
        RuleHit strengthHit = hits.stream().filter(hit -> "USE_GOD_STRENGTH".equals(hit.getRuleCode())).findFirst().orElseThrow();
        assertTrue(hits.stream().anyMatch(hit -> "MOVING_LINE_AFFECT_USE_GOD".equals(hit.getRuleCode())));
        assertEquals(1, strengthHit.getEvidence().get("targetCount"));
        assertNotNull(strengthHit.getEvidence().get("targetSummary"));
    }

    @Test
    void shouldBuildStructuredRuleEvaluationSummary() {
        ChartBuilderService chartBuilderService = new ChartBuilderService(
                new CalendarFacade(),
                new HexagramResolver(),
                new ShiYingResolver(),
                new LiuShenResolver(),
                new PalaceResolver(),
                new NaJiaResolver(),
                new LiuQinResolver()
        );

        DivinationInput input = new DivinationInput();
        input.setQuestion("我下个月工资会不会上涨");
        input.setQuestionCategory("收入");
        input.setDivinationTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        input.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        input.setMovingLines(List.of(1, 5));

        ChartSnapshot chartSnapshot = chartBuilderService.buildChart(input);
        chartSnapshot.setUseGod("妻财");
        chartSnapshot.setExt(new LinkedHashMap<>(chartSnapshot.getExt()));
        chartSnapshot.getExt().put("useGod", "妻财");

        RuleEngineService ruleEngineService = new RuleEngineService(List.of(
                new MovingLineAffectUseGodRule(),
                new UseGodMonthBreakRule(),
                new UseGodStrengthRule(),
                new MovingLineExistsRule()
        ), new RuleDefinitionConfigLoader(new ObjectMapper()), new RuleMatcher(), new RuleReasoningService());

        RuleEvaluationResult evaluationResult = ruleEngineService.evaluateResult(chartSnapshot);
        int expectedScore = evaluationResult.getHits().stream()
                .map(RuleHit::getScoreDelta)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        assertTrue(evaluationResult.getHits().stream().map(RuleHit::getRuleCode).toList()
                .containsAll(List.of("MOVING_LINE_EXISTS", "USE_GOD_STRENGTH", "MOVING_LINE_AFFECT_USE_GOD")));
        assertTrue(evaluationResult.getHits().stream().allMatch(hit -> hit.getPriority() != null));
        assertTrue(evaluationResult.getHits().stream().map(RuleHit::getRuleId).toList()
                .containsAll(List.of("R006", "R099", "R017")));
        assertEquals(expectedScore, evaluationResult.getScore());
        assertNotNull(evaluationResult.getResultLevel());
        assertNotNull(evaluationResult.getSummary());
        assertFalse(evaluationResult.getTags().isEmpty());
        assertTrue(evaluationResult.getTags().stream().anyMatch(tag ->
                "用神有力".equals(tag) || "用神中平".equals(tag) || "用神偏弱".equals(tag)));
    }

    @Test
    void shouldApplyConfiguredCompositeRuleAfterBuiltInRules() {
        ChartBuilderService chartBuilderService = new ChartBuilderService(
                new CalendarFacade(),
                new HexagramResolver(),
                new ShiYingResolver(),
                new LiuShenResolver(),
                new PalaceResolver(),
                new NaJiaResolver(),
                new LiuQinResolver()
        );

        DivinationInput input = new DivinationInput();
        input.setQuestion("最近局势会不会太乱");
        input.setQuestionCategory("测试");
        input.setDivinationTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        input.setRawLines(List.of("老阳", "老阴", "老阳", "老阴", "少阳", "少阴"));
        input.setMovingLines(List.of(1, 2, 3, 4));

        ChartSnapshot chartSnapshot = chartBuilderService.buildChart(input);
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());
        RuleEngineService ruleEngineService = new RuleEngineService(List.of(
                new MovingLineExistsRule()
        ), loader, new RuleMatcher(), new RuleReasoningService());

        RuleEvaluationResult evaluationResult = ruleEngineService.evaluateResult(chartSnapshot);

        assertTrue(evaluationResult.getHits().stream().anyMatch(hit -> "R016".equals(hit.getRuleCode())));
        assertTrue(evaluationResult.getTags().contains("局势混乱"));
    }

    @Test
    void shouldApplyConfiguredShiStateRules() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());
        RuleEngineService ruleEngineService = new RuleEngineService(List.of(), loader, new RuleMatcher(), new RuleReasoningService());

        ChartSnapshot strongChart = new ChartSnapshot();
        strongChart.setShi(1);
        strongChart.setLines(new ArrayList<>(List.of(buildShiLine(1, true, "木", "木"))));

        RuleEvaluationResult strongResult = ruleEngineService.evaluateResult(strongChart);
        assertTrue(strongResult.getHits().stream().anyMatch(hit -> "R007".equals(hit.getRuleId())));
        assertTrue(strongResult.getHits().stream().anyMatch(hit -> "R010".equals(hit.getRuleId())));
        assertTrue(strongResult.getTags().contains("自身有力"));
        assertTrue(strongResult.getTags().contains("主动变化"));

        ChartSnapshot weakChart = new ChartSnapshot();
        weakChart.setShi(1);
        weakChart.setLines(new ArrayList<>(List.of(buildShiLine(1, false, "木", "火"))));

        RuleEvaluationResult weakResult = ruleEngineService.evaluateResult(weakChart);
        assertTrue(weakResult.getHits().stream().anyMatch(hit -> "R008".equals(hit.getRuleId())));
        assertTrue(weakResult.getTags().contains("自身不足"));

        ChartSnapshot emptyChart = new ChartSnapshot();
        emptyChart.setShi(1);
        emptyChart.setKongWang(List.of("卯"));
        emptyChart.setLines(new ArrayList<>(List.of(buildShiLine(1, false, "木", "火", "卯"))));

        RuleEvaluationResult emptyResult = ruleEngineService.evaluateResult(emptyChart);
        assertTrue(emptyResult.getHits().stream().anyMatch(hit -> "R011".equals(hit.getRuleId())));
        assertTrue(emptyResult.getTags().contains("心虚"));
    }

    @Test
    void shouldApplyConfiguredUseGodShiRules() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());
        RuleEngineService ruleEngineService = new RuleEngineService(List.of(), loader, new RuleMatcher(), new RuleReasoningService());

        ChartSnapshot shengShiChart = new ChartSnapshot();
        shengShiChart.setShi(1);
        shengShiChart.setUseGod("妻财");
        shengShiChart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "水", "火", "子"),
                buildUseGodLine(2, false, "金", "金", "申")
        )));
        RuleEvaluationResult shengShiResult = ruleEngineService.evaluateResult(shengShiChart);
        assertTrue(shengShiResult.getHits().stream().anyMatch(hit -> "R003".equals(hit.getRuleId())));
        assertTrue(shengShiResult.getTags().contains("有利"));

        ChartSnapshot heShiChart = new ChartSnapshot();
        heShiChart.setShi(1);
        heShiChart.setUseGod("妻财");
        heShiChart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "水", "火", "子"),
                buildUseGodLine(2, false, "土", "土", "丑")
        )));
        RuleEvaluationResult heShiResult = ruleEngineService.evaluateResult(heShiChart);
        assertTrue(heShiResult.getHits().stream().anyMatch(hit -> "R004".equals(hit.getRuleId())));
        assertTrue(heShiResult.getTags().contains("有接近"));

        ChartSnapshot retreatChart = new ChartSnapshot();
        retreatChart.setShi(1);
        retreatChart.setUseGod("妻财");
        retreatChart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "水", "火", "子"),
                buildUseGodLine(2, true, "水", "火", "丑", "父母")
        )));
        RuleEvaluationResult retreatResult = ruleEngineService.evaluateResult(retreatChart);
        assertTrue(retreatResult.getHits().stream().anyMatch(hit -> "R018".equals(hit.getRuleId())));
        assertTrue(retreatResult.getTags().contains("后劲不足"));
    }

    @Test
    void shouldApplyConfiguredSelectedUseGodFactRules() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());
        RuleEngineService ruleEngineService = new RuleEngineService(List.of(), loader, new RuleMatcher(), new RuleReasoningService());

        ChartSnapshot chart = new ChartSnapshot();
        chart.setShi(3);
        chart.setUseGod("官鬼");
        chart.setKongWang(List.of("戌"));
        chart.setExt(new LinkedHashMap<>());
        chart.getExt().put("useGod", "官鬼");
        chart.getExt().put("useGodLineIndex", 4);
        chart.setLines(new ArrayList<>(List.of(
                buildLine(2, "官鬼", false, "金", "金", "申"),
                buildShiLine(3, false, "木", "木", "寅"),
                buildLine(4, "官鬼", true, "水", "水", "子"),
                buildYingLine(6, false, "火", "火", "午")
        )));

        RuleEvaluationResult result = ruleEngineService.evaluateResult(chart);

        assertTrue(result.getHits().stream().anyMatch(hit -> "R021".equals(hit.getRuleId())));
        assertTrue(result.getHits().stream().anyMatch(hit -> "R022".equals(hit.getRuleId())));
        assertTrue(result.getHits().stream().anyMatch(hit -> "R023".equals(hit.getRuleId())));
        assertTrue(result.getTags().contains("主象已动"));
        assertTrue(result.getTags().contains("贴近日身"));
        assertTrue(result.getTags().contains("多重候选"));
    }

    @Test
    void shouldApplyConfiguredBreakAndShiYingDistanceRules() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());
        RuleEngineService ruleEngineService = new RuleEngineService(List.of(
                new UseGodMonthBreakRule(),
                new UseGodDayBreakRule()
        ), loader, new RuleMatcher(), new RuleReasoningService());

        ChartSnapshot chart = new ChartSnapshot();
        chart.setShi(1);
        chart.setYing(4);
        chart.setUseGod("官鬼");
        chart.setYueJian("午");
        chart.setRiChen("午");
        chart.setExt(new LinkedHashMap<>());
        chart.getExt().put("useGod", "官鬼");
        chart.getExt().put("useGodLineIndex", 6);
        chart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "木", "木", "寅"),
                buildYingLine(4, false, "火", "火", "午"),
                buildLine(6, "官鬼", false, "水", "水", "子")
        )));

        RuleEvaluationResult result = ruleEngineService.evaluateResult(chart);

        assertTrue(result.getHits().stream().anyMatch(hit -> "R025".equals(hit.getRuleId())));
        assertTrue(result.getHits().stream().anyMatch(hit -> "R026".equals(hit.getRuleId())));
        assertTrue(result.getHits().stream().anyMatch(hit -> "R027".equals(hit.getRuleId())));
        assertTrue(result.getTags().contains("月建受冲"));
        assertTrue(result.getTags().contains("日辰受冲"));
        assertTrue(result.getTags().contains("彼此有距"));
    }

    @Test
    void shouldApplyConfiguredMovingChongShiAndUseGodRules() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());
        RuleEngineService ruleEngineService = new RuleEngineService(List.of(), loader, new RuleMatcher(), new RuleReasoningService());

        ChartSnapshot chart = new ChartSnapshot();
        chart.setShi(1);
        chart.setYing(4);
        chart.setUseGod("官鬼");
        chart.setExt(new LinkedHashMap<>());
        chart.getExt().put("useGod", "官鬼");
        chart.getExt().put("useGodLineIndex", 6);
        chart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "木", "木", "子"),
                buildYingLine(4, false, "火", "火", "午"),
                buildLine(3, "兄弟", true, "土", "土", "午"),
                buildLine(6, "官鬼", false, "水", "水", "子")
        )));

        RuleEvaluationResult result = ruleEngineService.evaluateResult(chart);

        assertTrue(result.getHits().stream().anyMatch(hit -> "R028".equals(hit.getRuleId())));
        assertTrue(result.getHits().stream().anyMatch(hit -> "R029".equals(hit.getRuleId())));
        assertTrue(result.getTags().contains("动冲世爻"));
        assertTrue(result.getTags().contains("动冲用神"));
    }

    @Test
    void shouldApplyConfiguredUseGodRuMuRule() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());
        RuleEngineService ruleEngineService = new RuleEngineService(List.of(), loader, new RuleMatcher(), new RuleReasoningService());

        ChartSnapshot chart = new ChartSnapshot();
        chart.setShi(1);
        chart.setUseGod("官鬼");
        chart.setExt(new LinkedHashMap<>());
        chart.getExt().put("useGod", "官鬼");
        chart.getExt().put("useGodLineIndex", 4);
        chart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "木", "木", "寅"),
                buildLine(4, "官鬼", false, "水", "水", "辰"),
                buildYingLine(6, false, "火", "火", "午")
        )));

        RuleEvaluationResult result = ruleEngineService.evaluateResult(chart);

        assertTrue(result.getHits().stream().anyMatch(hit -> "R030".equals(hit.getRuleId())));
        assertTrue(result.getTags().contains("用神入墓"));
    }

    @Test
    void shouldApplyConfiguredUseGodChongKaiAndChongSanRules() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());
        RuleEngineService ruleEngineService = new RuleEngineService(List.of(
                new UseGodDayBreakRule()
        ), loader, new RuleMatcher(), new RuleReasoningService());

        ChartSnapshot chongKaiChart = new ChartSnapshot();
        chongKaiChart.setShi(1);
        chongKaiChart.setUseGod("官鬼");
        chongKaiChart.setKongWang(List.of("子"));
        chongKaiChart.setRiChen("午");
        chongKaiChart.setExt(new LinkedHashMap<>());
        chongKaiChart.getExt().put("useGod", "官鬼");
        chongKaiChart.getExt().put("useGodLineIndex", 4);
        chongKaiChart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "木", "木", "寅"),
                buildLine(4, "官鬼", false, "水", "水", "子"),
                buildYingLine(6, false, "火", "火", "午")
        )));
        RuleEvaluationResult chongKaiResult = ruleEngineService.evaluateResult(chongKaiChart);
        assertTrue(chongKaiResult.getHits().stream().anyMatch(hit -> "R031".equals(hit.getRuleId())));
        assertTrue(chongKaiResult.getTags().contains("冲开束缚"));

        ChartSnapshot chongSanChart = new ChartSnapshot();
        chongSanChart.setShi(1);
        chongSanChart.setUseGod("官鬼");
        chongSanChart.setRiChen("午");
        chongSanChart.setExt(new LinkedHashMap<>());
        chongSanChart.getExt().put("useGod", "官鬼");
        chongSanChart.getExt().put("useGodLineIndex", 4);
        chongSanChart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "木", "木", "寅"),
                buildLine(4, "官鬼", false, "水", "水", "子"),
                buildYingLine(6, false, "火", "火", "午")
        )));
        RuleEvaluationResult chongSanResult = ruleEngineService.evaluateResult(chongSanChart);
        assertTrue(chongSanResult.getHits().stream().anyMatch(hit -> "R032".equals(hit.getRuleId())));
        assertTrue(chongSanResult.getTags().contains("受冲易散"));
    }

    @Test
    void shouldApplyConfiguredShiYingRelationRules() {
        RuleDefinitionConfigLoader loader = new RuleDefinitionConfigLoader(new ObjectMapper());
        RuleEngineService ruleEngineService = new RuleEngineService(List.of(new com.yishou.liuyao.rule.advanced.ShiYingRelationRule()),
                loader, new RuleMatcher(), new RuleReasoningService());

        ChartSnapshot yingShengShiChart = new ChartSnapshot();
        yingShengShiChart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "土", "火", "辰"),
                buildYingLine(4, false, "火", "火", "午")
        )));
        RuleEvaluationResult yingShengShiResult = ruleEngineService.evaluateResult(yingShengShiChart);
        assertTrue(yingShengShiResult.getHits().stream().anyMatch(hit -> "R014".equals(hit.getRuleId())));
        assertTrue(yingShengShiResult.getTags().contains("外部助力"));

        ChartSnapshot yingKeShiChart = new ChartSnapshot();
        yingKeShiChart.setUseGod("妻财");
        yingKeShiChart.setLines(new ArrayList<>(List.of(
                buildShiLine(1, false, "木", "火", "卯"),
                buildYingLine(4, false, "金", "金", "酉"),
                buildUseGodLine(5, false, "金", "金", "申")
        )));
        RuleEvaluationResult yingKeShiResult = ruleEngineService.evaluateResult(yingKeShiChart);
        assertTrue(yingKeShiResult.getHits().stream().anyMatch(hit -> "R009".equals(hit.getRuleId())));
        assertTrue(yingKeShiResult.getHits().stream().anyMatch(hit -> "R013".equals(hit.getRuleId())));
        assertTrue(yingKeShiResult.getHits().stream().anyMatch(hit -> "R015".equals(hit.getRuleId())));
        assertTrue(yingKeShiResult.getTags().contains("压力"));
        assertTrue(yingKeShiResult.getTags().contains("对立"));
        assertTrue(yingKeShiResult.getTags().contains("外部压制"));
    }

    private LineInfo buildShiLine(int index, boolean moving, String wuXing, String changeWuXing) {
        return buildShiLine(index, moving, wuXing, changeWuXing, null);
    }

    private LineInfo buildShiLine(int index, boolean moving, String wuXing, String changeWuXing, String branch) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setShi(true);
        line.setMoving(moving);
        line.setWuXing(wuXing);
        line.setChangeWuXing(changeWuXing);
        line.setBranch(branch);
        return line;
    }

    private LineInfo buildUseGodLine(int index, boolean moving, String wuXing, String changeWuXing, String branch) {
        return buildUseGodLine(index, moving, wuXing, changeWuXing, branch, null);
    }

    private LineInfo buildUseGodLine(int index, boolean moving, String wuXing, String changeWuXing, String branch, String changeLiuQin) {
        return buildLine(index, "妻财", moving, wuXing, changeWuXing, branch, changeLiuQin);
    }

    private LineInfo buildLine(int index, String liuQin, boolean moving, String wuXing, String changeWuXing, String branch) {
        return buildLine(index, liuQin, moving, wuXing, changeWuXing, branch, null);
    }

    private LineInfo buildLine(int index, String liuQin, boolean moving, String wuXing, String changeWuXing, String branch, String changeLiuQin) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setLiuQin(liuQin);
        line.setMoving(moving);
        line.setWuXing(wuXing);
        line.setChangeWuXing(changeWuXing);
        line.setBranch(branch);
        line.setChangeLiuQin(changeLiuQin);
        return line;
    }

    private LineInfo buildYingLine(int index, boolean moving, String wuXing, String changeWuXing, String branch) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setYing(true);
        line.setMoving(moving);
        line.setWuXing(wuXing);
        line.setChangeWuXing(changeWuXing);
        line.setBranch(branch);
        return line;
    }
}
