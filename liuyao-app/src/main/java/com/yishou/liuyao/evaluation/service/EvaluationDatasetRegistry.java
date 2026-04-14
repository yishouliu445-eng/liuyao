package com.yishou.liuyao.evaluation.service;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class EvaluationDatasetRegistry {

    private static final Set<String> DATASET_TYPES = Set.of(
            "PROMPT_REGRESSION",
            "CASE_REPLAY",
            "RAG_RECALL"
    );

    public boolean supports(String datasetType) {
        return datasetType != null && DATASET_TYPES.contains(datasetType);
    }
}
