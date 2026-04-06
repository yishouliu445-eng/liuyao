package com.yishou.liuyao.casecenter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.casecenter.domain.CaseAnalysisResult;
import com.yishou.liuyao.casecenter.domain.CaseChartSnapshot;
import com.yishou.liuyao.casecenter.domain.CaseRuleHit;
import com.yishou.liuyao.casecenter.domain.DivinationCase;
import com.yishou.liuyao.casecenter.dto.CaseDetailDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CaseCenterService {

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
            entity.setRuleCode(ruleHit.getRuleCode());
            entity.setRuleName(ruleHit.getRuleName());
            entity.setHitReason(ruleHit.getHitReason());
            entity.setImpactLevel(ruleHit.getImpactLevel());
            entity.setEvidenceJson(JsonUtils.toJson(objectMapper, ruleHit.getEvidence()));
            caseRuleHitRepository.save(entity);
        }

        CaseAnalysisResult result = new CaseAnalysisResult();
        result.setCaseId(divinationCase.getId());
        result.setProvider("stub");
        result.setModelName("skeleton");
        result.setAnalysisText(analysis);
        caseAnalysisResultRepository.save(result);
    }

    @Transactional(readOnly = true)
    public List<CaseSummaryDTO> listRecentCases() {
        return divinationCaseRepository.findTop20ByOrderByDivinationTimeDescIdDesc().stream()
                .map(this::toCaseSummary)
                .toList();
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
                .ifPresent(result -> dto.setAnalysis(result.getAnalysisText()));
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
        dto.setRuleCode(entity.getRuleCode());
        dto.setRuleName(entity.getRuleName());
        dto.setHitReason(entity.getHitReason());
        dto.setImpactLevel(entity.getImpactLevel());
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
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception exception) {
            throw new IllegalStateException("JSON deserializing failed", exception);
        }
    }
}
