package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalysisContextFactoryTest {

    private final AnalysisContextFactory factory = new AnalysisContextFactory();

    @Test
    void shouldCreateBaseAnalysisContextFromChartAndRuleHits() {
        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestionCategory("合作");
        chartSnapshot.setUseGod("应爻");
        chartSnapshot.setMainHexagram("山火贲");
        chartSnapshot.setChangedHexagram("风山渐");

        RuleHit first = new RuleHit();
        first.setRuleCode("R010");
        RuleHit second = new RuleHit();
        second.setRuleCode("SHI_YING_RELATION");

        AnalysisContextDTO context = factory.create("这次合作能成吗", chartSnapshot, List.of(first, second));

        assertEquals("v1", context.getContextVersion());
        assertEquals("这次合作能成吗", context.getQuestion());
        assertEquals("合作", context.getQuestionCategory());
        assertEquals("应爻", context.getUseGod());
        assertEquals("山火贲", context.getMainHexagram());
        assertEquals("风山渐", context.getChangedHexagram());
        assertEquals(2, context.getRuleCount());
        assertEquals(List.of("R010", "SHI_YING_RELATION"), context.getRuleCodes());
    }
}
