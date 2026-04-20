package com.yishou.liuyao.rule.usegod;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.batch.UseGodLineLocator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseGodLineLocatorTest {

    @Test
    void shouldResolveYingLineForEmotionQuestion() {
        UseGodSelector selector = new UseGodSelector(
                new QuestionIntentResolver(new QuestionCategoryNormalizer()),
                new UseGodRuleConfigLoader(new ObjectMapper())
        );

        ChartSnapshot chartSnapshot = buildBaseChart();
        chartSnapshot.setQuestion("我和她能不能继续发展");
        chartSnapshot.setQuestionCategory("感情");

        UseGodSelection selection = selector.select(chartSnapshot);

        assertEquals(UseGodType.YING, selection.getUseGod());
        assertEquals(6, selection.getSelectedLineIndex());
        assertEquals(List.of(6), selection.getCandidateLineIndexes());
        assertEquals("SINGLE_MATCH", selection.getSelectionStrategy());
        assertFalse(Boolean.TRUE.equals(selection.getFallbackApplied()));
    }

    @Test
    void shouldPreferMovingOfficialGhostWhenMultipleCandidatesExist() {
        UseGodSelector selector = new UseGodSelector(
                new QuestionIntentResolver(new QuestionCategoryNormalizer()),
                new UseGodRuleConfigLoader(new ObjectMapper())
        );

        ChartSnapshot chartSnapshot = buildBaseChart();
        chartSnapshot.setQuestion("这次面试能拿到 offer 吗");
        chartSnapshot.setQuestionCategory("求职");
        chartSnapshot.setLines(List.of(
                line(2, "官鬼", false, false, false, "申"),
                line(4, "官鬼", true, false, false, "酉"),
                line(6, "兄弟", false, false, true, "亥")
        ));

        UseGodSelection selection = selector.select(chartSnapshot);

        assertEquals(UseGodType.GUAN_GUI, selection.getUseGod());
        assertEquals(4, selection.getSelectedLineIndex());
        assertEquals(List.of(2, 4), selection.getCandidateLineIndexes());
        assertEquals("SCORING", selection.getSelectionStrategy());
        assertTrue(selection.getScoreDetails().size() >= 2);
    }

    @Test
    void shouldFallbackToShiLineWhenSelectedTypeHasNoMatchingLine() {
        UseGodSelector selector = new UseGodSelector(
                new QuestionIntentResolver(new QuestionCategoryNormalizer()),
                new UseGodRuleConfigLoader(new ObjectMapper())
        );

        ChartSnapshot chartSnapshot = buildBaseChart();
        chartSnapshot.setQuestion("丢的文件还能不能找回来");
        chartSnapshot.setQuestionCategory("失物");
        chartSnapshot.setLines(List.of(
                line(1, "父母", false, false, false, "子"),
                line(3, "兄弟", false, true, false, "寅"),
                line(6, "父母", false, false, true, "午")
        ));

        UseGodSelection selection = selector.select(chartSnapshot);

        assertEquals(UseGodType.QI_CAI, selection.getUseGod());
        assertEquals(3, selection.getSelectedLineIndex());
        assertEquals("FALLBACK", selection.getSelectionStrategy());
        assertTrue(Boolean.TRUE.equals(selection.getFallbackApplied()));
        assertEquals("USE_SHI_LINE", selection.getFallbackStrategy());
    }

    @Test
    void shouldFindYingLineFromTypedLocator() {
        ChartSnapshot chartSnapshot = buildBaseChart();

        List<LineInfo> lines = UseGodLineLocator.findCandidates(chartSnapshot, UseGodType.YING);

        assertEquals(1, lines.size());
        assertEquals(6, lines.get(0).getIndex());
    }

    @Test
    void shouldExposeExplicitSelectionStatesWhenScoringMultipleCandidates() {
        ChartSnapshot chartSnapshot = buildBaseChart();
        chartSnapshot.setLines(List.of(
                line(2, "官鬼", false, false, false, "申"),
                line(4, "官鬼", true, false, false, "酉"),
                line(6, "兄弟", false, false, true, "亥")
        ));

        UseGodLineLocator.SelectionResult selection = UseGodLineLocator.locate(chartSnapshot, UseGodType.GUAN_GUI);

        assertEquals(4, selection.selectedLineIndex());
        assertEquals("SCORING", selection.selectionStrategy());
        Map<String, Object> detail = selection.scoreDetails().get(0);
        assertInstanceOf(List.class, detail.get("stateFlags"));
        assertTrue(((List<?>) detail.get("stateFlags")).contains("动"));
        assertTrue(((List<?>) detail.get("stateFlags")).contains("近世"));
    }

    private ChartSnapshot buildBaseChart() {
        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setShi(3);
        chartSnapshot.setYing(6);
        chartSnapshot.setKongWang(List.of("戌"));
        chartSnapshot.setLines(List.of(
                line(1, "父母", false, false, false, "子"),
                line(2, "官鬼", false, false, false, "丑"),
                line(3, "兄弟", false, true, false, "寅"),
                line(4, "妻财", false, false, false, "卯"),
                line(5, "子孙", false, false, false, "辰"),
                line(6, "官鬼", false, false, true, "巳")
        ));
        return chartSnapshot;
    }

    private LineInfo line(int index, String liuQin, boolean moving, boolean shi, boolean ying, String branch) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setLiuQin(liuQin);
        line.setMoving(moving);
        line.setShi(shi);
        line.setYing(ying);
        line.setBranch(branch);
        line.setWuXing(UseGodLineLocator.branchToWuXing(branch));
        return line;
    }
}
