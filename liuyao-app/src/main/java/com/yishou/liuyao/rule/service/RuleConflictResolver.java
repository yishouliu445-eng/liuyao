package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RuleConflictResolver {

    public List<Map<String, Object>> resolveConflicts(List<RuleHit> hits) {
        Map<String, Integer> positiveCounts = new LinkedHashMap<>();
        Map<String, Integer> negativeCounts = new LinkedHashMap<>();
        Map<String, Integer> positiveScores = new LinkedHashMap<>();
        Map<String, Integer> negativeScores = new LinkedHashMap<>();
        Map<String, Integer> positiveMaxPriority = new LinkedHashMap<>();
        Map<String, Integer> negativeMaxPriority = new LinkedHashMap<>();
        Map<String, List<String>> positiveRules = new LinkedHashMap<>();
        Map<String, List<String>> negativeRules = new LinkedHashMap<>();
        for (RuleHit hit : hits) {
            if (hit.getScoreDelta() == null || hit.getScoreDelta() == 0) {
                continue;
            }
            String category = hit.getCategory() == null || hit.getCategory().isBlank() ? "GENERAL" : hit.getCategory();
            if (hit.getScoreDelta() > 0) {
                positiveCounts.merge(category, 1, Integer::sum);
                positiveScores.merge(category, hit.getScoreDelta(), Integer::sum);
                positiveMaxPriority.merge(category, defaultPriority(hit), Math::max);
                positiveRules.computeIfAbsent(category, key -> new ArrayList<>()).add(hit.getRuleCode());
            } else {
                negativeCounts.merge(category, 1, Integer::sum);
                negativeScores.merge(category, hit.getScoreDelta(), Integer::sum);
                negativeMaxPriority.merge(category, defaultPriority(hit), Math::max);
                negativeRules.computeIfAbsent(category, key -> new ArrayList<>()).add(hit.getRuleCode());
            }
        }
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (String category : positiveCounts.keySet()) {
            if (!negativeCounts.containsKey(category)) {
                continue;
            }
            Map<String, Object> conflict = new LinkedHashMap<>();
            conflict.put("category", category);
            conflict.put("positiveCount", positiveCounts.get(category));
            conflict.put("negativeCount", negativeCounts.get(category));
            int positiveScore = positiveScores.getOrDefault(category, 0);
            int negativeScore = negativeScores.getOrDefault(category, 0);
            int netScore = positiveScore + negativeScore;
            int positivePriority = positiveMaxPriority.getOrDefault(category, 0);
            int negativePriority = negativeMaxPriority.getOrDefault(category, 0);
            String decision = resolveDecision(netScore, positivePriority, negativePriority);
            conflict.put("positiveScore", positiveScore);
            conflict.put("negativeScore", negativeScore);
            conflict.put("netScore", netScore);
            conflict.put("positivePriority", positivePriority);
            conflict.put("negativePriority", negativePriority);
            conflict.put("decision", decision);
            conflict.put("positiveRules", positiveRules.getOrDefault(category, List.of()));
            conflict.put("negativeRules", negativeRules.getOrDefault(category, List.of()));
            conflict.put("effectiveRules", resolveEffectiveRules(decision,
                    positiveRules.getOrDefault(category, List.of()),
                    negativeRules.getOrDefault(category, List.of())));
            conflict.put("suppressedRules", resolveSuppressedRules(decision,
                    positiveRules.getOrDefault(category, List.of()),
                    negativeRules.getOrDefault(category, List.of())));
            conflicts.add(conflict);
        }
        return conflicts;
    }

    private String resolveDecision(int netScore, int positivePriority, int negativePriority) {
        if (netScore > 0) {
            return "POSITIVE_DOMINANT";
        }
        if (netScore < 0) {
            return "NEGATIVE_DOMINANT";
        }
        if (positivePriority > negativePriority) {
            return "POSITIVE_DOMINANT";
        }
        if (negativePriority > positivePriority) {
            return "NEGATIVE_DOMINANT";
        }
        return "MIXED";
    }

    private int defaultPriority(RuleHit hit) {
        return hit.getPriority() == null ? 0 : hit.getPriority();
    }

    private List<String> resolveEffectiveRules(String decision, List<String> positiveRules, List<String> negativeRules) {
        return switch (decision) {
            case "POSITIVE_DOMINANT" -> positiveRules;
            case "NEGATIVE_DOMINANT" -> negativeRules;
            default -> List.of();
        };
    }

    private List<String> resolveSuppressedRules(String decision, List<String> positiveRules, List<String> negativeRules) {
        return switch (decision) {
            case "POSITIVE_DOMINANT" -> negativeRules;
            case "NEGATIVE_DOMINANT" -> positiveRules;
            default -> List.of();
        };
    }
}
