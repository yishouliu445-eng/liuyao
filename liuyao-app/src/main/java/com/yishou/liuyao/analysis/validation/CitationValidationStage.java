package com.yishou.liuyao.analysis.validation;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import com.yishou.liuyao.evidence.service.CitationValidationResult;
import com.yishou.liuyao.evidence.service.CitationValidationService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CitationValidationStage implements AnalysisValidationStage {

    private final CitationValidationService citationValidationService;

    public CitationValidationStage() {
        this(new CitationValidationService());
    }

    public CitationValidationStage(CitationValidationService citationValidationService) {
        this.citationValidationService = citationValidationService;
    }

    @Override
    public List<AnalysisValidationIssue> validate(AnalysisOutputDTO output,
                                                  StructuredAnalysisResultDTO structuredResult,
                                                  List<EvidenceHit> evidenceHits,
                                                  List<String> knowledgeSnippets,
                                                  String questionCategory) {
        CitationValidationResult result = citationValidationService.enrichAndValidate(output, evidenceHits);
        if (result.isValid()) {
            return List.of();
        }
        return List.of(new AnalysisValidationIssue(
                "CITATION_MISMATCH",
                "存在无法追溯到本次 evidence 的古籍引用"
        ));
    }
}
