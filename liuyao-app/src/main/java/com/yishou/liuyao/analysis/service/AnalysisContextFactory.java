package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalysisContextFactory {

    public AnalysisContextDTO create(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setContextVersion("v1");
        context.setQuestion(question);
        if (chartSnapshot != null) {
            context.setQuestionCategory(chartSnapshot.getQuestionCategory());
            context.setUseGod(chartSnapshot.getUseGod());
            context.setMainHexagram(chartSnapshot.getMainHexagram());
            context.setChangedHexagram(chartSnapshot.getChangedHexagram());
        }
        context.setRuleCount(ruleHits == null ? 0 : ruleHits.size());
        context.setRuleCodes(ruleHits == null ? List.of() : ruleHits.stream().map(RuleHit::getRuleCode).toList());
        return context;
    }
}
