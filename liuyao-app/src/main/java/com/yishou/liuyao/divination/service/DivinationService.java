package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.analysis.service.AnalysisService;
import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.RuleConflictSummaryDTO;
import com.yishou.liuyao.analysis.dto.RuleCategorySummaryDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.casecenter.service.CaseCenterService;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeResponse;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.divination.dto.LineInfoDTO;
import com.yishou.liuyao.divination.mapper.DivinationMapper;
import com.yishou.liuyao.knowledge.service.KnowledgeSearchService;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.dto.RuleHitDTO;
import com.yishou.liuyao.rule.service.RuleEvaluationResult;
import com.yishou.liuyao.rule.service.RuleEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DivinationService {

    private static final Logger log = LoggerFactory.getLogger(DivinationService.class);

    private final DivinationMapper divinationMapper;
    private final ChartBuilderService chartBuilderService;
    private final RuleEngineService ruleEngineService;
    private final AnalysisService analysisService;
    private final CaseCenterService caseCenterService;
    private final KnowledgeSearchService knowledgeSearchService;

    public DivinationService(DivinationMapper divinationMapper,
                             ChartBuilderService chartBuilderService,
                             RuleEngineService ruleEngineService,
                             AnalysisService analysisService,
                             CaseCenterService caseCenterService,
                             KnowledgeSearchService knowledgeSearchService) {
        this.divinationMapper = divinationMapper;
        this.chartBuilderService = chartBuilderService;
        this.ruleEngineService = ruleEngineService;
        this.analysisService = analysisService;
        this.caseCenterService = caseCenterService;
        this.knowledgeSearchService = knowledgeSearchService;
    }

    public DivinationAnalyzeResponse analyze(DivinationAnalyzeRequest request) {
        log.info("开始分析六爻请求: category={}, time={}, rawLineCount={}, movingLineCount={}",
                request.getQuestionCategory(),
                request.getDivinationTime(),
                request.getRawLines() == null ? 0 : request.getRawLines().size(),
                request.getMovingLines() == null ? 0 : request.getMovingLines().size());
        DivinationInput input = divinationMapper.toInput(request);
        ChartSnapshot chartSnapshot = chartBuilderService.buildChart(input);
        RuleEvaluationResult evaluationResult = ruleEngineService.evaluateResult(chartSnapshot);
        List<RuleHit> ruleHits = evaluationResult.getHits();
        StructuredAnalysisResultDTO structuredResult = toStructuredResultDto(evaluationResult);
        AnalysisContextDTO analysisContext = buildAnalysisContext(request.getQuestionText(), chartSnapshot, ruleHits, structuredResult);
        if (log.isDebugEnabled()) {
            log.debug("分析上下文摘要: question={}, useGod={}, ruleCodes={}",
                    analysisContext.getQuestion(),
                    analysisContext.getChartSnapshot() == null ? "" : analysisContext.getChartSnapshot().getUseGod(),
                    analysisContext.getRuleHits().stream().map(RuleHitDTO::getRuleCode).toList());
        }
        String analysis = analysisService.analyze(analysisContext);
        caseCenterService.recordAnalysis(request, chartSnapshot, ruleHits, analysisContext, structuredResult, analysis);
        log.info("六爻分析完成: mainHexagram={}, changedHexagram={}, useGod={}, ruleHitCount={}",
                chartSnapshot.getMainHexagram(),
                chartSnapshot.getChangedHexagram(),
                chartSnapshot.getUseGod(),
                ruleHits.size());
        return new DivinationAnalyzeResponse(
                toChartSnapshotDto(chartSnapshot),
                toRuleHitDtos(ruleHits),
                analysis,
                analysisContext,
                structuredResult
        );
    }

    private AnalysisContextDTO buildAnalysisContext(String question,
                                                    ChartSnapshot chartSnapshot,
                                                    List<RuleHit> ruleHits,
                                                    StructuredAnalysisResultDTO structuredResult) {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setContextVersion("v1");
        context.setQuestion(question);
        context.setQuestionCategory(chartSnapshot.getQuestionCategory());
        context.setUseGod(chartSnapshot.getUseGod());
        context.setMainHexagram(chartSnapshot.getMainHexagram());
        context.setChangedHexagram(chartSnapshot.getChangedHexagram());
        context.setChartSnapshot(toChartSnapshotDto(chartSnapshot));
        context.setRuleHits(toRuleHitDtos(ruleHits));
        context.setRuleCount(ruleHits == null ? 0 : ruleHits.size());
        context.setRuleCodes(ruleHits == null ? List.of() : ruleHits.stream().map(RuleHit::getRuleCode).toList());
        context.setStructuredResult(structuredResult);
        context.setKnowledgeSnippets(knowledgeSearchService.suggestKnowledgeSnippets(
                chartSnapshot.getQuestionCategory(),
                chartSnapshot.getUseGod(),
                context.getRuleCodes(),
                6
        ));
        return context;
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

    private StructuredAnalysisResultDTO toStructuredResultDto(RuleEvaluationResult evaluationResult) {
        StructuredAnalysisResultDTO dto = new StructuredAnalysisResultDTO();
        dto.setScore(evaluationResult.getScore());
        dto.setResultLevel(evaluationResult.getResultLevel());
        dto.setEffectiveScore(evaluationResult.getEffectiveScore());
        dto.setEffectiveResultLevel(evaluationResult.getEffectiveResultLevel());
        dto.setTags(evaluationResult.getTags());
        dto.setEffectiveRuleCodes(evaluationResult.getEffectiveRuleCodes());
        dto.setSuppressedRuleCodes(evaluationResult.getSuppressedRuleCodes());
        dto.setSummary(evaluationResult.getSummary());
        dto.setCategorySummaries(evaluationResult.getCategorySummaries().stream().map(this::toCategorySummaryDto).toList());
        dto.setConflictSummaries(evaluationResult.getConflictSummaries().stream().map(this::toConflictSummaryDto).toList());
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

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private ChartSnapshotDTO toChartSnapshotDto(ChartSnapshot chartSnapshot) {
        // 响应层只暴露稳定字段，避免把领域对象里的扩展结构直接泄漏到接口契约。
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
        dto.setLines(chartSnapshot.getLines().stream().map(this::toLineInfoDto).toList());
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
}
