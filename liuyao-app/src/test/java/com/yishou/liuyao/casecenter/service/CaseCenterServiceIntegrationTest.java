package com.yishou.liuyao.casecenter.service;

import com.yishou.liuyao.casecenter.dto.CaseDetailDTO;
import com.yishou.liuyao.casecenter.dto.CaseListResponseDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayAssessmentListDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunListDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunStatsDTO;
import com.yishou.liuyao.casecenter.dto.CaseSummaryDTO;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.service.DivinationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=true")
class CaseCenterServiceIntegrationTest {

    @Autowired
    private DivinationService divinationService;

    @Autowired
    private CaseCenterService caseCenterService;

    @Test
    void shouldListRecentCasesWithSnapshotSummaryFields() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次出行会不会顺利");
        request.setQuestionCategory("出行");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 7, 9, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        divinationService.analyze(request);

        List<CaseSummaryDTO> cases = caseCenterService.listRecentCases();
        CaseSummaryDTO travelCase = cases.stream()
                .filter(item -> "这次出行会不会顺利".equals(item.getQuestionText()))
                .findFirst()
                .orElseThrow();

        assertFalse(cases.isEmpty());
        assertEquals("出行", travelCase.getQuestionCategory());
        assertEquals("父母", travelCase.getUseGod());
        assertEquals("ANALYZED", travelCase.getStatus());
    }

    @Test
    void shouldLoadCaseDetailWithChartRulesAndAnalysis() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次合作签约能不能顺利推进");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 8, 11, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        divinationService.analyze(request);

        CaseSummaryDTO summary = caseCenterService.listRecentCases().stream()
                .filter(item -> "这次合作签约能不能顺利推进".equals(item.getQuestionText()))
                .findFirst()
                .orElseThrow();
        CaseDetailDTO detail = caseCenterService.getCaseDetail(summary.getCaseId());

        assertEquals(summary.getCaseId(), detail.getCaseId());
        assertEquals("合作", detail.getQuestionCategory());
        assertNotNull(detail.getChartSnapshot());
        assertEquals("应爻", detail.getChartSnapshot().getUseGod());
        assertNotNull(detail.getChartSnapshot().getMainUpperTrigram());
        assertNotNull(detail.getChartSnapshot().getMainLowerTrigram());
        assertEquals("v1", detail.getChartSnapshot().getSnapshotVersion());
        assertEquals("v1", detail.getChartSnapshot().getCalendarVersion());
        assertEquals("卯", detail.getChartSnapshot().getLines().get(0).getBranch());
        assertEquals("辰", detail.getChartSnapshot().getLines().get(0).getChangeBranch());
        assertEquals("父母", detail.getChartSnapshot().getLines().get(4).getChangeLiuQin());
        assertFalse(detail.getRuleHits().isEmpty());
        assertNotNull(detail.getStructuredResult());
        assertNotNull(detail.getStructuredResult().getScore());
        assertNotNull(detail.getStructuredResult().getResultLevel());
        assertNotNull(detail.getAnalysis());
        assertNotNull(detail.getAnalysisContext());
        assertEquals("v1", detail.getAnalysisContext().getContextVersion());
        assertEquals("应爻", detail.getAnalysisContext().getUseGod());
        assertEquals(detail.getChartSnapshot().getMainHexagram(), detail.getAnalysisContext().getMainHexagram());
        assertEquals(detail.getChartSnapshot().getMainHexagram(), detail.getAnalysisContext().getChartSnapshot().getMainHexagram());
        assertNotNull(detail.getRuleHits().get(0).getRuleId());
        assertNotNull(detail.getRuleHits().get(0).getScoreDelta());
        assertNotNull(detail.getRuleHits().get(0).getTags());
    }

    @Test
    void shouldSupportPagedCategorySearchAtServiceLevel() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次合作沟通是否顺利");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 13, 9, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        divinationService.analyze(request);

        CaseListResponseDTO response = caseCenterService.listCases("合作", 1, 5);

        assertEquals(1, response.getPage());
        assertEquals(5, response.getSize());
        assertFalse(response.getItems().isEmpty());
        assertEquals("合作", response.getItems().get(0).getQuestionCategory());
    }

    @Test
    void shouldReplaySavedCaseThroughCurrentRuleAndAnalysisPipeline() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次合作推进会不会反复");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 14, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        divinationService.analyze(request);

        Long caseId = caseCenterService.listRecentCases().stream()
                .filter(item -> "这次合作推进会不会反复".equals(item.getQuestionText()))
                .findFirst()
                .orElseThrow()
                .getCaseId();

        CaseReplayDTO replay = caseCenterService.replayCase(caseId);

        assertEquals(caseId, replay.getCaseId());
        assertFalse(replay.getBaselineRuleCodes().isEmpty());
        assertFalse(replay.getReplayRuleCodes().isEmpty());
        assertNotNull(replay.getBaselineEffectiveRuleCodes());
        assertNotNull(replay.getReplayEffectiveRuleCodes());
        assertNotNull(replay.getBaselineSuppressedRuleCodes());
        assertNotNull(replay.getReplaySuppressedRuleCodes());
        assertNotNull(replay.getBaselineTags());
        assertNotNull(replay.getReplayTags());
        assertNotNull(replay.getBaselineEffectiveScore());
        assertNotNull(replay.getReplayEffectiveScore());
        assertNotNull(replay.getEffectiveScoreDelta());
        assertNotNull(replay.getBaselineRuleVersion());
        assertNotNull(replay.getReplayRuleVersion());
        assertEquals("v1", replay.getReplayRuleVersion());
        assertNotNull(replay.getReplayUseGodConfigVersion());
        assertEquals("v1", replay.getReplayUseGodConfigVersion());
        assertNotNull(replay.getRuleBundleVersion());
        assertEquals("v1", replay.getRuleBundleVersion());
        assertNotNull(replay.getRuleDefinitionsVersion());
        assertEquals("v1", replay.getRuleDefinitionsVersion());
        assertNotNull(replay.getUseGodRulesVersion());
        assertEquals("v1", replay.getUseGodRulesVersion());
        assertNotNull(replay.getResultLevelChanged());
        assertNotNull(replay.getBaselineAnalysisContext());
        assertNotNull(replay.getBaselineStructuredResult());
        assertFalse(replay.getBaselineRuleHits().isEmpty());
        assertNotNull(replay.getBaselineSummary());
        assertNotNull(replay.getReplaySummary());
        assertNotNull(replay.getSummaryChanged());
        assertNotNull(replay.getAnalysisChanged());
        assertNotNull(replay.getRecommendPersistReplay());
        assertNotNull(replay.getPersistenceAssessment());
        assertFalse(replay.getPersistenceAssessment().isBlank());
        assertNotNull(replay.getReplayAnalysis());
        assertNotNull(replay.getReplayAnalysisContext());
        assertNotNull(replay.getReplayStructuredResult());
        assertFalse(replay.getReplayRuleHits().isEmpty());
        assertTrue(replay.getReplayAnalysis().contains("合作"));
    }

    @Test
    void shouldListReplayPersistenceAssessmentsWithoutPersistingHistory() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次合作后续是否还有变数");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 16, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        divinationService.analyze(request);

        CaseReplayAssessmentListDTO response = caseCenterService.listReplayPersistenceAssessments("合作", 1, 10);

        assertEquals(1, response.getPage());
        assertEquals(10, response.getSize());
        assertTrue(response.getTotal() >= 1);
        assertFalse(response.getItems().isEmpty());
        assertEquals("合作", response.getItems().get(0).getQuestionCategory());
        assertNotNull(response.getItems().get(0).getRecommendPersistReplay());
        assertNotNull(response.getItems().get(0).getPersistenceAssessment());
        assertNotNull(response.getItems().get(0).getRuleBundleVersion());
        assertNotNull(response.getItems().get(0).getReplayRuleVersion());
    }

    @Test
    void shouldPersistAndListReplayRunsForCase() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次合作复盘要不要落 replay run");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 18, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        divinationService.analyze(request);

        Long caseId = caseCenterService.listRecentCases().stream()
                .filter(item -> "这次合作复盘要不要落 replay run".equals(item.getQuestionText()))
                .findFirst()
                .orElseThrow()
                .getCaseId();

        CaseReplayRunDTO created = caseCenterService.createReplayRun(caseId);

        assertNotNull(created.getReplayRunId());
        assertEquals(caseId, created.getCaseId());
        assertNotNull(created.getRuleBundleVersion());
        assertNotNull(created.getPayloadJson());

        List<CaseReplayRunDTO> history = caseCenterService.listReplayRuns(caseId);

        assertFalse(history.isEmpty());
        assertEquals(caseId, history.get(0).getCaseId());
        assertNotNull(history.get(0).getReplayRuleVersion());
    }

    @Test
    void shouldSearchReplayRunsWithCategoryAndRecommendationFilters() {
        DivinationAnalyzeRequest coopRequest = new DivinationAnalyzeRequest();
        coopRequest.setQuestionText("这次合作 replay run 是否值得筛选");
        coopRequest.setQuestionCategory("合作");
        coopRequest.setDivinationMethod("手工起卦");
        coopRequest.setDivinationTime(LocalDateTime.of(2026, 4, 20, 10, 0));
        coopRequest.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        coopRequest.setMovingLines(List.of(1, 5));
        divinationService.analyze(coopRequest);

        DivinationAnalyzeRequest travelRequest = new DivinationAnalyzeRequest();
        travelRequest.setQuestionText("这次出行 replay run 是否值得筛选");
        travelRequest.setQuestionCategory("出行");
        travelRequest.setDivinationMethod("手工起卦");
        travelRequest.setDivinationTime(LocalDateTime.of(2026, 4, 21, 10, 0));
        travelRequest.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        travelRequest.setMovingLines(List.of(1, 5));
        divinationService.analyze(travelRequest);

        Long coopCaseId = caseCenterService.listRecentCases().stream()
                .filter(item -> "这次合作 replay run 是否值得筛选".equals(item.getQuestionText()))
                .findFirst()
                .orElseThrow()
                .getCaseId();
        Long travelCaseId = caseCenterService.listRecentCases().stream()
                .filter(item -> "这次出行 replay run 是否值得筛选".equals(item.getQuestionText()))
                .findFirst()
                .orElseThrow()
                .getCaseId();

        caseCenterService.createReplayRun(coopCaseId);
        caseCenterService.createReplayRun(travelCaseId);

        CaseReplayRunListDTO filtered = caseCenterService.listReplayRuns("合作", false, 1, 10);

        assertEquals(1, filtered.getPage());
        assertEquals(10, filtered.getSize());
        assertTrue(filtered.getTotal() >= 1);
        assertFalse(filtered.getItems().isEmpty());
        assertEquals("合作", filtered.getItems().get(0).getQuestionCategory());
        assertEquals(Boolean.FALSE, filtered.getItems().get(0).getRecommendPersistReplay());
        assertNotNull(filtered.getItems().get(0).getReplayRunId());
    }

    @Test
    void shouldSummarizeReplayRunStats() {
        DivinationAnalyzeRequest coopRequest = new DivinationAnalyzeRequest();
        coopRequest.setQuestionText("这次合作 replay run 统计是否可见");
        coopRequest.setQuestionCategory("合作");
        coopRequest.setDivinationMethod("手工起卦");
        coopRequest.setDivinationTime(LocalDateTime.of(2026, 4, 23, 10, 0));
        coopRequest.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        coopRequest.setMovingLines(List.of(1, 5));
        divinationService.analyze(coopRequest);

        DivinationAnalyzeRequest travelRequest = new DivinationAnalyzeRequest();
        travelRequest.setQuestionText("这次出行 replay run 统计是否可见");
        travelRequest.setQuestionCategory("出行");
        travelRequest.setDivinationMethod("手工起卦");
        travelRequest.setDivinationTime(LocalDateTime.of(2026, 4, 24, 10, 0));
        travelRequest.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        travelRequest.setMovingLines(List.of(1, 5));
        divinationService.analyze(travelRequest);

        Long coopCaseId = caseCenterService.listRecentCases().stream()
                .filter(item -> "这次合作 replay run 统计是否可见".equals(item.getQuestionText()))
                .findFirst()
                .orElseThrow()
                .getCaseId();
        Long travelCaseId = caseCenterService.listRecentCases().stream()
                .filter(item -> "这次出行 replay run 统计是否可见".equals(item.getQuestionText()))
                .findFirst()
                .orElseThrow()
                .getCaseId();

        caseCenterService.createReplayRun(coopCaseId);
        caseCenterService.createReplayRun(travelCaseId);

        CaseReplayRunStatsDTO stats = caseCenterService.getReplayRunStats();

        assertTrue(stats.getTotalRuns() >= 2);
        assertNotNull(stats.getRecommendPersistRuns());
        assertNotNull(stats.getObserveOnlyRuns());
        assertFalse(stats.getCategoryStats().isEmpty());
        assertTrue(stats.getCategoryStats().stream().anyMatch(item -> "合作".equals(item.getQuestionCategory())));
    }
}
