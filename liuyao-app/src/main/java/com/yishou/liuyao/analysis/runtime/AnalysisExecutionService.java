package com.yishou.liuyao.analysis.runtime;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.RuleCategorySummaryDTO;
import com.yishou.liuyao.analysis.dto.RuleConflictSummaryDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.analysis.presentation.PresentationCompatibilityAdapter;
import com.yishou.liuyao.analysis.service.AnalysisContextFactory;
import com.yishou.liuyao.analysis.service.AnalysisService;
import com.yishou.liuyao.analysis.service.OrchestratedAnalysisService;
import com.yishou.liuyao.analysis.service.PromptTemplateEngine;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.divination.dto.LineInfoDTO;
import com.yishou.liuyao.divination.dto.ShenShaHitDTO;
import com.yishou.liuyao.divination.mapper.DivinationMapper;
import com.yishou.liuyao.divination.service.ChartBuilderService;
import com.yishou.liuyao.divination.domain.ShenShaHit;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import com.yishou.liuyao.evidence.dto.EvidenceSelectionResult;
import com.yishou.liuyao.evidence.service.CitationValidationResult;
import com.yishou.liuyao.evidence.service.CitationValidationService;
import com.yishou.liuyao.evidence.service.EvidenceRetrievalService;
import com.yishou.liuyao.analysis.validation.AnalysisValidationPipeline;
import com.yishou.liuyao.analysis.validation.AnalysisValidationResult;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.dto.RuleHitDTO;
import com.yishou.liuyao.rule.service.RuleEngineService;
import com.yishou.liuyao.rule.service.RuleEvaluationResult;
import com.yishou.liuyao.session.domain.ChatMessage;
import com.yishou.liuyao.ops.audit.service.ExecutionAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisExecutionService.class);

    private final DivinationMapper divinationMapper;
    private final ChartBuilderService chartBuilderService;
    private final RuleEngineService ruleEngineService;
    private final EvidenceRetrievalService evidenceRetrievalService;
    private final CitationValidationService citationValidationService;
    private final AnalysisValidationPipeline analysisValidationPipeline;
    private final DegradationResolver degradationResolver;
    private final ExecutionAuditService executionAuditService;
    private final OrchestratedAnalysisService orchestratedAnalysisService;
    private final AnalysisContextFactory analysisContextFactory;
    private final PresentationCompatibilityAdapter presentationCompatibilityAdapter;
    private final AnalysisService legacyAnalysisService;
    private final PromptTemplateEngine promptTemplateEngine;

    public AnalysisExecutionService(DivinationMapper divinationMapper,
                                    ChartBuilderService chartBuilderService,
                                    RuleEngineService ruleEngineService,
                                    EvidenceRetrievalService evidenceRetrievalService,
                                    CitationValidationService citationValidationService,
                                    AnalysisValidationPipeline analysisValidationPipeline,
                                    DegradationResolver degradationResolver,
                                    ExecutionAuditService executionAuditService,
                                    OrchestratedAnalysisService orchestratedAnalysisService,
                                    AnalysisContextFactory analysisContextFactory,
                                    PresentationCompatibilityAdapter presentationCompatibilityAdapter,
                                    AnalysisService legacyAnalysisService,
                                    PromptTemplateEngine promptTemplateEngine) {
        this.divinationMapper = divinationMapper;
        this.chartBuilderService = chartBuilderService;
        this.ruleEngineService = ruleEngineService;
        this.evidenceRetrievalService = evidenceRetrievalService;
        this.citationValidationService = citationValidationService;
        this.analysisValidationPipeline = analysisValidationPipeline;
        this.degradationResolver = degradationResolver;
        this.executionAuditService = executionAuditService;
        this.orchestratedAnalysisService = orchestratedAnalysisService;
        this.analysisContextFactory = analysisContextFactory;
        this.presentationCompatibilityAdapter = presentationCompatibilityAdapter;
        this.legacyAnalysisService = legacyAnalysisService;
        this.promptTemplateEngine = promptTemplateEngine;
    }

    public AnalysisExecutionEnvelope executeInitial(DivinationAnalyzeRequest request, AnalysisExecutionMode mode) {
        DivinationInput input = divinationMapper.toInput(request);
        ChartSnapshot chartSnapshot = chartBuilderService.buildChart(input);
        RuleEvaluationResult evaluationResult = ruleEngineService.evaluateResult(chartSnapshot);
        List<RuleHit> ruleHits = evaluationResult.getHits();
        StructuredAnalysisResultDTO structuredResult = toStructuredResultDto(evaluationResult);
        AnalysisContextDTO analysisContext = analysisContextFactory.create(request.getQuestionText(), chartSnapshot, ruleHits);
        EvidenceSelectionResult evidenceSelection = safeRetrieveInitialEvidence(chartSnapshot, ruleHits);
        List<EvidenceHit> evidenceHits = evidenceSelection.getHits();
        List<String> knowledgeSnippets = evidenceSelection.toPromptSnippets();
        if (analysisContext != null) {
            analysisContext.setChartSnapshot(toChartSnapshotDto(chartSnapshot));
            analysisContext.setRuleHits(toRuleHitDtos(ruleHits));
            analysisContext.setStructuredResult(structuredResult);
            analysisContext.setKnowledgeSnippets(knowledgeSnippets);
            analysisContext.setEvidenceHits(evidenceHits);
        }
        AnalysisOutputDTO analysisOutput = orchestratedAnalysisService.analyzeInitial(
                chartSnapshot,
                ruleHits,
                safeInt(evaluationResult.getEffectiveScore()),
                evaluationResult.getEffectiveResultLevel(),
                knowledgeSnippets
        );
        enrichAnalysisOutput(analysisOutput, evidenceHits);
        AnalysisValidationResult validationResult = validateOutput(
                analysisOutput,
                structuredResult,
                evidenceHits,
                knowledgeSnippets,
                chartSnapshot.getQuestionCategory()
        );
        AnalysisExecutionEnvelope envelope = buildEnvelope(mode, chartSnapshot, ruleHits, structuredResult,
                analysisContext, applySafetyFallbackIfNeeded(analysisOutput, validationResult),
                knowledgeSnippets, evidenceHits, validationResult);
        executionAuditService.record(envelope, validationResult);
        return envelope;
    }

    public AnalysisExecutionEnvelope executeFollowUp(ChartSnapshot chartSnapshot,
                                                     List<ChatMessage> history,
                                                     String followUpQuestion) {
        RuleEvaluationResult evaluationResult = ruleEngineService.evaluateResult(chartSnapshot);
        List<RuleHit> ruleHits = evaluationResult.getHits();
        StructuredAnalysisResultDTO structuredResult = toStructuredResultDto(evaluationResult);
        EvidenceSelectionResult evidenceSelection = safeRetrieveFollowUpEvidence(followUpQuestion, chartSnapshot);
        List<EvidenceHit> evidenceHits = evidenceSelection.getHits();
        List<String> knowledgeSnippets = evidenceSelection.toPromptSnippets();
        AnalysisOutputDTO analysisOutput = orchestratedAnalysisService.analyzeFollowUp(
                chartSnapshot,
                ruleHits,
                safeInt(evaluationResult.getEffectiveScore()),
                evaluationResult.getEffectiveResultLevel(),
                history,
                knowledgeSnippets,
                followUpQuestion
        );
        enrichAnalysisOutput(analysisOutput, evidenceHits);
        AnalysisValidationResult validationResult = validateOutput(
                analysisOutput,
                structuredResult,
                evidenceHits,
                knowledgeSnippets,
                chartSnapshot.getQuestionCategory()
        );
        AnalysisExecutionEnvelope envelope = buildEnvelope(AnalysisExecutionMode.FOLLOW_UP, chartSnapshot, ruleHits, structuredResult,
                null, applySafetyFallbackIfNeeded(analysisOutput, validationResult),
                knowledgeSnippets, evidenceHits, validationResult);
        executionAuditService.record(envelope, validationResult);
        return envelope;
    }

    private AnalysisExecutionEnvelope buildEnvelope(AnalysisExecutionMode mode,
                                                    ChartSnapshot chartSnapshot,
                                                    List<RuleHit> ruleHits,
                                                    StructuredAnalysisResultDTO structuredResult,
                                                    AnalysisContextDTO analysisContext,
                                                    AnalysisOutputDTO analysisOutput,
                                                    List<String> knowledgeSnippets,
                                                    List<EvidenceHit> evidenceHits,
                                                    AnalysisValidationResult validationResult) {
        AnalysisExecutionEnvelope envelope = new AnalysisExecutionEnvelope();
        envelope.setExecutionId(UUID.randomUUID());
        envelope.setMode(mode);
        envelope.setChartSnapshot(chartSnapshot);
        envelope.setRuleHits(ruleHits);
        envelope.setStructuredResult(structuredResult);
        envelope.setAnalysisContext(analysisContext);
        envelope.setAnalysisOutput(analysisOutput);
        envelope.setLegacyAnalysisText(buildLegacyAnalysisText(analysisContext, structuredResult, analysisOutput));
        envelope.setKnowledgeSnippets(knowledgeSnippets);
        envelope.setEvidenceHits(evidenceHits);
        envelope.setDegradation(degradationResolver.resolve(validationResult));
        envelope.setVersions(buildVersions(analysisOutput));
        return envelope;
    }

    private AnalysisExecutionVersions buildVersions(AnalysisOutputDTO analysisOutput) {
        AnalysisExecutionVersions versions = new AnalysisExecutionVersions();
        versions.setPromptVersion(promptTemplateEngine.getCurrentVersion());
        versions.setModelVersion(resolveModelVersion(analysisOutput));
        return versions;
    }

    private EvidenceSelectionResult safeRetrieveInitialEvidence(ChartSnapshot chart, List<RuleHit> ruleHits) {
        try {
            List<String> ruleCodes = ruleHits.stream()
                    .filter(h -> Boolean.TRUE.equals(h.getHit()))
                    .map(RuleHit::getRuleCode)
                    .toList();
            return evidenceRetrievalService.retrieveInitial(
                    chart.getQuestionCategory(), chart.getUseGod(), ruleCodes, 6);
        } catch (Exception e) {
            log.warn("RAG知识检索失败（不影响主流程）: {}", e.getMessage());
            return emptyEvidenceSelection();
        }
    }

    private EvidenceSelectionResult safeRetrieveFollowUpEvidence(String question, ChartSnapshot chart) {
        try {
            return evidenceRetrievalService.retrieveFollowUp(
                    chart.getQuestionCategory(), chart.getUseGod(), question, 4);
        } catch (Exception e) {
            log.warn("追问RAG检索失败: {}", e.getMessage());
            return emptyEvidenceSelection();
        }
    }

    private EvidenceSelectionResult emptyEvidenceSelection() {
        EvidenceSelectionResult selection = new EvidenceSelectionResult();
        selection.setHits(Collections.emptyList());
        return selection;
    }

    private void enrichAnalysisOutput(AnalysisOutputDTO analysisOutput, List<EvidenceHit> evidenceHits) {
        if (analysisOutput == null) {
            return;
        }
        AnalysisOutputDTO.AnalysisMetadata metadata = analysisOutput.getMetadata();
        if (metadata == null) {
            metadata = new AnalysisOutputDTO.AnalysisMetadata();
            analysisOutput.setMetadata(metadata);
        }
        metadata.setRagSourceCount(evidenceHits == null ? 0 : evidenceHits.size());

        CitationValidationResult validation = citationValidationService.enrichAndValidate(analysisOutput, evidenceHits);
        if (!validation.isValid()
                && analysisOutput.getAnalysis() != null
                && analysisOutput.getAnalysis().getClassicReferences() != null) {
            analysisOutput.getAnalysis().getClassicReferences()
                    .removeAll(validation.getUnmatchedReferences());
            log.warn("清理未映射的古籍引用 {} 条，避免返回悬空 citation", validation.getUnmatchedReferences().size());
        }
    }

    private AnalysisValidationResult validateOutput(AnalysisOutputDTO analysisOutput,
                                                    StructuredAnalysisResultDTO structuredResult,
                                                    List<EvidenceHit> evidenceHits,
                                                    List<String> knowledgeSnippets,
                                                    String questionCategory) {
        return analysisValidationPipeline.validate(
                analysisOutput,
                structuredResult,
                evidenceHits,
                knowledgeSnippets,
                questionCategory
        );
    }

    private AnalysisOutputDTO applySafetyFallbackIfNeeded(AnalysisOutputDTO analysisOutput,
                                                          AnalysisValidationResult validationResult) {
        AnalysisExecutionDegradation degradation = degradationResolver.resolve(validationResult);
        if (!"BLOCK".equals(degradation.getLevel())) {
            return analysisOutput;
        }
        AnalysisOutputDTO safeOutput = new AnalysisOutputDTO();
        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setConclusion("当前问题涉及高敏感情境，我只能提供一般性参考，不能给出绝对判断。");
        analysis.setActionPlan(List.of("结合专业人士意见再做决定", "先补充关键事实与正式信息来源"));
        analysis.setEmotionalTone("CAUTIOUS");
        safeOutput.setAnalysis(analysis);
        safeOutput.setMetadata(analysisOutput == null ? null : analysisOutput.getMetadata());
        safeOutput.setSmartPrompts(List.of("还有哪些事实需要进一步确认？", "下一步最稳妥的行动是什么？", "如何降低风险？"));
        return safeOutput;
    }

    private String buildLegacyAnalysisText(AnalysisContextDTO analysisContext,
                                           StructuredAnalysisResultDTO structuredResult,
                                           AnalysisOutputDTO analysisOutput) {
        try {
            String compatibleText = presentationCompatibilityAdapter.render(
                    analysisContext,
                    structuredResult,
                    analysisOutput
            );
            if (compatibleText != null && !compatibleText.isBlank()) {
                return compatibleText;
            }
        } catch (Exception exception) {
            log.warn("生成兼容展示文本失败，尝试回退旧分析组件: {}", exception.getMessage());
        }
        if (analysisContext != null) {
            try {
                String legacyAnalysis = legacyAnalysisService.analyze(analysisContext);
                if (legacyAnalysis != null && !legacyAnalysis.isBlank()) {
                    return legacyAnalysis;
                }
            } catch (Exception exception) {
                log.warn("生成兼容分析文本失败，回退为结论摘要: {}", exception.getMessage());
            }
        }
        if (analysisOutput == null || analysisOutput.getAnalysis() == null) {
            return "";
        }
        String conclusion = analysisOutput.getAnalysis().getConclusion();
        return conclusion == null ? "" : conclusion;
    }

    private StructuredAnalysisResultDTO toStructuredResultDto(RuleEvaluationResult evaluationResult) {
        StructuredAnalysisResultDTO dto = new StructuredAnalysisResultDTO();
        dto.setScore(evaluationResult.getScore());
        dto.setResultLevel(evaluationResult.getResultLevel());
        dto.setEffectiveScore(evaluationResult.getEffectiveScore());
        dto.setEffectiveResultLevel(evaluationResult.getEffectiveResultLevel());
        dto.setTags(evaluationResult.getTags() != null ? evaluationResult.getTags() : List.of());
        dto.setEffectiveRuleCodes(evaluationResult.getEffectiveRuleCodes() != null
                ? evaluationResult.getEffectiveRuleCodes() : List.of());
        dto.setSuppressedRuleCodes(evaluationResult.getSuppressedRuleCodes() != null
                ? evaluationResult.getSuppressedRuleCodes() : List.of());
        dto.setSummary(evaluationResult.getSummary());
        dto.setCategorySummaries(evaluationResult.getCategorySummaries() == null
                ? List.of()
                : evaluationResult.getCategorySummaries().stream().map(this::toCategorySummaryDto).toList());
        dto.setConflictSummaries(evaluationResult.getConflictSummaries() == null
                ? List.of()
                : evaluationResult.getConflictSummaries().stream().map(this::toConflictSummaryDto).toList());
        return dto;
    }

    private List<RuleHitDTO> toRuleHitDtos(List<RuleHit> ruleHits) {
        return ruleHits.stream().map(this::toRuleHitDto).toList();
    }

    private RuleHitDTO toRuleHitDto(RuleHit ruleHit) {
        RuleHitDTO dto = new RuleHitDTO();
        dto.setRuleId(ruleHit.getRuleId());
        dto.setRuleCode(ruleHit.getRuleCode());
        dto.setRuleName(ruleHit.getRuleName());
        dto.setCategory(ruleHit.getCategory());
        dto.setPriority(ruleHit.getPriority());
        dto.setHitReason(ruleHit.getHitReason());
        dto.setImpactLevel(ruleHit.getImpactLevel());
        dto.setScoreDelta(ruleHit.getScoreDelta());
        dto.setTags(ruleHit.getTags());
        dto.setEvidence(ruleHit.getEvidence());
        return dto;
    }

    private ChartSnapshotDTO toChartSnapshotDto(ChartSnapshot chartSnapshot) {
        ChartSnapshotDTO dto = new ChartSnapshotDTO();
        dto.setQuestion(chartSnapshot.getQuestion());
        dto.setQuestionCategory(chartSnapshot.getQuestionCategory());
        dto.setDivinationMethod(chartSnapshot.getDivinationMethod());
        dto.setDivinationTime(chartSnapshot.getDivinationTime());
        dto.setMainHexagram(chartSnapshot.getMainHexagram());
        dto.setChangedHexagram(chartSnapshot.getChangedHexagram());
        dto.setMainHexagramCode(chartSnapshot.getMainHexagramCode());
        dto.setChangedHexagramCode(chartSnapshot.getChangedHexagramCode());
        dto.setMainUpperTrigram(chartSnapshot.getMainUpperTrigram());
        dto.setMainLowerTrigram(chartSnapshot.getMainLowerTrigram());
        dto.setChangedUpperTrigram(chartSnapshot.getChangedUpperTrigram());
        dto.setChangedLowerTrigram(chartSnapshot.getChangedLowerTrigram());
        dto.setMutualHexagram(chartSnapshot.getMutualHexagram());
        dto.setMutualHexagramCode(chartSnapshot.getMutualHexagramCode());
        dto.setOppositeHexagram(chartSnapshot.getOppositeHexagram());
        dto.setOppositeHexagramCode(chartSnapshot.getOppositeHexagramCode());
        dto.setReversedHexagram(chartSnapshot.getReversedHexagram());
        dto.setReversedHexagramCode(chartSnapshot.getReversedHexagramCode());
        dto.setPalace(chartSnapshot.getPalace());
        dto.setPalaceWuXing(chartSnapshot.getPalaceWuXing());
        dto.setShi(chartSnapshot.getShi());
        dto.setYing(chartSnapshot.getYing());
        dto.setUseGod(chartSnapshot.getUseGod());
        dto.setRiChen(chartSnapshot.getRiChen());
        dto.setYueJian(chartSnapshot.getYueJian());
        dto.setSnapshotVersion(chartSnapshot.getSnapshotVersion());
        dto.setCalendarVersion(chartSnapshot.getCalendarVersion());
        dto.setKongWang(chartSnapshot.getKongWang());
        dto.setShenShaHits(chartSnapshot.getShenShaHits().stream().map(this::toShenShaHitDto).toList());
        dto.setLines(chartSnapshot.getLines().stream().map(this::toLineInfoDto).toList());
        return dto;
    }

    private ShenShaHitDTO toShenShaHitDto(ShenShaHit shenShaHit) {
        ShenShaHitDTO dto = new ShenShaHitDTO();
        dto.setCode(shenShaHit.getCode());
        dto.setName(shenShaHit.getName());
        dto.setScope(shenShaHit.getScope());
        dto.setBranch(shenShaHit.getBranch());
        dto.setMatchedBy(shenShaHit.getMatchedBy());
        dto.setSummary(shenShaHit.getSummary());
        dto.setLineIndexes(shenShaHit.getLineIndexes());
        dto.setEvidence(shenShaHit.getEvidence());
        return dto;
    }

    private LineInfoDTO toLineInfoDto(LineInfo lineInfo) {
        LineInfoDTO dto = new LineInfoDTO();
        dto.setIndex(lineInfo.getIndex());
        dto.setYinYang(lineInfo.getYinYang());
        dto.setMoving(lineInfo.getMoving());
        dto.setChangeTo(lineInfo.getChangeTo());
        dto.setLiuQin(lineInfo.getLiuQin());
        dto.setLiuShen(lineInfo.getLiuShen());
        dto.setBranch(lineInfo.getBranch());
        dto.setWuXing(lineInfo.getWuXing());
        dto.setChangeBranch(lineInfo.getChangeBranch());
        dto.setChangeWuXing(lineInfo.getChangeWuXing());
        dto.setChangeLiuQin(lineInfo.getChangeLiuQin());
        dto.setShi(lineInfo.getShi());
        dto.setYing(lineInfo.getYing());
        return dto;
    }

    private RuleCategorySummaryDTO toCategorySummaryDto(java.util.Map<String, Object> item) {
        RuleCategorySummaryDTO dto = new RuleCategorySummaryDTO();
        dto.setCategory(String.valueOf(item.getOrDefault("category", "GENERAL")));
        dto.setHitCount(asInteger(item.get("hitCount")));
        dto.setScore(asInteger(item.get("score")));
        dto.setEffectiveHitCount(asInteger(item.get("effectiveHitCount")));
        dto.setEffectiveScore(asInteger(item.get("effectiveScore")));
        dto.setStageOrder(asInteger(item.get("stageOrder")));
        return dto;
    }

    private RuleConflictSummaryDTO toConflictSummaryDto(java.util.Map<String, Object> item) {
        RuleConflictSummaryDTO dto = new RuleConflictSummaryDTO();
        dto.setCategory(String.valueOf(item.getOrDefault("category", "GENERAL")));
        dto.setPositiveCount(asInteger(item.get("positiveCount")));
        dto.setNegativeCount(asInteger(item.get("negativeCount")));
        dto.setPositiveScore(asInteger(item.get("positiveScore")));
        dto.setNegativeScore(asInteger(item.get("negativeScore")));
        dto.setNetScore(asInteger(item.get("netScore")));
        dto.setDecision(String.valueOf(item.getOrDefault("decision", "MIXED")));
        dto.setPositiveRules(asStringList(item.get("positiveRules")));
        dto.setNegativeRules(asStringList(item.get("negativeRules")));
        dto.setEffectiveRules(asStringList(item.get("effectiveRules")));
        dto.setSuppressedRules(asStringList(item.get("suppressedRules")));
        return dto;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String resolveModelVersion(AnalysisOutputDTO analysisOutput) {
        if (analysisOutput == null || analysisOutput.getMetadata() == null) {
            return "unknown";
        }
        String modelUsed = analysisOutput.getMetadata().getModelUsed();
        return modelUsed == null || modelUsed.isBlank() ? "unknown" : modelUsed;
    }
}
