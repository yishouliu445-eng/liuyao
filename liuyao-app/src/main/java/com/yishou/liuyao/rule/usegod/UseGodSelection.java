package com.yishou.liuyao.rule.usegod;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UseGodSelection {

    private final QuestionIntent intent;
    private final UseGodType useGod;
    private final Integer priority;
    private final String scenario;
    private final String note;
    private final String configVersion;
    private final Integer selectedLineIndex;
    private final List<Integer> candidateLineIndexes;
    private final String selectionStrategy;
    private final String selectionReason;
    private final Boolean fallbackApplied;
    private final String fallbackStrategy;
    private final List<Map<String, Object>> scoreDetails;
    private final Map<String, Object> evidence;

    public UseGodSelection(QuestionIntent intent,
                           UseGodType useGod,
                           Integer priority,
                           String scenario,
                           String note,
                           String configVersion,
                           Integer selectedLineIndex,
                           List<Integer> candidateLineIndexes,
                           String selectionStrategy,
                           String selectionReason,
                           Boolean fallbackApplied,
                           String fallbackStrategy,
                           List<Map<String, Object>> scoreDetails,
                           Map<String, Object> evidence) {
        this.intent = intent;
        this.useGod = useGod;
        this.priority = priority;
        this.scenario = scenario;
        this.note = note;
        this.configVersion = configVersion;
        this.selectedLineIndex = selectedLineIndex;
        this.candidateLineIndexes = candidateLineIndexes == null ? List.of() : List.copyOf(candidateLineIndexes);
        this.selectionStrategy = selectionStrategy;
        this.selectionReason = selectionReason;
        this.fallbackApplied = fallbackApplied;
        this.fallbackStrategy = fallbackStrategy;
        this.scoreDetails = scoreDetails == null ? List.of() : scoreDetails.stream().map(LinkedHashMap::new).map(Map::copyOf).toList();
        this.evidence = evidence == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(evidence));
    }

    public QuestionIntent getIntent() {
        return intent;
    }

    public UseGodType getUseGod() {
        return useGod;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getScenario() {
        return scenario;
    }

    public String getNote() {
        return note;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public Integer getSelectedLineIndex() {
        return selectedLineIndex;
    }

    public List<Integer> getCandidateLineIndexes() {
        return candidateLineIndexes;
    }

    public String getSelectionStrategy() {
        return selectionStrategy;
    }

    public String getSelectionReason() {
        return selectionReason;
    }

    public Boolean getFallbackApplied() {
        return fallbackApplied;
    }

    public String getFallbackStrategy() {
        return fallbackStrategy;
    }

    public List<Map<String, Object>> getScoreDetails() {
        return scoreDetails;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }
}
