package com.yishou.liuyao.analysis.validation;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SchemaValidationStage implements AnalysisValidationStage {

    @Override
    public List<AnalysisValidationIssue> validate(AnalysisOutputDTO output,
                                                  StructuredAnalysisResultDTO structuredResult,
                                                  List<EvidenceHit> evidenceHits,
                                                  List<String> knowledgeSnippets,
                                                  String questionCategory) {
        List<AnalysisValidationIssue> issues = new ArrayList<>();
        if (output == null || output.getAnalysis() == null) {
            issues.add(new AnalysisValidationIssue("SCHEMA_REQUIRED_FIELDS_MISSING", "缺少 analysis 主体"));
            return issues;
        }
        if (isBlank(output.getAnalysis().getConclusion())
                || output.getAnalysis().getActionPlan() == null
                || output.getAnalysis().getActionPlan().isEmpty()
                || isBlank(output.getAnalysis().getEmotionalTone())) {
            issues.add(new AnalysisValidationIssue("SCHEMA_REQUIRED_FIELDS_MISSING", "analysis 必填字段不完整"));
        }
        return issues;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
