package com.yishou.liuyao.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.yishou.liuyao.rule.Rule;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.advanced.ShiYingRelationRule;
import com.yishou.liuyao.rule.basic.MovingLineExistsRule;
import com.yishou.liuyao.rule.usegod.QuestionIntentResolver;
import com.yishou.liuyao.rule.usegod.UseGodRule;
import com.yishou.liuyao.rule.usegod.UseGodRuleConfigLoader;
import com.yishou.liuyao.rule.usegod.UseGodSelector;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineScenarioRegressionTest {

    private final ChartBuilderService chartBuilderService = new ChartBuilderService(
            new CalendarFacade(),
            new HexagramResolver(),
            new ShiYingResolver(),
            new LiuShenResolver(),
            new PalaceResolver(),
            new NaJiaResolver(),
            new LiuQinResolver()
    );

    private final RuleEngineService ruleEngineService = new RuleEngineService(List.of(
            buildUseGodRule(),
            new MovingLineExistsRule(),
            new ShiYingRelationRule()
    ));

    @ParameterizedTest
    @MethodSource("cases")
    void shouldKeepScenarioRuleChainStable(String question,
                                           String category,
                                           String expectedUseGod,
                                           boolean shouldHitUseGodSelection) {
        DivinationInput input = new DivinationInput();
        input.setQuestion(question);
        input.setQuestionCategory(category);
        input.setDivinationTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        input.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        input.setMovingLines(List.of(1, 5));

        ChartSnapshot chartSnapshot = chartBuilderService.buildChart(input);
        List<RuleHit> hits = ruleEngineService.evaluate(chartSnapshot);

        assertEquals(expectedUseGod, chartSnapshot.getUseGod());
        assertEquals(shouldHitUseGodSelection, hits.stream().anyMatch(hit -> "USE_GOD_SELECTION".equals(hit.getRuleCode())));
        assertTrue(hits.stream().anyMatch(hit -> "MOVING_LINE_EXISTS".equals(hit.getRuleCode())));
        assertTrue(hits.stream().anyMatch(hit -> "SHI_YING_RELATION".equals(hit.getRuleCode())));
    }

    private static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of("我下个月工资会不会上涨", "收入", "妻财", true),
                Arguments.of("我和她能不能结婚", "感情", "应爻", true),
                Arguments.of("这次求职能不能拿到 offer", "工作", "官鬼", true),
                Arguments.of("最近身体恢复得怎么样", "健康", "官鬼", true),
                Arguments.of("这次合作签约能不能顺利推进", "合作", "应爻", true),
                Arguments.of("这次出行会不会顺利", "出行", "父母", true)
        );
    }

    private static Rule buildUseGodRule() {
        UseGodRuleConfigLoader loader = new UseGodRuleConfigLoader(new ObjectMapper());
        return new UseGodRule(new UseGodSelector(new QuestionIntentResolver(), loader));
    }
}
