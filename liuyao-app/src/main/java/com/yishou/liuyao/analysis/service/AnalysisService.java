package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisService {

    private final AnalysisContextFactory contextFactory;
    private final AnalysisSectionComposer sectionComposer;

    public AnalysisService() {
        this(new AnalysisContextFactory(),
                new AnalysisSectionComposer(
                        new AnalysisKnowledgeEvidenceService(),
                        new AnalysisCategoryTextResolver(),
                        new AnalysisOutcomeTextResolver()
                ));
    }

    AnalysisService(AnalysisContextFactory contextFactory, AnalysisSectionComposer sectionComposer) {
        this.contextFactory = contextFactory;
        this.sectionComposer = sectionComposer;
    }

    public String analyze(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        AnalysisContextDTO context = buildContext(question, chartSnapshot, ruleHits);
        return analyze(context);
    }

    public String analyze(AnalysisContextDTO context) {
        return sectionComposer.compose(context);
    }

    private AnalysisContextDTO buildContext(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        return contextFactory.create(question, chartSnapshot, ruleHits);
    }

}
