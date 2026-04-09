package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisCategoryTextResolverTest {

    private final AnalysisCategoryTextResolver resolver = new AnalysisCategoryTextResolver();

    @Test
    void shouldRenderCategorySpecificUseGodFocusAndMovingSignal() {
        assertEquals("重点看机会是否成形，以及录用流程能否真正推进。", resolver.renderUseGodFocus("求职", "官鬼"));
        assertTrue(resolver.renderMovingSignal("合作", true).contains("合作条件"));
        assertTrue(resolver.renderMovingSignal("房产", false).contains("手续推进"));
    }

    @Test
    void shouldRenderDominantSignalByEffectiveRules() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveRuleCodes(List.of("R021"));
        context.setStructuredResult(structuredResult);

        assertTrue(resolver.renderDominantSignalText(context).contains("变化途中"));
    }

    @Test
    void shouldRenderDominantSignalForConstraintAndChongKaiRules() {
        AnalysisContextDTO constrainedContext = new AnalysisContextDTO();
        StructuredAnalysisResultDTO constrainedResult = new StructuredAnalysisResultDTO();
        constrainedResult.setEffectiveRuleCodes(List.of("R030"));
        constrainedContext.setStructuredResult(constrainedResult);

        AnalysisContextDTO chongKaiContext = new AnalysisContextDTO();
        StructuredAnalysisResultDTO chongKaiResult = new StructuredAnalysisResultDTO();
        chongKaiResult.setEffectiveRuleCodes(List.of("R031"));
        chongKaiContext.setStructuredResult(chongKaiResult);

        assertTrue(resolver.renderDominantSignalText(constrainedContext).contains("受制较深"));
        assertTrue(resolver.renderDominantSignalText(chongKaiContext).contains("被冲开"));
    }
}
