package com.yishou.liuyao.analysis.validation;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisValidationPipeline {

    private final List<AnalysisValidationStage> stages;

    public AnalysisValidationPipeline(List<AnalysisValidationStage> stages) {
        this.stages = stages == null ? List.of() : stages;
    }

    public AnalysisValidationResult validate(AnalysisOutputDTO output,
                                             StructuredAnalysisResultDTO structuredResult,
                                             List<EvidenceHit> evidenceHits,
                                             List<String> knowledgeSnippets,
                                             String questionCategory) {
        List<AnalysisValidationIssue> issues = new ArrayList<>();
        for (AnalysisValidationStage stage : stages) {
            issues.addAll(stage.validate(output, structuredResult, evidenceHits, knowledgeSnippets, questionCategory));
        }
        AnalysisValidationResult result = new AnalysisValidationResult();
        result.setIssues(issues);
        return result;
    }
}
