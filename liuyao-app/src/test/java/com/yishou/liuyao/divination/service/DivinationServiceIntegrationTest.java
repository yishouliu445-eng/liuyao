package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.casecenter.repository.CaseAnalysisResultRepository;
import com.yishou.liuyao.casecenter.repository.CaseChartSnapshotRepository;
import com.yishou.liuyao.casecenter.repository.CaseRuleHitRepository;
import com.yishou.liuyao.casecenter.repository.DivinationCaseRepository;
import com.yishou.liuyao.casecenter.domain.CaseChartSnapshot;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeResponse;
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
class DivinationServiceIntegrationTest {

    @Autowired
    private DivinationService divinationService;

    @Autowired
    private DivinationCaseRepository divinationCaseRepository;

    @Autowired
    private CaseChartSnapshotRepository caseChartSnapshotRepository;

    @Autowired
    private CaseRuleHitRepository caseRuleHitRepository;

    @Autowired
    private CaseAnalysisResultRepository caseAnalysisResultRepository;

    @Test
    void shouldAnalyzeAndPersistMinimalFlow() {
        long initialCaseCount = divinationCaseRepository.count();
        long initialSnapshotCount = caseChartSnapshotRepository.count();
        long initialRuleHitCount = caseRuleHitRepository.count();
        long initialAnalysisCount = caseAnalysisResultRepository.count();

        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("我下个月工资会不会上涨");
        request.setQuestionCategory("收入");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        DivinationAnalyzeResponse response = divinationService.analyze(request);

        assertNotNull(response.getChartSnapshot());
        assertEquals("收入", response.getChartSnapshot().getQuestionCategory());
        assertEquals("妻财", response.getChartSnapshot().getUseGod());
        assertNotNull(response.getChartSnapshot().getMainHexagramCode());
        assertNotNull(response.getChartSnapshot().getPalace());
        assertFalse(response.getRuleHits().isEmpty());
        assertEquals("妻财", response.getRuleHits().get(0).getEvidence().get("useGod"));
        assertEquals("HIGH", response.getRuleHits().get(0).getImpactLevel());
        assertEquals(initialCaseCount + 1, divinationCaseRepository.count());
        assertEquals(initialSnapshotCount + 1, caseChartSnapshotRepository.count());
        assertEquals(initialRuleHitCount + response.getRuleHits().size(), caseRuleHitRepository.count());
        assertEquals(initialAnalysisCount + 1, caseAnalysisResultRepository.count());

        CaseChartSnapshot savedSnapshot = caseChartSnapshotRepository.findAll().stream()
                .max((left, right) -> Long.compare(left.getId(), right.getId()))
                .orElseThrow();
        assertEquals(response.getChartSnapshot().getMainHexagram(), savedSnapshot.getMainHexagram());
        assertEquals(response.getChartSnapshot().getChangedHexagram(), savedSnapshot.getChangedHexagram());
        assertEquals(response.getChartSnapshot().getPalace(), savedSnapshot.getPalace());
        assertEquals(response.getChartSnapshot().getUseGod(), savedSnapshot.getUseGod());
    }
}
