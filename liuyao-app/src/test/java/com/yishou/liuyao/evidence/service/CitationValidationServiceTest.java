package com.yishou.liuyao.evidence.service;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationValidationServiceTest {

    private final CitationValidationService service = new CitationValidationService();

    @Test
    void shouldEnrichClassicReferencesWithCitationIdsFromEvidenceHits() {
        AnalysisOutputDTO.ClassicReference reference = new AnalysisOutputDTO.ClassicReference();
        reference.setSource("《增删卜易·用神总论》");
        reference.setQuote("用神旺相则事可成。");
        reference.setRelevance("用神得势，与本卦判断一致。");

        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setClassicReferences(List.of(reference));

        AnalysisOutputDTO output = new AnalysisOutputDTO();
        output.setAnalysis(analysis);

        EvidenceHit hit = new EvidenceHit();
        hit.setCitationId("chunk:21");
        hit.setChunkId(21L);
        hit.setBookId(1L);
        hit.setSourceTitle("增删卜易");
        hit.setChapterTitle("用神总论");
        hit.setContent("用神旺相则事可成。");

        CitationValidationResult result = service.enrichAndValidate(output, List.of(hit));

        assertTrue(result.isValid());
        assertEquals("chunk:21", output.getAnalysis().getClassicReferences().get(0).getCitationId());
        assertEquals(21L, output.getAnalysis().getClassicReferences().get(0).getChunkId());
        assertEquals(1L, output.getAnalysis().getClassicReferences().get(0).getBookId());
    }

    @Test
    void shouldRejectClassicReferencesThatCannotMapBackToEvidenceHits() {
        AnalysisOutputDTO.ClassicReference reference = new AnalysisOutputDTO.ClassicReference();
        reference.setSource("《卜筮正宗·世应章》");
        reference.setQuote("世应相合则吉。");
        reference.setRelevance("引用用于辅助判断。");

        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setClassicReferences(List.of(reference));

        AnalysisOutputDTO output = new AnalysisOutputDTO();
        output.setAnalysis(analysis);

        EvidenceHit hit = new EvidenceHit();
        hit.setCitationId("chunk:21");
        hit.setChunkId(21L);
        hit.setBookId(1L);
        hit.setSourceTitle("增删卜易");
        hit.setChapterTitle("用神总论");
        hit.setContent("用神旺相则事可成。");

        CitationValidationResult result = service.enrichAndValidate(output, List.of(hit));

        assertFalse(result.isValid());
        assertEquals(1, result.getUnmatchedReferences().size());
    }
}
