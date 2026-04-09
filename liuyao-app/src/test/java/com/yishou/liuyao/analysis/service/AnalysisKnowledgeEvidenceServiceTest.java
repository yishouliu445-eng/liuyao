package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisKnowledgeEvidenceServiceTest {

    private final AnalysisKnowledgeEvidenceService service = new AnalysisKnowledgeEvidenceService();

    @Test
    void shouldPreferRiskSnippetMatchingCategoryAndRuleCodes() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestionCategory("求职");
        context.setKnowledgeSnippets(List.of(
                "[通用] 宜继续观察，不必着急。",
                "[增删卜易] 官鬼空亡，求职之事多拖延反复。"
        ));
        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveRuleCodes(List.of("USE_GOD_EMPTY"));
        context.setStructuredResult(structuredResult);

        String snippet = service.selectKnowledgeSnippet(context, "risk");

        assertEquals("[增删卜易] 官鬼空亡，求职之事多拖延反复。", snippet);
    }

    @Test
    void shouldAppendReadableKnowledgeEvidence() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestionCategory("合作");
        context.setRuleCodes(List.of("SHI_YING_RELATION"));
        context.setKnowledgeSnippets(List.of("[卜筮正宗] 世应相接，则看对方回应与来往节度。"));

        String text = service.appendKnowledgeEvidence("继续看对方是否给出实质回应", context, "action");

        assertTrue(text.contains("可结合卜筮正宗中的"));
        assertTrue(text.contains("继续判断"));
    }

    @Test
    void shouldPreferConstraintSnippetForRuMuRule() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestionCategory("财运");
        context.setKnowledgeSnippets(List.of(
                "[通用] 宜继续观察财务节奏。",
                "[增删卜易] 用神入墓，财气受困，先防迟滞与压制。"
        ));
        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveRuleCodes(List.of("R030"));
        context.setStructuredResult(structuredResult);

        String snippet = service.selectKnowledgeSnippet(context, "risk");

        assertEquals("[增删卜易] 用神入墓，财气受困，先防迟滞与压制。", snippet);
    }
}
