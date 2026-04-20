package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.divination.dto.ShenShaHitDTO;
import com.yishou.liuyao.rule.dto.RuleHitDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisSectionComposerTest {

    private final AnalysisSectionComposer composer = new AnalysisSectionComposer(
            new AnalysisKnowledgeEvidenceService(),
            new AnalysisCategoryTextResolver(),
            new AnalysisOutcomeTextResolver(),
            new AnalysisPhaseTwoSignalFormatter()
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

    @Test
    void shouldComposePhaseTwoSignalsIntoMechanicalAnalysis() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setQuestionCategory("出行");
        context.setUseGod("父母");
        context.setMainHexagram("山火贲");
        context.setChangedHexagram("风山渐");
        context.setRuleCount(4);
        context.setRuleCodes(List.of("R010", "FAN_FU_YIN", "TIMING_SIGNAL", "SHEN_SHA"));

        ChartSnapshotDTO chartSnapshot = new ChartSnapshotDTO();
        chartSnapshot.setQuestionCategory("出行");
        chartSnapshot.setMainHexagram("山火贲");
        chartSnapshot.setChangedHexagram("风山渐");
        chartSnapshot.setMutualHexagram("雷水解");
        chartSnapshot.setOppositeHexagram("泽水困");
        chartSnapshot.setReversedHexagram("火雷噬嗑");
        ShenShaHitDTO nobleman = new ShenShaHitDTO();
        nobleman.setName("天乙贵人");
        chartSnapshot.setShenShaHits(List.of(nobleman));
        context.setChartSnapshot(chartSnapshot);

        RuleHitDTO fanFuYin = new RuleHitDTO();
        fanFuYin.setRuleCode("FAN_FU_YIN");
        fanFuYin.setHitReason("局部动爻反吟，事情更易反复。");
        RuleHitDTO timing = new RuleHitDTO();
        timing.setRuleCode("TIMING_SIGNAL");
        timing.setEvidence(Map.of(
                "timingBucket", "SHORT_TERM",
                "timingHint", "用神发动，近日更容易出现动静。"
        ));
        context.setRuleHits(List.of(fanFuYin, timing));

        StructuredAnalysisResultDTO result = new StructuredAnalysisResultDTO();
        result.setEffectiveResultLevel("GOOD");
        result.setEffectiveScore(2);
        result.setSummary("整体可推进。");
        result.setTags(List.of("有利", "神煞信号"));
        result.setEffectiveRuleCodes(List.of("R010", "R206"));
        context.setStructuredResult(result);

        String analysis = composer.compose(context);

        assertTrue(analysis.contains("互卦雷水解"));
        assertTrue(analysis.contains("错卦泽水困"));
        assertTrue(analysis.contains("综卦火雷噬嗑"));
        assertTrue(analysis.contains("反伏吟提示：局部动爻反吟，事情更易反复"));
        assertTrue(analysis.contains("应期参考偏近期：用神发动，近日更容易出现动静"));
        assertTrue(analysis.contains("神煞辅助：盘面见天乙贵人"));
    }
}
