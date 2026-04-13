package com.yishou.liuyao.casecenter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.RuleCategorySummaryDTO;
import com.yishou.liuyao.analysis.dto.RuleConflictSummaryDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.analysis.service.AnalysisContextFactory;
import com.yishou.liuyao.analysis.service.AnalysisService;
import com.yishou.liuyao.casecenter.domain.CaseAnalysisResult;
import com.yishou.liuyao.casecenter.domain.CaseChartSnapshot;
import com.yishou.liuyao.casecenter.domain.CaseReplayRun;
import com.yishou.liuyao.casecenter.domain.CaseRuleHit;
import com.yishou.liuyao.casecenter.domain.DivinationCase;
import com.yishou.liuyao.casecenter.dto.CaseDetailDTO;
import com.yishou.liuyao.casecenter.dto.CaseListResponseDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayAssessmentDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayAssessmentListDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunCategoryStatsDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunListDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunStatsDTO;
import com.yishou.liuyao.casecenter.dto.CaseSummaryDTO;
import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.casecenter.repository.CaseAnalysisResultRepository;
import com.yishou.liuyao.casecenter.repository.CaseChartSnapshotRepository;
import com.yishou.liuyao.casecenter.repository.CaseReplayRunRepository;
import com.yishou.liuyao.casecenter.repository.CaseRuleHitRepository;
import com.yishou.liuyao.casecenter.repository.DivinationCaseRepository;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.knowledge.service.KnowledgeSearchService;
import com.yishou.liuyao.infrastructure.util.JsonUtils;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.dto.RuleHitDTO;
import com.yishou.liuyao.rule.service.RuleResourceMetadata;
import com.yishou.liuyao.rule.service.RuleResourceMetadataLoader;
import com.yishou.liuyao.rule.service.RuleEngineService;
import com.yishou.liuyao.rule.service.RuleEvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
public class CaseCenterService {

    private static final Logger log = LoggerFactory.getLogger(CaseCenterService.class);

    private final DivinationCaseRepository divinationCaseRepository;
    private final CaseChartSnapshotRepository caseChartSnapshotRepository;
    private final CaseRuleHitRepository caseRuleHitRepository;
    private final CaseAnalysisResultRepository caseAnalysisResultRepository;
    private final CaseReplayRunRepository caseReplayRunRepository;
    private final ObjectMapper objectMapper;
    private final RuleEngineService ruleEngineService;
    private final RuleResourceMetadataLoader ruleResourceMetadataLoader;
    private final AnalysisService analysisService;
    private final AnalysisContextFactory analysisContextFactory;
    private final KnowledgeSearchService knowledgeSearchService;

