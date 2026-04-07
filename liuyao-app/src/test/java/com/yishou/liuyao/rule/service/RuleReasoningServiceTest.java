package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleReasoningServiceTest {

    @Test
    void shouldSummarizeConflictsWithinSameCategory() {
        RuleHit positiveHit = new RuleHit();
        positiveHit.setRuleCode("R003");
        positiveHit.setRuleName("用神生世");
        positiveHit.setCategory("YONGSHEN_STATE");
        positiveHit.setScoreDelta(2);
        positiveHit.setTags(List.of("有利"));

        RuleHit negativeHit = new RuleHit();
        negativeHit.setRuleCode("R005");
        negativeHit.setRuleName("用神空亡");
        negativeHit.setCategory("YONGSHEN_STATE");
        negativeHit.setScoreDelta(-2);
        negativeHit.setTags(List.of("落空"));

        RuleEvaluationResult result = new RuleReasoningService().summarize(null, List.of(positiveHit, negativeHit));

        assertEquals(0, result.getScore());
        assertEquals(1, result.getConflictSummaries().size());
        assertEquals("YONGSHEN_STATE", result.getConflictSummaries().get(0).get("category"));
        assertEquals(1, result.getConflictSummaries().get(0).get("positiveCount"));
        assertEquals(1, result.getConflictSummaries().get(0).get("negativeCount"));
        assertEquals("MIXED", result.getConflictSummaries().get(0).get("decision"));
        assertEquals(0, result.getConflictSummaries().get(0).get("netScore"));
        assertEquals(List.of(), result.getConflictSummaries().get(0).get("suppressedRules"));
        assertEquals(0, result.getEffectiveScore());
        assertEquals(List.of(), result.getEffectiveRuleCodes());
        assertEquals(List.of(), result.getSuppressedRuleCodes());
        assertEquals(2, result.getCategorySummaries().get(0).get("hitCount"));
        assertEquals(0, result.getCategorySummaries().get(0).get("effectiveHitCount"));
        assertEquals(0, result.getCategorySummaries().get(0).get("effectiveScore"));
        assertFalse(result.getCategorySummaries().isEmpty());
        assertTrue(result.getSummary().contains("冲突"));
    }

    @Test
    void shouldMarkPositiveDominanceWhenPositiveSignalsOutweighNegativeSignals() {
        RuleHit positiveHit = new RuleHit();
        positiveHit.setRuleCode("R003");
        positiveHit.setRuleName("用神生世");
        positiveHit.setCategory("YONGSHEN_STATE");
        positiveHit.setScoreDelta(3);

        RuleHit negativeHit = new RuleHit();
        negativeHit.setRuleCode("R005");
        negativeHit.setRuleName("用神空亡");
        negativeHit.setCategory("YONGSHEN_STATE");
        negativeHit.setScoreDelta(-1);

        RuleEvaluationResult result = new RuleReasoningService().summarize(null, List.of(positiveHit, negativeHit));

        assertEquals(1, result.getConflictSummaries().size());
        assertEquals("POSITIVE_DOMINANT", result.getConflictSummaries().get(0).get("decision"));
        assertEquals(2, result.getConflictSummaries().get(0).get("netScore"));
        assertEquals(List.of("R005"), result.getConflictSummaries().get(0).get("suppressedRules"));
        assertEquals(List.of("R003"), result.getConflictSummaries().get(0).get("effectiveRules"));
        assertEquals(3, result.getEffectiveScore());
        assertEquals(List.of("R003"), result.getEffectiveRuleCodes());
        assertEquals(List.of("R005"), result.getSuppressedRuleCodes());
        assertEquals(1, result.getCategorySummaries().get(0).get("effectiveHitCount"));
        assertEquals(3, result.getCategorySummaries().get(0).get("effectiveScore"));
    }

    @Test
    void shouldUsePriorityAsTieBreakerWhenConflictScoreIsEven() {
        RuleHit positiveHit = new RuleHit();
        positiveHit.setRuleCode("R003");
        positiveHit.setRuleName("用神生世");
        positiveHit.setCategory("YONGSHEN_STATE");
        positiveHit.setScoreDelta(2);
        positiveHit.setPriority(120);

        RuleHit negativeHit = new RuleHit();
        negativeHit.setRuleCode("R005");
        negativeHit.setRuleName("用神空亡");
        negativeHit.setCategory("YONGSHEN_STATE");
        negativeHit.setScoreDelta(-2);
        negativeHit.setPriority(80);

        RuleEvaluationResult result = new RuleReasoningService().summarize(null, List.of(positiveHit, negativeHit));

        assertEquals("POSITIVE_DOMINANT", result.getConflictSummaries().get(0).get("decision"));
        assertEquals(List.of("R003"), result.getEffectiveRuleCodes());
        assertEquals(List.of("R005"), result.getSuppressedRuleCodes());
        assertEquals(2, result.getEffectiveScore());
    }
}
