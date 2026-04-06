package com.yishou.liuyao.rule.usegod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseGodRuleTest {

    @Test
    void shouldSelectQiCaiForIncomeQuestion() {
        UseGodRule rule = new UseGodRule(
                new UseGodSelector(
                        new QuestionIntentResolver(),
                        new UseGodRuleConfigLoader(new ObjectMapper())
                )
        );

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestion("我下个月工资会不会上涨");
        chartSnapshot.setQuestionCategory("收入");

        RuleHit hit = rule.evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("妻财", hit.getEvidence().get("useGod"));
    }
}