    public CaseCenterService(DivinationCaseRepository divinationCaseRepository,
                             CaseChartSnapshotRepository caseChartSnapshotRepository,
                             CaseRuleHitRepository caseRuleHitRepository,
                             CaseAnalysisResultRepository caseAnalysisResultRepository,
                             CaseReplayRunRepository caseReplayRunRepository,
                             ObjectMapper objectMapper,
                             RuleEngineService ruleEngineService,
                             RuleResourceMetadataLoader ruleResourceMetadataLoader,
                             AnalysisService analysisService,
                             AnalysisContextFactory analysisContextFactory,
                             KnowledgeSearchService knowledgeSearchService) {
        this.divinationCaseRepository = divinationCaseRepository;
        this.caseChartSnapshotRepository = caseChartSnapshotRepository;
        this.caseRuleHitRepository = caseRuleHitRepository;
        this.caseAnalysisResultRepository = caseAnalysisResultRepository;
        this.caseReplayRunRepository = caseReplayRunRepository;
        this.objectMapper = objectMapper;
        this.ruleEngineService = ruleEngineService;
        this.ruleResourceMetadataLoader = ruleResourceMetadataLoader;
        this.analysisService = analysisService;
        this.analysisContextFactory = analysisContextFactory;
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @Transactional
    public RecordedAnalysisRefs recordAnalysis(DivinationAnalyzeRequest request,
                                               ChartSnapshot chartSnapshot,
                                               List<RuleHit> ruleHits,
                                               AnalysisContextDTO analysisContext,
                                               StructuredAnalysisResultDTO structuredResult,
                                               String analysis) {
        // 一次分析会同时留下 case、快照、规则命中和分析结果四类留痕。
        DivinationCase divinationCase = new DivinationCase();
        divinationCase.setQuestionText(request.getQuestionText());
        divinationCase.setQuestionCategory(request.getQuestionCategory());
        divinationCase.setDivinationTime(request.getDivinationTime());
        divinationCase.setStatus("ANALYZED");
        divinationCase = divinationCaseRepository.save(divinationCase);

        CaseChartSnapshot snapshot = new CaseChartSnapshot();
        snapshot.setCaseId(divinationCase.getId());
        snapshot.setMainHexagram(chartSnapshot.getMainHexagram());
        snapshot.setChangedHexagram(chartSnapshot.getChangedHexagram());
        snapshot.setMainHexagramCode(chartSnapshot.getMainHexagramCode());
        snapshot.setChangedHexagramCode(chartSnapshot.getChangedHexagramCode());
        snapshot.setPalace(chartSnapshot.getPalace());
        snapshot.setPalaceWuXing(chartSnapshot.getPalaceWuXing());
        snapshot.setUseGod(chartSnapshot.getUseGod());
        snapshot.setChartJson(JsonUtils.toJson(objectMapper, chartSnapshot));
        caseChartSnapshotRepository.save(snapshot);

        for (RuleHit ruleHit : ruleHits) {
            CaseRuleHit entity = new CaseRuleHit();
            entity.setCaseId(divinationCase.getId());
            entity.setRuleId(ruleHit.getRuleId());
            entity.setRuleCode(ruleHit.getRuleCode());
            entity.setRuleName(ruleHit.getRuleName());
            entity.setHitReason(ruleHit.getHitReason());
            entity.setImpactLevel(ruleHit.getImpactLevel());
            entity.setCategory(ruleHit.getCategory());
            entity.setPriority(ruleHit.getPriority());
            entity.setScoreDelta(ruleHit.getScoreDelta());
            entity.setTagsJson(JsonUtils.toJson(objectMapper, ruleHit.getTags()));
            entity.setEvidenceJson(JsonUtils.toJson(objectMapper, ruleHit.getEvidence()));
            caseRuleHitRepository.save(entity);
        }

        CaseAnalysisResult result = new CaseAnalysisResult();
        result.setCaseId(divinationCase.getId());
        result.setProvider("stub");
        result.setModelName("skeleton");
        result.setAnalysisText(analysis);
        result.setScore(structuredResult == null ? null : structuredResult.getScore());
        result.setResultLevel(structuredResult == null ? null : structuredResult.getResultLevel());
        result.setStructuredResultJson(JsonUtils.toJson(objectMapper, structuredResult));
        result.setAnalysisContextJson(JsonUtils.toJson(objectMapper, analysisContext));
        caseAnalysisResultRepository.save(result);
        log.info("案例留痕完成: caseId={}, category={}, mainHexagram={}, useGod={}, ruleHitCount={}",
                divinationCase.getId(),
                divinationCase.getQuestionCategory(),
                snapshot.getMainHexagram(),
                snapshot.getUseGod(),
                ruleHits.size());
        return new RecordedAnalysisRefs(divinationCase.getId(), snapshot.getId());
    }

    @Transactional(readOnly = true)
    public List<CaseSummaryDTO> listRecentCases() {
        return divinationCaseRepository.findTop20ByOrderByDivinationTimeDescIdDesc().stream()
                .map(this::toCaseSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseListResponseDTO listCases(String questionCategory, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        // 统一使用 1-based 页码对外，内部转成 Spring Data 的 0-based。
        PageRequest pageable = PageRequest.of(safePage - 1, safeSize);
        Page<DivinationCase> cases = questionCategory == null || questionCategory.isBlank()
                ? divinationCaseRepository.findAllByOrderByDivinationTimeDescIdDesc(pageable)
                : divinationCaseRepository.findByQuestionCategoryOrderByDivinationTimeDescIdDesc(questionCategory, pageable);

        CaseListResponseDTO response = new CaseListResponseDTO();
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotal(cases.getTotalElements());
        response.setItems(cases.getContent().stream().map(this::toCaseSummary).toList());
        log.info("案例分页查询完成: category={}, page={}, size={}, total={}, returned={}",
                questionCategory,
                safePage,
                safeSize,
                cases.getTotalElements(),
                response.getItems().size());
        if (log.isDebugEnabled()) {
            log.debug("案例分页查询摘要: caseIds={}",
                    response.getItems().stream().map(CaseSummaryDTO::getCaseId).toList());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public CaseDetailDTO getCaseDetail(Long caseId) {
        DivinationCase divinationCase = divinationCaseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "案例不存在"));

        CaseDetailDTO dto = new CaseDetailDTO();
        dto.setCaseId(divinationCase.getId());
        dto.setQuestionText(divinationCase.getQuestionText());
        dto.setQuestionCategory(divinationCase.getQuestionCategory());
        dto.setDivinationTime(divinationCase.getDivinationTime());
        dto.setStatus(divinationCase.getStatus());

        caseChartSnapshotRepository.findByCaseId(caseId)
                .ifPresent(snapshot -> dto.setChartSnapshot(readJson(snapshot.getChartJson(), ChartSnapshotDTO.class)));

        dto.setRuleHits(caseRuleHitRepository.findByCaseIdOrderByIdAsc(caseId).stream()
                .map(this::toRuleHitDto)
                .toList());

        caseAnalysisResultRepository.findTopByCaseIdOrderByIdDesc(caseId)
                .ifPresent(result -> {
                    dto.setAnalysis(result.getAnalysisText());
                    dto.setStructuredResult(readJson(result.getStructuredResultJson(), StructuredAnalysisResultDTO.class));
                    dto.setAnalysisContext(readJson(result.getAnalysisContextJson(), AnalysisContextDTO.class));
                });
        log.info("案例详情读取完成: caseId={}, category={}, status={}",
                dto.getCaseId(),
                dto.getQuestionCategory(),
                dto.getStatus());
        return dto;
    }

    @Transactional(readOnly = true)
    public CaseReplayAssessmentListDTO listReplayPersistenceAssessments(String questionCategory, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(safePage - 1, safeSize);
        Page<DivinationCase> cases = questionCategory == null || questionCategory.isBlank()
                ? divinationCaseRepository.findAllByOrderByDivinationTimeDescIdDesc(pageable)
                : divinationCaseRepository.findByQuestionCategoryOrderByDivinationTimeDescIdDesc(questionCategory, pageable);

        CaseReplayAssessmentListDTO response = new CaseReplayAssessmentListDTO();
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotal(cases.getTotalElements());
        response.setItems(cases.getContent().stream()
                .map(this::toReplayAssessment)
                .toList());
        return response;
    }

    @Transactional
    public CaseReplayRunDTO createReplayRun(Long caseId) {
        DivinationCase divinationCase = divinationCaseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "案例不存在"));
        CaseReplayDTO replay = replayCase(caseId);
        CaseReplayRun entity = new CaseReplayRun();
        entity.setCaseId(replay.getCaseId());
        entity.setQuestionText(divinationCase.getQuestionText());
        entity.setQuestionCategory(divinationCase.getQuestionCategory());
        entity.setRuleBundleVersion(replay.getRuleBundleVersion());
        entity.setRuleDefinitionsVersion(replay.getRuleDefinitionsVersion());
        entity.setUseGodRulesVersion(replay.getUseGodRulesVersion());
        entity.setBaselineRuleVersion(replay.getBaselineRuleVersion());
        entity.setReplayRuleVersion(replay.getReplayRuleVersion());
        entity.setBaselineUseGodConfigVersion(replay.getBaselineUseGodConfigVersion());
        entity.setReplayUseGodConfigVersion(replay.getReplayUseGodConfigVersion());
        entity.setRecommendPersistReplay(Boolean.TRUE.equals(replay.getRecommendPersistReplay()));
        entity.setPersistenceAssessment(replay.getPersistenceAssessment());
        entity.setScoreDelta(replay.getScoreDelta());
        entity.setEffectiveScoreDelta(replay.getEffectiveScoreDelta());
        entity.setResultLevelChanged(Boolean.TRUE.equals(replay.getResultLevelChanged()));
        entity.setSummaryChanged(Boolean.TRUE.equals(replay.getSummaryChanged()));
        entity.setAnalysisChanged(Boolean.TRUE.equals(replay.getAnalysisChanged()));
        entity.setPayloadJson(JsonUtils.toJson(objectMapper, replay));
        return toReplayRunDto(caseReplayRunRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<CaseReplayRunDTO> listReplayRuns(Long caseId) {
        if (!divinationCaseRepository.existsById(caseId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "案例不存在");
        }
        return caseReplayRunRepository.findByCaseIdOrderByIdDesc(caseId).stream()
                .map(this::toReplayRunDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseReplayRunListDTO listReplayRuns(String questionCategory, Boolean recommendPersistReplay, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(safePage - 1, safeSize);
        Page<CaseReplayRun> replayRuns = findReplayRuns(questionCategory, recommendPersistReplay, pageable);

        CaseReplayRunListDTO response = new CaseReplayRunListDTO();
        response.setPage(safePage);
        response.setSize(safeSize);
        response.setTotal(replayRuns.getTotalElements());
        response.setItems(replayRuns.getContent().stream().map(this::toReplayRunDto).toList());
        return response;
    }

    @Transactional(readOnly = true)
    public CaseReplayRunStatsDTO getReplayRunStats() {
        List<CaseReplayRun> replayRuns = caseReplayRunRepository.findAll();
        CaseReplayRunStatsDTO dto = new CaseReplayRunStatsDTO();
        dto.setTotalRuns(replayRuns.size());
        dto.setRecommendPersistRuns(replayRuns.stream()
                .filter(item -> Boolean.TRUE.equals(item.getRecommendPersistReplay()))
                .count());
        dto.setObserveOnlyRuns(dto.getTotalRuns() - dto.getRecommendPersistRuns());
        dto.setCategoryStats(replayRuns.stream()
                .collect(java.util.stream.Collectors.groupingBy(item -> {
                    String category = item.getQuestionCategory();
                    return category == null || category.isBlank() ? "未分类" : category;
                }))
                .entrySet()
                .stream()
                .map(entry -> toReplayRunCategoryStats(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(CaseReplayRunCategoryStatsDTO::getRunCount).reversed()
                        .thenComparing(CaseReplayRunCategoryStatsDTO::getQuestionCategory))
                .toList());
        return dto;
    }

    @Transactional(readOnly = true)
    public CaseReplayDTO replayCase(Long caseId) {
        DivinationCase divinationCase = divinationCaseRepository.findById(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "案例不存在"));
        CaseChartSnapshot snapshot = caseChartSnapshotRepository.findByCaseId(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "案例快照不存在"));
        CaseAnalysisResult baselineResult = caseAnalysisResultRepository.findTopByCaseIdOrderByIdDesc(caseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "案例分析结果不存在"));
        List<CaseRuleHit> baselineRuleHits = caseRuleHitRepository.findByCaseIdOrderByIdAsc(caseId);
        StructuredAnalysisResultDTO baselineStructuredResult = readJson(baselineResult.getStructuredResultJson(), StructuredAnalysisResultDTO.class);
        AnalysisContextDTO baselineAnalysisContext = readJson(baselineResult.getAnalysisContextJson(), AnalysisContextDTO.class);

        ChartSnapshot chartSnapshot = readJson(snapshot.getChartJson(), ChartSnapshot.class);
        RuleEvaluationResult replayEvaluation = ruleEngineService.evaluateResult(chartSnapshot);
        StructuredAnalysisResultDTO replayStructuredResult = toStructuredResultDto(replayEvaluation);
        AnalysisContextDTO replayContext = buildReplayAnalysisContext(divinationCase.getQuestionText(), chartSnapshot, replayEvaluation.getHits(), replayStructuredResult);
        String replayAnalysis = analysisService.analyze(replayContext);
        RuleResourceMetadata ruleMetadata = ruleResourceMetadataLoader.getMetadata();

        CaseReplayDTO dto = new CaseReplayDTO();
        dto.setCaseId(caseId);
        dto.setBaselineAnalysis(baselineResult.getAnalysisText());
        dto.setBaselineAnalysisContext(baselineAnalysisContext);
        dto.setBaselineStructuredResult(baselineStructuredResult);
        dto.setBaselineRuleHits(baselineRuleHits.stream().map(this::toRuleHitDto).toList());
        dto.setReplayAnalysis(replayAnalysis);
        dto.setReplayAnalysisContext(replayContext);
        dto.setReplayStructuredResult(replayStructuredResult);
        dto.setReplayRuleHits(toRuleHitDtos(replayEvaluation.getHits()));
        dto.setReplayRuleCodes(replayEvaluation.getHits().stream().map(RuleHit::getRuleCode).toList());
        dto.setBaselineRuleCodes(baselineRuleHits.stream().map(CaseRuleHit::getRuleCode).toList());
        dto.setAddedRuleCodes(dto.getReplayRuleCodes().stream().filter(code -> !dto.getBaselineRuleCodes().contains(code)).toList());
        dto.setRemovedRuleCodes(dto.getBaselineRuleCodes().stream().filter(code -> !dto.getReplayRuleCodes().contains(code)).toList());
        dto.setBaselineEffectiveRuleCodes(baselineStructuredResult == null || baselineStructuredResult.getEffectiveRuleCodes() == null
                ? List.of()
                : baselineStructuredResult.getEffectiveRuleCodes());
        dto.setReplayEffectiveRuleCodes(replayStructuredResult.getEffectiveRuleCodes() == null
                ? List.of()
                : replayStructuredResult.getEffectiveRuleCodes());
        dto.setAddedEffectiveRuleCodes(dto.getReplayEffectiveRuleCodes().stream()
                .filter(code -> !dto.getBaselineEffectiveRuleCodes().contains(code))
                .toList());
        dto.setRemovedEffectiveRuleCodes(dto.getBaselineEffectiveRuleCodes().stream()
                .filter(code -> !dto.getReplayEffectiveRuleCodes().contains(code))
                .toList());
        dto.setBaselineSuppressedRuleCodes(baselineStructuredResult == null || baselineStructuredResult.getSuppressedRuleCodes() == null
                ? List.of()
                : baselineStructuredResult.getSuppressedRuleCodes());
        dto.setReplaySuppressedRuleCodes(replayStructuredResult.getSuppressedRuleCodes() == null
                ? List.of()
                : replayStructuredResult.getSuppressedRuleCodes());
        dto.setAddedSuppressedRuleCodes(dto.getReplaySuppressedRuleCodes().stream()
                .filter(code -> !dto.getBaselineSuppressedRuleCodes().contains(code))
                .toList());
        dto.setRemovedSuppressedRuleCodes(dto.getBaselineSuppressedRuleCodes().stream()
                .filter(code -> !dto.getReplaySuppressedRuleCodes().contains(code))
                .toList());
        dto.setBaselineTags(baselineStructuredResult == null || baselineStructuredResult.getTags() == null
                ? List.of()
                : baselineStructuredResult.getTags());
        dto.setReplayTags(replayStructuredResult.getTags() == null
                ? List.of()
                : replayStructuredResult.getTags());
        dto.setAddedTags(dto.getReplayTags().stream()
                .filter(tag -> !dto.getBaselineTags().contains(tag))
                .toList());
        dto.setRemovedTags(dto.getBaselineTags().stream()
                .filter(tag -> !dto.getReplayTags().contains(tag))
                .toList());
        dto.setBaselineScore(baselineResult.getScore());
        dto.setReplayScore(replayStructuredResult.getScore());
        dto.setScoreDelta(safeInt(dto.getReplayScore()) - safeInt(dto.getBaselineScore()));
        dto.setBaselineEffectiveScore(baselineStructuredResult == null ? null : baselineStructuredResult.getEffectiveScore());
        dto.setReplayEffectiveScore(replayStructuredResult.getEffectiveScore());
        dto.setEffectiveScoreDelta(safeInt(dto.getReplayEffectiveScore()) - safeInt(dto.getBaselineEffectiveScore()));
        dto.setRuleBundleVersion(ruleMetadata.getBundleVersion());
        dto.setRuleDefinitionsVersion(ruleMetadata.getRuleDefinitionsVersion());
        dto.setUseGodRulesVersion(ruleMetadata.getUseGodRulesVersion());
        dto.setBaselineRuleVersion(resolveBaselineRuleVersion(baselineRuleHits));
        dto.setReplayRuleVersion(resolveReplayRuleVersion(replayEvaluation.getHits()));
        dto.setBaselineUseGodConfigVersion(resolveBaselineUseGodConfigVersion(baselineRuleHits));
        dto.setReplayUseGodConfigVersion(resolveReplayUseGodConfigVersion(replayEvaluation.getHits()));
        dto.setBaselineResultLevel(baselineResult.getResultLevel());
        dto.setReplayResultLevel(replayStructuredResult.getResultLevel());
        dto.setResultLevelChanged(!java.util.Objects.equals(dto.getBaselineResultLevel(), dto.getReplayResultLevel()));
        dto.setBaselineSummary(baselineStructuredResult == null ? null : baselineStructuredResult.getSummary());
        dto.setReplaySummary(replayStructuredResult.getSummary());
        dto.setSummaryChanged(!java.util.Objects.equals(dto.getBaselineSummary(), dto.getReplaySummary()));
        dto.setAnalysisChanged(!java.util.Objects.equals(dto.getBaselineAnalysis(), dto.getReplayAnalysis()));
        dto.setRecommendPersistReplay(shouldRecommendPersistReplay(dto));
        dto.setPersistenceAssessment(buildPersistenceAssessment(dto));
        return dto;
    }

    private boolean shouldRecommendPersistReplay(CaseReplayDTO dto) {
        if (dto == null) {
            return false;
        }
        return !safeList(dto.getAddedRuleCodes()).isEmpty()
                || !safeList(dto.getRemovedRuleCodes()).isEmpty()
                || !safeList(dto.getAddedEffectiveRuleCodes()).isEmpty()
                || !safeList(dto.getRemovedEffectiveRuleCodes()).isEmpty()
                || !safeList(dto.getAddedSuppressedRuleCodes()).isEmpty()
                || !safeList(dto.getRemovedSuppressedRuleCodes()).isEmpty()
                || !java.util.Objects.equals(dto.getBaselineRuleVersion(), dto.getReplayRuleVersion())
                || !java.util.Objects.equals(dto.getBaselineUseGodConfigVersion(), dto.getReplayUseGodConfigVersion())
                || safeInt(dto.getScoreDelta()) != 0
                || safeInt(dto.getEffectiveScoreDelta()) != 0
                || Boolean.TRUE.equals(dto.getResultLevelChanged())
                || Boolean.TRUE.equals(dto.getSummaryChanged())
                || Boolean.TRUE.equals(dto.getAnalysisChanged());
    }

    private String buildPersistenceAssessment(CaseReplayDTO dto) {
        if (dto == null) {
            return "缺少 replay 结果，暂不建议持久化。";
        }
        if (!Boolean.TRUE.equals(dto.getRecommendPersistReplay())) {
            return "当前 replay 与基线差异有限，建议继续只读观察，不必立即持久化。";
        }
        return "当前 replay 已出现规则、分数或分析差异，具备持久化评估价值，可作为 T1-2 的候选样本。";
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String resolveBaselineRuleVersion(List<CaseRuleHit> baselineRuleHits) {
        return baselineRuleHits.stream()
                .map(CaseRuleHit::getEvidenceJson)
                .map(this::readEvidence)
                .map(evidence -> evidence == null ? null : evidence.get("ruleVersion"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
    }

    private String resolveReplayRuleVersion(List<RuleHit> replayRuleHits) {
        return replayRuleHits.stream()
                .map(RuleHit::getEvidence)
                .map(evidence -> evidence == null ? null : evidence.get("ruleVersion"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
    }

    private String resolveBaselineUseGodConfigVersion(List<CaseRuleHit> baselineRuleHits) {
        return baselineRuleHits.stream()
                .filter(hit -> "USE_GOD_SELECTION".equals(hit.getRuleCode()))
                .map(CaseRuleHit::getEvidenceJson)
                .map(this::readEvidence)
                .map(evidence -> evidence == null ? null : evidence.get("configVersion"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
    }

    private String resolveReplayUseGodConfigVersion(List<RuleHit> replayRuleHits) {
        return replayRuleHits.stream()
                .filter(hit -> "USE_GOD_SELECTION".equals(hit.getRuleCode()))
                .map(RuleHit::getEvidence)
                .map(evidence -> evidence == null ? null : evidence.get("configVersion"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null);
    }

    private CaseSummaryDTO toCaseSummary(DivinationCase divinationCase) {
        CaseSummaryDTO dto = new CaseSummaryDTO();
        dto.setCaseId(divinationCase.getId());
        dto.setQuestionText(divinationCase.getQuestionText());
        dto.setQuestionCategory(divinationCase.getQuestionCategory());
        dto.setDivinationTime(divinationCase.getDivinationTime());
        dto.setStatus(divinationCase.getStatus());

        caseChartSnapshotRepository.findByCaseId(divinationCase.getId()).ifPresent(snapshot -> {
            dto.setMainHexagram(snapshot.getMainHexagram());
            dto.setChangedHexagram(snapshot.getChangedHexagram());
            dto.setPalace(snapshot.getPalace());
            dto.setUseGod(snapshot.getUseGod());
        });
        return dto;
    }

    private CaseReplayAssessmentDTO toReplayAssessment(DivinationCase divinationCase) {
        CaseReplayDTO replay = replayCase(divinationCase.getId());
        CaseReplayAssessmentDTO dto = new CaseReplayAssessmentDTO();
        dto.setCaseId(divinationCase.getId());
        dto.setQuestionText(divinationCase.getQuestionText());
        dto.setQuestionCategory(divinationCase.getQuestionCategory());
        dto.setDivinationTime(divinationCase.getDivinationTime());
        dto.setRecommendPersistReplay(replay.getRecommendPersistReplay());
        dto.setPersistenceAssessment(replay.getPersistenceAssessment());
        dto.setRuleBundleVersion(replay.getRuleBundleVersion());
        dto.setReplayRuleVersion(replay.getReplayRuleVersion());
        dto.setScoreDelta(replay.getScoreDelta());
        dto.setEffectiveScoreDelta(replay.getEffectiveScoreDelta());
        return dto;
    }

    private CaseReplayRunDTO toReplayRunDto(CaseReplayRun entity) {
        CaseReplayRunDTO dto = new CaseReplayRunDTO();
        dto.setReplayRunId(entity.getId());
        dto.setCaseId(entity.getCaseId());
        dto.setQuestionText(entity.getQuestionText());
        dto.setQuestionCategory(entity.getQuestionCategory());
        dto.setRuleBundleVersion(entity.getRuleBundleVersion());
        dto.setRuleDefinitionsVersion(entity.getRuleDefinitionsVersion());
        dto.setUseGodRulesVersion(entity.getUseGodRulesVersion());
        dto.setBaselineRuleVersion(entity.getBaselineRuleVersion());
        dto.setReplayRuleVersion(entity.getReplayRuleVersion());
        dto.setBaselineUseGodConfigVersion(entity.getBaselineUseGodConfigVersion());
        dto.setReplayUseGodConfigVersion(entity.getReplayUseGodConfigVersion());
        dto.setRecommendPersistReplay(entity.getRecommendPersistReplay());
        dto.setPersistenceAssessment(entity.getPersistenceAssessment());
        dto.setScoreDelta(entity.getScoreDelta());
        dto.setEffectiveScoreDelta(entity.getEffectiveScoreDelta());
        dto.setResultLevelChanged(entity.getResultLevelChanged());
        dto.setSummaryChanged(entity.getSummaryChanged());
        dto.setAnalysisChanged(entity.getAnalysisChanged());
        dto.setPayloadJson(entity.getPayloadJson());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private Page<CaseReplayRun> findReplayRuns(String questionCategory, Boolean recommendPersistReplay, PageRequest pageable) {
        if (questionCategory != null && !questionCategory.isBlank() && recommendPersistReplay != null) {
            return caseReplayRunRepository.findByQuestionCategoryAndRecommendPersistReplayOrderByIdDesc(
                    questionCategory,
                    recommendPersistReplay,
                    pageable
            );
        }
        if (questionCategory != null && !questionCategory.isBlank()) {
            return caseReplayRunRepository.findByQuestionCategoryOrderByIdDesc(questionCategory, pageable);
        }
        if (recommendPersistReplay != null) {
            return caseReplayRunRepository.findByRecommendPersistReplayOrderByIdDesc(recommendPersistReplay, pageable);
        }
        return caseReplayRunRepository.findAllByOrderByIdDesc(pageable);
    }

    private CaseReplayRunCategoryStatsDTO toReplayRunCategoryStats(String questionCategory, List<CaseReplayRun> replayRuns) {
        CaseReplayRunCategoryStatsDTO dto = new CaseReplayRunCategoryStatsDTO();
        dto.setQuestionCategory(questionCategory);
        dto.setRunCount(replayRuns.size());
        dto.setRecommendPersistRuns(replayRuns.stream()
                .filter(item -> Boolean.TRUE.equals(item.getRecommendPersistReplay()))
                .count());
        dto.setObserveOnlyRuns(dto.getRunCount() - dto.getRecommendPersistRuns());
        dto.setLatestReplayTime(replayRuns.stream()
                .map(CaseReplayRun::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        return dto;
    }

    private RuleHitDTO toRuleHitDto(CaseRuleHit entity) {
        RuleHitDTO dto = new RuleHitDTO();
        dto.setRuleId(entity.getRuleId());
        dto.setRuleCode(entity.getRuleCode());
        dto.setRuleName(entity.getRuleName());
        dto.setPriority(entity.getPriority());
        dto.setHitReason(entity.getHitReason());
        dto.setImpactLevel(entity.getImpactLevel());
        dto.setCategory(entity.getCategory());
        dto.setScoreDelta(entity.getScoreDelta());
        dto.setTags(readStringList(entity.getTagsJson()));
        dto.setEvidence(readEvidence(entity.getEvidenceJson()));
        return dto;
    }

    private List<RuleHitDTO> toRuleHitDtos(List<RuleHit> ruleHits) {
        return ruleHits == null ? List.of() : ruleHits.stream().map(this::toRuleHitDto).toList();
    }

    private RuleHitDTO toRuleHitDto(RuleHit ruleHit) {
        RuleHitDTO dto = new RuleHitDTO();
        dto.setRuleId(ruleHit.getRuleId());
        dto.setRuleCode(ruleHit.getRuleCode());
        dto.setRuleName(ruleHit.getRuleName());
        dto.setPriority(ruleHit.getPriority());
        dto.setHitReason(ruleHit.getHitReason());
        dto.setImpactLevel(ruleHit.getImpactLevel());
        dto.setCategory(ruleHit.getCategory());
        dto.setScoreDelta(ruleHit.getScoreDelta());
        dto.setTags(ruleHit.getTags());
        dto.setEvidence(ruleHit.getEvidence());
        return dto;
    }

    private AnalysisContextDTO buildReplayAnalysisContext(String question,
                                                          ChartSnapshot chartSnapshot,
                                                          List<RuleHit> ruleHits,
                                                          StructuredAnalysisResultDTO structuredResult) {
        AnalysisContextDTO context = analysisContextFactory.create(question, chartSnapshot, ruleHits);
        context.setChartSnapshot(objectMapper.convertValue(chartSnapshot, ChartSnapshotDTO.class));
        context.setRuleHits(toRuleHitDtos(ruleHits));
        context.setStructuredResult(structuredResult);
        context.setKnowledgeSnippets(knowledgeSearchService.suggestKnowledgeSnippets(
                chartSnapshot.getQuestionCategory(),
                chartSnapshot.getUseGod(),
                context.getRuleCodes(),
                6
        ));
        return context;
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

    private RuleCategorySummaryDTO toCategorySummaryDto(Map<String, Object> item) {
        RuleCategorySummaryDTO dto = new RuleCategorySummaryDTO();
        dto.setCategory(String.valueOf(item.getOrDefault("category", "GENERAL")));
        dto.setHitCount(asInteger(item.get("hitCount")));
        dto.setScore(asInteger(item.get("score")));
        dto.setEffectiveHitCount(asInteger(item.get("effectiveHitCount")));
        dto.setEffectiveScore(asInteger(item.get("effectiveScore")));
        dto.setStageOrder(asInteger(item.get("stageOrder")));
        return dto;
    }

    private RuleConflictSummaryDTO toConflictSummaryDto(Map<String, Object> item) {
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

    private Map<String, Object> readEvidence(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(evidenceJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("JSON deserializing failed", exception);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("JSON deserializing failed", exception);
        }
    }

    private <T> T readJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON deserializing failed", exception);
        }
    }

    public record RecordedAnalysisRefs(Long caseId, Long snapshotId) {
    }
}
