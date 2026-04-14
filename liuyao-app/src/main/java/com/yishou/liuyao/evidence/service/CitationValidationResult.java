package com.yishou.liuyao.evidence.service;

import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;

import java.util.ArrayList;
import java.util.List;

public class CitationValidationResult {

    private boolean valid;
    private List<AnalysisOutputDTO.ClassicReference> unmatchedReferences = new ArrayList<>();

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<AnalysisOutputDTO.ClassicReference> getUnmatchedReferences() {
        return unmatchedReferences;
    }

    public void setUnmatchedReferences(List<AnalysisOutputDTO.ClassicReference> unmatchedReferences) {
        this.unmatchedReferences = unmatchedReferences;
    }
}
