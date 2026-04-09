package com.yishou.liuyao.rule.usegod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseGodRuleTest {

    @Test
    void shouldSelectQiCaiForIncomeQuestion() {
        UseGodRule rule = new UseGodRule(
                new UseGodSelector(
                        new QuestionIntentResolver(new QuestionCategoryNormalizer()),
                        new UseGodRuleConfigLoader(new ObjectMapper())
                )
        );

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestion("我下个月工资会不会上涨");
        chartSnapshot.setQuestionCategory("收入");

        RuleHit hit = rule.evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("妻财", hit.getEvidence().get("useGod"));
        assertEquals("v1", hit.getEvidence().get("configVersion"));
    }

    @Test
    void shouldSelectFuMuForRealEstateQuestion() {
        UseGodRule rule = new UseGodRule(
                new UseGodSelector(
                        new QuestionIntentResolver(new QuestionCategoryNormalizer()),
                        new UseGodRuleConfigLoader(new ObjectMapper())
                )
        );

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestion("这次买房手续能顺利办下来吗");
        chartSnapshot.setQuestionCategory("买房");

        RuleHit hit = rule.evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("父母", hit.getEvidence().get("useGod"));
    }

    @Test
    void shouldSelectQiCaiForLostItemQuestion() {
        UseGodRule rule = new UseGodRule(
                new UseGodSelector(
                        new QuestionIntentResolver(new QuestionCategoryNormalizer()),
                        new UseGodRuleConfigLoader(new ObjectMapper())
                )
        );

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestion("丢的东西还能找回来吗");
        chartSnapshot.setQuestionCategory("失物");

        RuleHit hit = rule.evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("妻财", hit.getEvidence().get("useGod"));
    }

    @Test
    void shouldWriteResolvedYingLineEvidence() {
        UseGodRule rule = new UseGodRule(
                new UseGodSelector(
                        new QuestionIntentResolver(new QuestionCategoryNormalizer()),
                        new UseGodRuleConfigLoader(new ObjectMapper())
                )
        );

        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestion("这段关系还能不能继续");
        chartSnapshot.setQuestionCategory("感情");
        chartSnapshot.setShi(3);
        chartSnapshot.setYing(6);
        chartSnapshot.setLines(List.of(
                line(3, "兄弟", true, true, false, "寅"),
                line(6, "官鬼", false, false, true, "巳")
        ));

        RuleHit hit = rule.evaluate(chartSnapshot);

        assertTrue(Boolean.TRUE.equals(hit.getHit()));
        assertEquals("应爻", hit.getEvidence().get("useGod"));
        assertEquals(6, hit.getEvidence().get("selectedLineIndex"));
        assertEquals(List.of(6), hit.getEvidence().get("candidateLineIndexes"));
        assertEquals(6, chartSnapshot.getExt().get("useGodLineIndex"));
    }

    private LineInfo line(int index, String liuQin, boolean moving, boolean shi, boolean ying, String branch) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setLiuQin(liuQin);
        line.setMoving(moving);
        line.setShi(shi);
        line.setYing(ying);
        line.setBranch(branch);
        return line;
    }
}
