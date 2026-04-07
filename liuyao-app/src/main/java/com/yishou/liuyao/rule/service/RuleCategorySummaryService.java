package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RuleCategorySummaryService {

    public List<Map<String, Object>> buildCategorySummaries(List<RuleHit> hits, List<String> effectiveRuleCodes) {
        Set<String> effectiveCodes = effectiveRuleCodes == null ? Set.of() : new LinkedHashSet<>(effectiveRuleCodes);
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (RuleHit hit : hits) {
            String category = hit.getCategory() == null || hit.getCategory().isBlank() ? "GENERAL" : hit.getCategory();
            Map<String, Object> summary = grouped.computeIfAbsent(category, key -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("category", key);
                item.put("hitCount", 0);
                item.put("score", 0);
                item.put("effectiveHitCount", 0);
                item.put("effectiveScore", 0);
                item.put("stageOrder", resolveStageOrder(key));
                return item;
            });
            summary.put("hitCount", ((Integer) summary.get("hitCount")) + 1);
            summary.put("score", ((Integer) summary.get("score")) + (hit.getScoreDelta() == null ? 0 : hit.getScoreDelta()));
            if (effectiveCodes.contains(hit.getRuleCode())) {
                summary.put("effectiveHitCount", ((Integer) summary.get("effectiveHitCount")) + 1);
                summary.put("effectiveScore", ((Integer) summary.get("effectiveScore")) + (hit.getScoreDelta() == null ? 0 : hit.getScoreDelta()));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    public int resolveStageOrder(String category) {
        return switch (category) {
            case "YONGSHEN_STATE" -> 1;
            case "SHI_STATE" -> 2;
            case "SHI_YING", "SHI_YING_STATE" -> 3;
            case "MOVING_CHANGE" -> 4;
            case "EMPTY_STATE" -> 5;
            case "WANG_SHUAI" -> 6;
            case "COMPOSITE" -> 7;
            case "SCENARIO_WEIGHT" -> 8;
            default -> 50;
        };
    }
}
