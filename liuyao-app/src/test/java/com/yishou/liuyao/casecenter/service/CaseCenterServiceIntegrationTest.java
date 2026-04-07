package com.yishou.liuyao.casecenter.service;

import com.yishou.liuyao.casecenter.dto.CaseDetailDTO;
import com.yishou.liuyao.casecenter.dto.CaseListResponseDTO;
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
}
