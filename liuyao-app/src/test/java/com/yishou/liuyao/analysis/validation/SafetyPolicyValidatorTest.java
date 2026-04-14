package com.yishou.liuyao.analysis.validation;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafetyPolicyValidatorTest {

    @Test
    void shouldFlagAbsoluteMedicalStatementsForHealthQuestions() {
        AnalysisValidationPipeline pipeline = new AnalysisValidationPipeline(List.of(new SafetyPolicyStage()));

        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setConclusion("这个病一定会恶化，已经没希望了。");
        analysis.setActionPlan(List.of("什么都不要做"));
        analysis.setEmotionalTone("CAUTIOUS");

        AnalysisOutputDTO output = new AnalysisOutputDTO();
        output.setAnalysis(analysis);

        AnalysisValidationResult result = pipeline.validate(
                output,
                new StructuredAnalysisResultDTO(),
                List.of(),
                List.of(),
                "健康"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasIssueCode("SAFETY_SENSITIVE_ABSOLUTE"));
    }
}
