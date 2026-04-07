package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.RuleCategorySummaryDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisServiceTest {

    @Test
    void shouldGenerateReadableStructuredAnalysisText() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestion("我下个月工资会不会上涨");
        context.setQuestionCategory("收入");
        context.setUseGod("妻财");
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        context.setRuleCount(6);
        context.setRuleCodes(List.of("R003", "R010", "USE_GOD_STRENGTH"));
        context.setKnowledgeSnippets(List.of("《增删卜易》 用神宜旺相，不宜休囚。"));

        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setSummary("本卦山火贲，围绕用神妻财共命中6条规则。");
        structuredResult.setResultLevel("NEUTRAL");
        structuredResult.setEffectiveResultLevel("GOOD");
        structuredResult.setEffectiveScore(3);
        structuredResult.setTags(List.of("有利", "主动"));
        structuredResult.setEffectiveRuleCodes(List.of("R003", "R010"));
        structuredResult.setSuppressedRuleCodes(List.of("R005"));
        RuleCategorySummaryDTO categorySummaryDTO = new RuleCategorySummaryDTO();
        categorySummaryDTO.setCategory("YONGSHEN_STATE");
        categorySummaryDTO.setHitCount(3);
        categorySummaryDTO.setScore(2);
        categorySummaryDTO.setEffectiveHitCount(2);
        categorySummaryDTO.setEffectiveScore(3);
        structuredResult.setCategorySummaries(List.of(categorySummaryDTO));
        context.setStructuredResult(structuredResult);

        String analysis = new AnalysisService().analyze(context);

        assertTrue(analysis.contains("问收入"));
        assertTrue(analysis.contains("卦象概览"));
        assertTrue(analysis.contains("用神判断"));
        assertTrue(analysis.contains("结论建议"));
        assertTrue(analysis.contains("以妻财为用神"));
        assertTrue(analysis.contains("山火贲"));
        assertTrue(analysis.contains("风山渐"));
        assertTrue(analysis.contains("有效评分3"));
        assertTrue(analysis.contains("《增删卜易》"));
    }
}
