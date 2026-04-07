package com.yishou.liuyao.casecenter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.casecenter.domain.CaseAnalysisResult;
import com.yishou.liuyao.casecenter.domain.CaseChartSnapshot;
import com.yishou.liuyao.casecenter.domain.CaseRuleHit;
import com.yishou.liuyao.casecenter.domain.DivinationCase;
import com.yishou.liuyao.casecenter.dto.CaseDetailDTO;
import com.yishou.liuyao.casecenter.dto.CaseListResponseDTO;
import com.yishou.liuyao.casecenter.dto.CaseSummaryDTO;
import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.casecenter.repository.CaseAnalysisResultRepository;
import com.yishou.liuyao.casecenter.repository.CaseChartSnapshotRepository;
import com.yishou.liuyao.casecenter.repository.CaseRuleHitRepository;
import com.yishou.liuyao.casecenter.repository.DivinationCaseRepository;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.infrastructure.util.JsonUtils;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.dto.RuleHitDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CaseCenterService {

    private static final Logger log = LoggerFactory.getLogger(CaseCenterService.class);

    private final DivinationCaseRepository divinationCaseRepository;
    private final CaseChartSnapshotRepository caseChartSnapshotRepository;
    private final CaseRuleHitRepository caseRuleHitRepository;
    private final CaseAnalysisResultRepository caseAnalysisResultRepository;
    private final ObjectMapper objectMapper;

    public CaseCenterService(DivinationCaseRepository divinationCaseRepository,
                             CaseChartSnapshotRepository caseChartSnapshotRepository,
                             CaseRuleHitRepository caseRuleHitRepository,
                             CaseAnalysisResultRepository caseAnalysisResultRepository,
                             ObjectMapper objectMapper) {
        this.divinationCaseRepository = divinationCaseRepository;
        this.caseChartSnapshotRepository = caseChartSnapshotRepository;
        this.caseRuleHitRepository = caseRuleHitRepository;
        this.caseAnalysisResultRepository = caseAnalysisResultRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordAnalysis(DivinationAnalyzeRequest request,
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
        dto.setTags(readJson(entity.getTagsJson(), List.class));
        dto.setEvidence(readEvidence(entity.getEvidenceJson()));
        return dto;
    }

    private Map<String, Object> readEvidence(String evidenceJson) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return Collections.emptyMap();
        }
        return readJson(evidenceJson, Map.class);
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
}
