package com.yishou.liuyao.analysis.validation;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class SafetyPolicyStage implements AnalysisValidationStage {

    private static final Set<String> SENSITIVE_CATEGORIES = Set.of("健康", "官司");

    @Override
    public List<AnalysisValidationIssue> validate(AnalysisOutputDTO output,
                                                  StructuredAnalysisResultDTO structuredResult,
                                                  List<EvidenceHit> evidenceHits,
                                                  List<String> knowledgeSnippets,
                                                  String questionCategory) {
        if (!SENSITIVE_CATEGORIES.contains(questionCategory)) {
            return List.of();
        }
        String text = buildText(output);
        if (containsAny(text, "一定会", "没希望", "必然", "注定", "绝对", "败诉", "恶化")) {
            return List.of(new AnalysisValidationIssue(
                    "SAFETY_SENSITIVE_ABSOLUTE",
                    "高敏感问题中出现绝对化结论",
                    "BLOCK"
            ));
        }
        return List.of();
    }

    private String buildText(AnalysisOutputDTO output) {
        if (output == null || output.getAnalysis() == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        append(builder, output.getAnalysis().getConclusion());
        append(builder, output.getAnalysis().getDetailedReasoning());
        if (output.getAnalysis().getActionPlan() != null) {
            output.getAnalysis().getActionPlan().forEach(item -> append(builder, item));
        }
        return builder.toString();
    }

    private void append(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(value).append(' ');
        }
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
