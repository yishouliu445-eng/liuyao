package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Deprecated(since = "2.0", forRemoval = false)
public class AnalysisService {

    private final AnalysisContextFactory contextFactory;
    private final AnalysisSectionComposer sectionComposer;
    private final LlmExpressionClient llmExpressionClient;

    public AnalysisService(AnalysisContextFactory contextFactory, 
                           AnalysisSectionComposer sectionComposer,
                           LlmExpressionClient llmExpressionClient) {
        this.contextFactory = contextFactory;
        this.sectionComposer = sectionComposer;
        this.llmExpressionClient = llmExpressionClient;
    }

    public String analyze(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        AnalysisContextDTO context = buildContext(question, chartSnapshot, ruleHits);
        return analyze(context);
    }

    public String analyze(AnalysisContextDTO context) {
        String mechanicalText = sectionComposer.compose(context);
        String question = context == null ? null : context.getQuestion();
        
        if (llmExpressionClient != null) {
            String refinedText = llmExpressionClient.refine(mechanicalText, question);
            if (refinedText != null && !refinedText.isBlank()) {
                return refinedText;
            }
        }
        
        return mechanicalText;
    }

    private AnalysisContextDTO buildContext(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        return contextFactory.create(question, chartSnapshot, ruleHits);
    }

}
