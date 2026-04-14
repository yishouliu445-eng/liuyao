package com.yishou.liuyao.evidence.service;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CitationValidationService {

    public CitationValidationResult enrichAndValidate(AnalysisOutputDTO output, List<EvidenceHit> hits) {
        CitationValidationResult result = new CitationValidationResult();
        List<AnalysisOutputDTO.ClassicReference> unmatched = new ArrayList<>();
        if (output == null || output.getAnalysis() == null || output.getAnalysis().getClassicReferences() == null) {
            result.setValid(true);
            return result;
        }

        for (AnalysisOutputDTO.ClassicReference reference : output.getAnalysis().getClassicReferences()) {
            EvidenceHit matchedHit = match(reference, hits);
            if (matchedHit == null) {
                unmatched.add(reference);
                continue;
            }
            reference.setCitationId(matchedHit.getCitationId());
            reference.setChunkId(matchedHit.getChunkId());
            reference.setBookId(matchedHit.getBookId());
        }
        result.setUnmatchedReferences(unmatched);
        result.setValid(unmatched.isEmpty());
        return result;
    }

    private EvidenceHit match(AnalysisOutputDTO.ClassicReference reference, List<EvidenceHit> hits) {
        if (reference == null || hits == null || hits.isEmpty()) {
            return null;
        }
        String source = normalize(reference.getSource());
        String quote = normalize(reference.getQuote());
        for (EvidenceHit hit : hits) {
            if (hit == null) {
                continue;
            }
            boolean sourceMatched = source.contains(normalize(hit.getSourceTitle()))
                    || source.contains(normalize(hit.getChapterTitle()));
            boolean quoteMatched = quote.isBlank() || normalize(hit.getContent()).contains(quote);
            if (sourceMatched && quoteMatched) {
                return hit;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replace("《", "").replace("》", "").replace("·", "").replace(" ", "").trim();
    }
}
