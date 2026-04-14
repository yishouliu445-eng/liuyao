package com.yishou.liuyao.analysis.validation;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisValidationPipelineTest {

    @Test
    void shouldFlagDirectionConflictAgainstStructuredResult() {
        AnalysisValidationPipeline pipeline = new AnalysisValidationPipeline(List.of(
                new SchemaValidationStage(),
                new SemanticAlignmentStage(),
                new CitationValidationStage(),
                new SafetyPolicyStage()
        ));

        StructuredAnalysisResultDTO structuredResult = new StructuredAnalysisResultDTO();
        structuredResult.setEffectiveResultLevel("POSITIVE");

        AnalysisOutputDTO output = new AnalysisOutputDTO();
        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setConclusion("综合来看，这件事大概率会失败，建议尽快放弃。");
        analysis.setActionPlan(List.of("暂停推进"));
        analysis.setEmotionalTone("CAUTIOUS");
        output.setAnalysis(analysis);

        AnalysisValidationResult result = pipeline.validate(
                output,
                structuredResult,
                List.of(),
                List.of(),
                "合作"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasIssueCode("RESULT_DIRECTION_CONFLICT"));
    }

    @Test
    void shouldFlagCitationMismatchWhenClassicReferenceCannotTraceToEvidence() {
        AnalysisValidationPipeline pipeline = new AnalysisValidationPipeline(List.of(
                new SchemaValidationStage(),
                new SemanticAlignmentStage(),
                new CitationValidationStage(),
                new SafetyPolicyStage()
        ));

        AnalysisOutputDTO.ClassicReference reference = new AnalysisOutputDTO.ClassicReference();
        reference.setSource("《增删卜易·用神章》");
        reference.setQuote("用神旺相则事可成");
        reference.setRelevance("用于说明走势向好");

        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setConclusion("走势仍可推进。");
        analysis.setActionPlan(List.of("继续推进"));
        analysis.setEmotionalTone("ENCOURAGING");
        analysis.setClassicReferences(List.of(reference));

        AnalysisOutputDTO output = new AnalysisOutputDTO();
        output.setAnalysis(analysis);

        EvidenceHit hit = new EvidenceHit();
        hit.setCitationId("chunk:21");
        hit.setChunkId(21L);
        hit.setBookId(1L);
        hit.setSourceTitle("卜筮正宗");
        hit.setChapterTitle("世应章");
        hit.setContent("世应相接，则看对方回应与来往节度。");

        AnalysisValidationResult result = pipeline.validate(
                output,
                new StructuredAnalysisResultDTO(),
                List.of(hit),
                List.of(),
                "合作"
        );

        assertFalse(result.isValid());
        assertTrue(result.hasIssueCode("CITATION_MISMATCH"));
    }
}
