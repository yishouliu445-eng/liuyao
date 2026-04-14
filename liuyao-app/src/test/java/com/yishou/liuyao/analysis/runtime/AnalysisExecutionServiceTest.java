package com.yishou.liuyao.analysis.runtime;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.presentation.PresentationCompatibilityAdapter;
import com.yishou.liuyao.analysis.service.PromptTemplateEngine;
import com.yishou.liuyao.analysis.service.AnalysisContextFactory;
import com.yishou.liuyao.analysis.service.AnalysisService;
import com.yishou.liuyao.analysis.service.OrchestratedAnalysisService;
import com.yishou.liuyao.analysis.validation.AnalysisValidationPipeline;
import com.yishou.liuyao.analysis.validation.AnalysisValidationResult;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.mapper.DivinationMapper;
import com.yishou.liuyao.divination.service.ChartBuilderService;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import com.yishou.liuyao.evidence.dto.EvidenceSelectionResult;
import com.yishou.liuyao.evidence.service.CitationValidationResult;
import com.yishou.liuyao.evidence.service.CitationValidationService;
import com.yishou.liuyao.evidence.service.EvidenceRetrievalService;
import com.yishou.liuyao.ops.audit.service.ExecutionAuditService;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.service.RuleEngineService;
import com.yishou.liuyao.rule.service.RuleEvaluationResult;
import com.yishou.liuyao.session.domain.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisExecutionServiceTest {

    @Mock
    private DivinationMapper divinationMapper;
    @Mock
    private ChartBuilderService chartBuilderService;
    @Mock
    private RuleEngineService ruleEngineService;
    @Mock
    private EvidenceRetrievalService evidenceRetrievalService;
    @Mock
    private CitationValidationService citationValidationService;
    @Mock
    private AnalysisValidationPipeline analysisValidationPipeline;
    @Mock
    private DegradationResolver degradationResolver;
    @Mock
    private ExecutionAuditService executionAuditService;
    @Mock
    private OrchestratedAnalysisService orchestratedAnalysisService;
    @Mock
    private AnalysisContextFactory analysisContextFactory;
    @Mock
    private PresentationCompatibilityAdapter presentationCompatibilityAdapter;
    @Mock
    private AnalysisService legacyAnalysisService;
    @Mock
    private PromptTemplateEngine promptTemplateEngine;

    private AnalysisExecutionService service;

    @BeforeEach
    void setUp() {
        service = new AnalysisExecutionService(
                divinationMapper,
                chartBuilderService,
                ruleEngineService,
                evidenceRetrievalService,
                citationValidationService,
                analysisValidationPipeline,
                degradationResolver,
                executionAuditService,
                orchestratedAnalysisService,
                analysisContextFactory,
                presentationCompatibilityAdapter,
                legacyAnalysisService,
                promptTemplateEngine
        );
    }

    @Test
    void executeInitialShouldReturnUnifiedExecutionEnvelope() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("我这个月面试结果如何");
        request.setQuestionCategory("事业");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 12, 10, 0));

        DivinationInput input = new DivinationInput();
        ChartSnapshot chartSnapshot = sampleChartSnapshot();
        RuleEvaluationResult evaluationResult = sampleEvaluationResult();
        AnalysisContextDTO analysisContext = sampleAnalysisContext();
        AnalysisOutputDTO analysisOutput = sampleAnalysisOutput("综合来看，事情有推进空间。");
        EvidenceSelectionResult evidenceSelection = sampleEvidenceSelection();

        when(divinationMapper.toInput(request)).thenReturn(input);
        when(chartBuilderService.buildChart(input)).thenReturn(chartSnapshot);
        when(ruleEngineService.evaluateResult(chartSnapshot)).thenReturn(evaluationResult);
        when(analysisContextFactory.create(request.getQuestionText(), chartSnapshot, evaluationResult.getHits()))
                .thenReturn(analysisContext);
        when(evidenceRetrievalService.retrieveInitial(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(evidenceSelection);
        when(orchestratedAnalysisService.analyzeInitial(eq(chartSnapshot), anyList(), eq(8), eq("POSITIVE"), anyList()))
                .thenReturn(analysisOutput);
        when(citationValidationService.enrichAndValidate(eq(analysisOutput), eq(evidenceSelection.getHits())))
                .thenReturn(validCitationValidationResult());
        when(analysisValidationPipeline.validate(eq(analysisOutput), any(), eq(evidenceSelection.getHits()), anyList(), eq("事业")))
                .thenReturn(validAnalysisValidationResult());
        when(degradationResolver.resolve(any())).thenReturn(noDegradation());
        when(presentationCompatibilityAdapter.render(eq(analysisContext), any(), eq(analysisOutput))).thenReturn("兼容文本");
        when(promptTemplateEngine.getCurrentVersion()).thenReturn("v1");

        AnalysisExecutionEnvelope envelope = service.executeInitial(request, AnalysisExecutionMode.INITIAL);

        assertNotNull(envelope.getExecutionId());
        assertEquals(AnalysisExecutionMode.INITIAL, envelope.getMode());
        assertSame(chartSnapshot, envelope.getChartSnapshot());
        assertSame(analysisContext, envelope.getAnalysisContext());
        assertSame(analysisOutput, envelope.getAnalysisOutput());
        assertEquals("兼容文本", envelope.getLegacyAnalysisText());
        assertEquals(1, envelope.getRuleHits().size());
        assertNotNull(analysisContext.getChartSnapshot());
        assertEquals("地火明夷", analysisContext.getChartSnapshot().getMainHexagram());
        assertEquals(1, analysisContext.getRuleHits().size());
        assertEquals("career-positive", analysisContext.getRuleHits().get(0).getRuleCode());
        assertEquals("[《增删卜易》·用神章] 用神得地则事可成", analysisContext.getKnowledgeSnippets().get(0));
        assertEquals("chunk:21", analysisContext.getEvidenceHits().get(0).getCitationId());
        assertNotNull(analysisContext.getStructuredResult());
        assertEquals(8, analysisContext.getStructuredResult().getEffectiveScore());
        assertTrue(analysisContext.getStructuredResult().getEffectiveRuleCodes().isEmpty());
        assertNotNull(envelope.getVersions());
        assertEquals("v1", envelope.getVersions().getPromptVersion());
        assertEquals("mock-gpt", envelope.getVersions().getModelVersion());
        assertEquals(1, envelope.getAnalysisOutput().getMetadata().getRagSourceCount());
        verify(executionAuditService).record(eq(envelope), any());
    }

    @Test
    void executeFollowUpShouldReturnUnifiedExecutionEnvelope() {
        ChartSnapshot chartSnapshot = sampleChartSnapshot();
        RuleEvaluationResult evaluationResult = sampleEvaluationResult();
        AnalysisOutputDTO analysisOutput = sampleAnalysisOutput("追问来看，结果仍偏正面。");
        List<ChatMessage> history = List.of(ChatMessage.userMessage(java.util.UUID.randomUUID(), "我这个月面试结果如何"));
        EvidenceSelectionResult evidenceSelection = new EvidenceSelectionResult();
        evidenceSelection.setHits(List.of());

        when(ruleEngineService.evaluateResult(chartSnapshot)).thenReturn(evaluationResult);
        when(evidenceRetrievalService.retrieveFollowUp(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(evidenceSelection);
        when(orchestratedAnalysisService.analyzeFollowUp(eq(chartSnapshot), anyList(), eq(8), eq("POSITIVE"), eq(history), anyList(), eq("还需要注意什么？")))
                .thenReturn(analysisOutput);
        when(citationValidationService.enrichAndValidate(eq(analysisOutput), eq(List.of())))
                .thenReturn(validCitationValidationResult());
        when(analysisValidationPipeline.validate(eq(analysisOutput), any(), eq(List.of()), anyList(), eq("事业")))
                .thenReturn(validAnalysisValidationResult());
        when(degradationResolver.resolve(any())).thenReturn(noDegradation());
        when(presentationCompatibilityAdapter.render(eq(null), any(), eq(analysisOutput))).thenReturn("追问来看，结果仍偏正面。");
        when(promptTemplateEngine.getCurrentVersion()).thenReturn("v1");

        AnalysisExecutionEnvelope envelope = service.executeFollowUp(chartSnapshot, history, "还需要注意什么？");

        assertNotNull(envelope.getExecutionId());
        assertEquals(AnalysisExecutionMode.FOLLOW_UP, envelope.getMode());
        assertSame(chartSnapshot, envelope.getChartSnapshot());
        assertSame(analysisOutput, envelope.getAnalysisOutput());
        assertEquals("追问来看，结果仍偏正面。", envelope.getLegacyAnalysisText());
        verify(evidenceRetrievalService).retrieveFollowUp("事业", "官鬼", "还需要注意什么？", 4);
        verify(executionAuditService).record(eq(envelope), any());
    }

    @Test
    void executeInitialShouldDropUnmappedClassicReferences() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("合作这件事能不能推进");
        request.setQuestionCategory("合作");

        DivinationInput input = new DivinationInput();
        ChartSnapshot chartSnapshot = sampleChartSnapshot();
        RuleEvaluationResult evaluationResult = sampleEvaluationResult();
        AnalysisContextDTO analysisContext = sampleAnalysisContext();
        AnalysisOutputDTO analysisOutput = sampleAnalysisOutput("综合来看，可以推进。");
        AnalysisOutputDTO.ClassicReference reference = new AnalysisOutputDTO.ClassicReference();
        reference.setSource("《卜筮正宗·世应章》");
        reference.setQuote("世应相合则吉");
        reference.setRelevance("引用用于辅助判断");
        analysisOutput.getAnalysis().setClassicReferences(new java.util.ArrayList<>(List.of(reference)));
        EvidenceSelectionResult evidenceSelection = sampleEvidenceSelection();
        CitationValidationResult validationResult = new CitationValidationResult();
        validationResult.setValid(false);
        validationResult.setUnmatchedReferences(List.of(reference));

        when(divinationMapper.toInput(request)).thenReturn(input);
        when(chartBuilderService.buildChart(input)).thenReturn(chartSnapshot);
        when(ruleEngineService.evaluateResult(chartSnapshot)).thenReturn(evaluationResult);
        when(analysisContextFactory.create(request.getQuestionText(), chartSnapshot, evaluationResult.getHits()))
                .thenReturn(analysisContext);
        when(evidenceRetrievalService.retrieveInitial(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(evidenceSelection);
        when(orchestratedAnalysisService.analyzeInitial(eq(chartSnapshot), anyList(), eq(8), eq("POSITIVE"), anyList()))
                .thenReturn(analysisOutput);
        when(citationValidationService.enrichAndValidate(eq(analysisOutput), eq(evidenceSelection.getHits())))
                .thenReturn(validationResult);
        when(analysisValidationPipeline.validate(eq(analysisOutput), any(), eq(evidenceSelection.getHits()), anyList(), eq("事业")))
                .thenReturn(validAnalysisValidationResult());
        when(degradationResolver.resolve(any())).thenReturn(noDegradation());
        when(presentationCompatibilityAdapter.render(eq(analysisContext), any(), eq(analysisOutput))).thenReturn("兼容文本");
        when(promptTemplateEngine.getCurrentVersion()).thenReturn("v1");

        AnalysisExecutionEnvelope envelope = service.executeInitial(request, AnalysisExecutionMode.INITIAL);

        assertTrue(envelope.getAnalysisOutput().getAnalysis().getClassicReferences().isEmpty());
        assertEquals(1, envelope.getAnalysisOutput().getMetadata().getRagSourceCount());
    }

    private ChartSnapshot sampleChartSnapshot() {
        ChartSnapshot snapshot = new ChartSnapshot();
        snapshot.setQuestion("我这个月面试结果如何");
        snapshot.setQuestionCategory("事业");
        snapshot.setDivinationMethod("手工起卦");
        snapshot.setDivinationTime(LocalDateTime.of(2026, 4, 12, 10, 0));
        snapshot.setMainHexagram("地火明夷");
        snapshot.setChangedHexagram("地天泰");
        snapshot.setMainHexagramCode("000101");
        snapshot.setChangedHexagramCode("000111");
        snapshot.setPalace("坎");
        snapshot.setPalaceWuXing("水");
        snapshot.setUseGod("官鬼");
        snapshot.setRiChen("甲子");
        snapshot.setYueJian("辰");
        return snapshot;
    }

    private RuleEvaluationResult sampleEvaluationResult() {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("career-positive");
        hit.setHit(Boolean.TRUE);
        hit.setRuleName("官鬼得势");
        hit.setCategory("CAREER");

        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setHits(List.of(hit));
        result.setEffectiveScore(8);
        result.setEffectiveResultLevel("POSITIVE");
        return result;
    }

    private AnalysisContextDTO sampleAnalysisContext() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setContextVersion("v1");
        context.setQuestion("我这个月面试结果如何");
        context.setQuestionCategory("事业");
        context.setUseGod("官鬼");
        context.setRuleCodes(List.of("career-positive"));
        context.setRuleCount(1);
        return context;
    }

    private AnalysisOutputDTO sampleAnalysisOutput(String conclusion) {
        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setConclusion(conclusion);
        analysis.setDetailedReasoning("推演显示主线向好。");
        analysis.setActionPlan(List.of("保持节奏", "准备复盘"));

        AnalysisOutputDTO dto = new AnalysisOutputDTO();
        dto.setAnalysis(analysis);
        dto.setSmartPrompts(List.of("何时有结果", "还有什么风险"));
        AnalysisOutputDTO.AnalysisMetadata metadata = new AnalysisOutputDTO.AnalysisMetadata();
        metadata.setModelUsed("mock-gpt");
        dto.setMetadata(metadata);
        return dto;
    }

    private EvidenceSelectionResult sampleEvidenceSelection() {
        EvidenceHit hit = new EvidenceHit();
        hit.setChunkId(21L);
        hit.setBookId(1L);
        hit.setCitationId("chunk:21");
        hit.setSourceTitle("增删卜易");
        hit.setChapterTitle("用神章");
        hit.setContent("用神得地则事可成");

        EvidenceSelectionResult result = new EvidenceSelectionResult();
        result.setHits(List.of(hit));
        return result;
    }

    private CitationValidationResult validCitationValidationResult() {
        CitationValidationResult result = new CitationValidationResult();
        result.setValid(true);
        return result;
    }

    private AnalysisValidationResult validAnalysisValidationResult() {
        return new AnalysisValidationResult();
    }

    private AnalysisExecutionDegradation noDegradation() {
        return new AnalysisExecutionDegradation();
    }
}
