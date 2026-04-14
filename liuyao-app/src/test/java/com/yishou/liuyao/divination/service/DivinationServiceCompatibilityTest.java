package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionEnvelope;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionMode;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionService;
import com.yishou.liuyao.casecenter.service.CaseCenterService;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DivinationServiceCompatibilityTest {

    @Mock
    private AnalysisExecutionService analysisExecutionService;
    @Mock
    private CaseCenterService caseCenterService;

    private DivinationService divinationService;

    @BeforeEach
    void setUp() {
        divinationService = new DivinationService(
                analysisExecutionService,
                caseCenterService
        );
    }

    @Test
    void shouldBuildLegacyResponseFromExecutionEnvelope() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        AnalysisExecutionEnvelope executionEnvelope = new AnalysisExecutionEnvelope();
        AnalysisContextDTO analysisContext = new AnalysisContextDTO();
        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        ChartSnapshot chartSnapshot = new ChartSnapshot();
        chartSnapshot.setQuestion("这次合作推进会不会顺利");
        chartSnapshot.setQuestionCategory("合作");
        chartSnapshot.setMainHexagram("山火贲");
        chartSnapshot.setChangedHexagram("风山渐");
        executionEnvelope.setAnalysisContext(analysisContext);
        executionEnvelope.setStructuredResult(structuredResult);
        executionEnvelope.setLegacyAnalysisText("兼容展示文本");
        executionEnvelope.setChartSnapshot(chartSnapshot);
        executionEnvelope.setRuleHits(java.util.List.of());

        when(analysisExecutionService.executeInitial(any(), eq(AnalysisExecutionMode.LEGACY_COMPAT)))
                .thenReturn(executionEnvelope);

        DivinationAnalyzeResponse response = divinationService.analyze(request);

        assertEquals("兼容展示文本", response.getAnalysis());
        verify(analysisExecutionService).executeInitial(request, AnalysisExecutionMode.LEGACY_COMPAT);
    }
}
