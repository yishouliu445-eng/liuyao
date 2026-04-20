package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.analysis.runtime.AnalysisExecutionEnvelope;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionMode;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionService;
import com.yishou.liuyao.casecenter.service.CaseCenterService;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeResponse;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.divination.dto.LineInfoDTO;
import com.yishou.liuyao.divination.dto.ShenShaHitDTO;
import com.yishou.liuyao.divination.domain.ShenShaHit;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.dto.RuleHitDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DivinationService {

    private static final Logger log = LoggerFactory.getLogger(DivinationService.class);

    private final AnalysisExecutionService analysisExecutionService;
    private final CaseCenterService caseCenterService;

    public DivinationService(AnalysisExecutionService analysisExecutionService,
                             CaseCenterService caseCenterService) {
        this.analysisExecutionService = analysisExecutionService;
        this.caseCenterService = caseCenterService;
    }

    public DivinationAnalyzeResponse analyze(DivinationAnalyzeRequest request) {
        log.info("旧版分析接口转发到统一 execution runtime: category={}, time={}, rawLineCount={}, movingLineCount={}",
                request.getQuestionCategory(),
                request.getDivinationTime(),
                request.getRawLines() == null ? 0 : request.getRawLines().size(),
                request.getMovingLines() == null ? 0 : request.getMovingLines().size());
        AnalysisExecutionEnvelope execution = analysisExecutionService.executeInitial(request, AnalysisExecutionMode.LEGACY_COMPAT);
        recordCaseIfPossible(request, execution);
        DivinationAnalyzeResponse response = new DivinationAnalyzeResponse(
                toChartSnapshotDto(execution.getChartSnapshot()),
                toRuleHitDtos(execution.getRuleHits()),
                execution.getLegacyAnalysisText(),
                execution.getAnalysisContext(),
                execution.getStructuredResult()
        );
        response.setExecutionId(execution.getExecutionId());
        return response;
    }

    private void recordCaseIfPossible(DivinationAnalyzeRequest request, AnalysisExecutionEnvelope execution) {
        try {
            caseCenterService.recordAnalysis(
                    request,
                    execution.getChartSnapshot(),
                    execution.getRuleHits(),
                    execution.getAnalysisContext(),
                    execution.getStructuredResult(),
                    execution.getLegacyAnalysisText()
            );
        } catch (Exception exception) {
            log.warn("旧版分析接口留痕失败（不影响主流程）: {}", exception.getMessage());
        }
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
}
