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
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.advanced.UseGodMonthBreakRule;
import com.yishou.liuyao.rule.basic.MovingLineExistsRule;
import com.yishou.liuyao.rule.batch.MovingLineAffectUseGodRule;
import com.yishou.liuyao.rule.batch.UseGodStrengthRule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
