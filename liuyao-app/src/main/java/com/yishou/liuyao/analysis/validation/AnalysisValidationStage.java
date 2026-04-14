package com.yishou.liuyao.analysis.validation;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.evidence.dto.EvidenceHit;

import java.util.List;

public interface AnalysisValidationStage {

    List<AnalysisValidationIssue> validate(AnalysisOutputDTO output,
                                           StructuredAnalysisResultDTO structuredResult,
                                           List<EvidenceHit> evidenceHits,
                                           List<String> knowledgeSnippets,
                                           String questionCategory);
}
