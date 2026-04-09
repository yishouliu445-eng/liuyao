package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisSectionComposerTest {

    private final AnalysisSectionComposer composer = new AnalysisSectionComposer(
            new AnalysisKnowledgeEvidenceService(),
            new AnalysisCategoryTextResolver(),
            new AnalysisOutcomeTextResolver()
    );

    @Test
    void shouldComposeOrderedAnalysisSections() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestionCategory("合作");
        context.setUseGod("应爻");
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        context.setRuleCount(3);
        context.setRuleCodes(List.of("R010"));
        context.setKnowledgeSnippets(List.of("[卜筮正宗] 世应相接，则看对方回应与来往节度。"));

        StructuredAnalysisResultDTO result = new StructuredAnalysisResultDTO();
        result.setEffectiveResultLevel("GOOD");
        result.setEffectiveScore(2);
        result.setSummary("整体可推进。");
        result.setTags(List.of("有利"));
        result.setEffectiveRuleCodes(List.of("R010"));
        result.setSuppressedRuleCodes(List.of("R005"));
        context.setStructuredResult(result);

        String analysis = composer.compose(context);

        assertTrue(analysis.contains("卦象概览"));
        assertTrue(analysis.contains("用神判断"));
        assertTrue(analysis.contains("关系判断"));
        assertTrue(analysis.contains("动爻影响"));
        assertTrue(analysis.contains("风险提示"));
        assertTrue(analysis.contains("结论建议"));
        assertTrue(analysis.contains("下一步建议"));
        assertTrue(analysis.contains("可参考资料"));
    }
}
