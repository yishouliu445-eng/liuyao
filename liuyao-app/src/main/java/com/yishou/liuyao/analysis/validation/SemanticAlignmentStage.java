package com.yishou.liuyao.analysis.validation;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SemanticAlignmentStage implements AnalysisValidationStage {

    @Override
    public List<AnalysisValidationIssue> validate(AnalysisOutputDTO output,
                                                  StructuredAnalysisResultDTO structuredResult,
                                                  List<EvidenceHit> evidenceHits,
                                                  List<String> knowledgeSnippets,
                                                  String questionCategory) {
        String expected = normalizeDirection(structuredResult == null ? null : structuredResult.getEffectiveResultLevel());
        String actual = inferDirection(output);
        if (expected == null || actual == null || "NEUTRAL".equals(expected) || "NEUTRAL".equals(actual)) {
            return List.of();
        }
        if (!expected.equals(actual)) {
            return List.of(new AnalysisValidationIssue(
                    "RESULT_DIRECTION_CONFLICT",
                    "结论方向与结构化结果不一致"
            ));
        }
        return List.of();
    }

    private String inferDirection(AnalysisOutputDTO output) {
        if (output == null || output.getAnalysis() == null || output.getAnalysis().getConclusion() == null) {
            return null;
        }
        String conclusion = output.getAnalysis().getConclusion();
        if (containsAny(conclusion, "失败", "放弃", "阻力偏大", "不利", "恶化")) {
            return "NEGATIVE";
        }
        if (containsAny(conclusion, "顺利", "推进", "机会", "可成", "向好")) {
            return "POSITIVE";
        }
        return "NEUTRAL";
    }

    private String normalizeDirection(String resultLevel) {
        if (resultLevel == null || resultLevel.isBlank()) {
            return null;
        }
        return switch (resultLevel) {
            case "POSITIVE", "GOOD" -> "POSITIVE";
            case "BAD", "NEGATIVE" -> "NEGATIVE";
            default -> "NEUTRAL";
        };
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
