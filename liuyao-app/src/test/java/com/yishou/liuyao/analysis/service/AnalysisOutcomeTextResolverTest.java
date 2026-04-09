package com.yishou.liuyao.analysis.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisOutcomeTextResolverTest {

    private final AnalysisOutcomeTextResolver resolver = new AnalysisOutcomeTextResolver();

    @Test
    void shouldRenderRiskTextsByCategoryAndLevel() {
        assertTrue(resolver.renderRiskText("收入", "GOOD").contains("落袋速度"));
        assertTrue(resolver.renderRiskText("官司", "BAD").contains("稳住证据和节奏"));
        assertTrue(resolver.renderRiskText("感情", "NEUTRAL").contains("互动有空间"));
    }

    @Test
    void shouldRenderActionSuggestionsByCategoryAndLevel() {
        assertTrue(resolver.renderActionSuggestion("合作", "GOOD").contains("落成明确条件"));
        assertTrue(resolver.renderActionSuggestion("考试", "BAD").contains("复习节奏和临场准备稳住"));
        assertTrue(resolver.renderActionSuggestion("寻物", "NEUTRAL").contains("时间线倒推"));
    }

    @Test
    void shouldRenderConclusionDirectionByCategory() {
        assertTrue(resolver.renderConclusionDirection("财运").contains("投资回报与资金回流节奏"));
        assertTrue(resolver.renderConclusionDirection("合作").contains("对方配合度与条件变动"));
        assertTrue(resolver.renderConclusionDirection("未知分类").contains("关键节点是否兑现"));
    }
}
