package com.yishou.liuyao.rule.usegod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class UseGodFallbackTest {

    @Test
    void shouldNotAutoSelectUseGodWhenIntentUnknown() {
        UseGodRule rule = new UseGodRule(new UseGodSelector(
                new QuestionIntentResolver(),
                new UseGodRuleConfigLoader(new ObjectMapper())
        ));

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestion("今天整体感觉如何");
        chartSnapshot.setQuestionCategory("闲聊");

        RuleHit hit = rule.evaluate(chartSnapshot);

        assertFalse(Boolean.TRUE.equals(hit.getHit()));
        assertNull(chartSnapshot.getUseGod());
        assertEquals("UNKNOWN", hit.getEvidence().get("intent"));
    }
}
