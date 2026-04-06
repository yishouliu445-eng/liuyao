package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.analysis.service.AnalysisService;
import com.yishou.liuyao.casecenter.service.CaseCenterService;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeResponse;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.divination.dto.LineInfoDTO;
import com.yishou.liuyao.divination.mapper.DivinationMapper;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.dto.RuleHitDTO;
import com.yishou.liuyao.rule.service.RuleEngineService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DivinationService {

    private final DivinationMapper divinationMapper;
    private final ChartBuilderService chartBuilderService;
    private final RuleEngineService ruleEngineService;
    private final AnalysisService analysisService;
    private final CaseCenterService caseCenterService;

    public DivinationService(DivinationMapper divinationMapper,
                             ChartBuilderService chartBuilderService,
                             RuleEngineService ruleEngineService,
                             AnalysisService analysisService,
                             CaseCenterService caseCenterService) {
        this.divinationMapper = divinationMapper;
        this.chartBuilderService = chartBuilderService;
        this.ruleEngineService = ruleEngineService;
        this.analysisService = analysisService;
        this.caseCenterService = caseCenterService;
    }

    public DivinationAnalyzeResponse analyze(DivinationAnalyzeRequest request) {
        DivinationInput input = divinationMapper.toInput(request);
        ChartSnapshot chartSnapshot = chartBuilderService.buildChart(input);
        List<RuleHit> ruleHits = ruleEngineService.evaluate(chartSnapshot);
        String analysis = analysisService.analyze(request.getQuestionText(), chartSnapshot, ruleHits);
        caseCenterService.recordAnalysis(request, chartSnapshot, ruleHits, analysis);
        return new DivinationAnalyzeResponse(toChartSnapshotDto(chartSnapshot), toRuleHitDtos(ruleHits), analysis);
    }

    private List<RuleHitDTO> toRuleHitDtos(List<RuleHit> ruleHits) {
        return ruleHits.stream().map(this::toRuleHitDto).toList();
    }

    private RuleHitDTO toRuleHitDto(RuleHit ruleHit) {
        RuleHitDTO dto = new RuleHitDTO();
        dto.setRuleCode(ruleHit.getRuleCode());
        dto.setRuleName(ruleHit.getRuleName());
        dto.setHitReason(ruleHit.getHitReason());
        dto.setImpactLevel(ruleHit.getImpactLevel());
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
        dto.setPalace(chartSnapshot.getPalace());
        dto.setPalaceWuXing(chartSnapshot.getPalaceWuXing());
        dto.setShi(chartSnapshot.getShi());
        dto.setYing(chartSnapshot.getYing());
        dto.setUseGod(chartSnapshot.getUseGod());
        dto.setRiChen(chartSnapshot.getRiChen());
        dto.setYueJian(chartSnapshot.getYueJian());
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
