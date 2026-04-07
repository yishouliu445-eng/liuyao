package com.yishou.liuyao.rule.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RuleReasoningService {

    private final RuleCategorySummaryService ruleCategorySummaryService;
    private final RuleConflictResolver ruleConflictResolver;

    public RuleReasoningService() {
        this(new RuleCategorySummaryService(), new RuleConflictResolver());
    }

    public RuleReasoningService(RuleCategorySummaryService ruleCategorySummaryService,
                                RuleConflictResolver ruleConflictResolver) {
        this.ruleCategorySummaryService = ruleCategorySummaryService;
        this.ruleConflictResolver = ruleConflictResolver;
    }

    public RuleEvaluationResult summarize(ChartSnapshot chartSnapshot, List<RuleHit> hits) {
        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setHits(hits);
        int score = hits.stream()
                .map(RuleHit::getScoreDelta)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        List<Map<String, Object>> conflictSummaries = ruleConflictResolver.resolveConflicts(hits);
        Set<String> suppressedRuleCodes = collectRuleCodes(conflictSummaries, "suppressedRules");
        Set<String> effectiveRuleCodes = collectEffectiveRuleCodes(hits, conflictSummaries);
        int effectiveScore = hits.stream()
                .filter(hit -> effectiveRuleCodes.contains(hit.getRuleCode()))
                .map(RuleHit::getScoreDelta)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        result.setScore(score);
        result.setResultLevel(resolveResultLevel(score));
        result.setEffectiveScore(effectiveScore);
        result.setEffectiveResultLevel(resolveResultLevel(effectiveScore));
        result.setTags(collectTags(hits));
        result.setEffectiveRuleCodes(new ArrayList<>(effectiveRuleCodes));
        result.setSuppressedRuleCodes(new ArrayList<>(suppressedRuleCodes));
        result.setCategorySummaries(ruleCategorySummaryService.buildCategorySummaries(hits, result.getEffectiveRuleCodes()));
        result.setConflictSummaries(conflictSummaries);
        result.setSummary(buildSummaryText(chartSnapshot, score, effectiveScore,
                result.getResultLevel(), result.getEffectiveResultLevel(),
                result.getTags(), hits.size(), result.getConflictSummaries().size()));
        return result;
    }

    private String resolveResultLevel(int score) {
        if (score > 2) {
            return "GOOD";
        }
        if (score < -2) {
            return "BAD";
        }
        return "NEUTRAL";
    }

    private List<String> collectTags(List<RuleHit> hits) {
        Set<String> tags = new LinkedHashSet<>();
        for (RuleHit hit : hits) {
            if (hit.getTags() != null) {
                tags.addAll(hit.getTags());
            }
        }
        return new ArrayList<>(tags);
    }

    private String buildSummaryText(ChartSnapshot chartSnapshot,
                                    int score,
                                    int effectiveScore,
                                    String resultLevel,
                                    String effectiveResultLevel,
                                    List<String> tags,
                                    int hitCount,
                                    int conflictCount) {
        String useGod = chartSnapshot == null || chartSnapshot.getUseGod() == null || chartSnapshot.getUseGod().isBlank()
                ? "未定用神"
                : chartSnapshot.getUseGod();
        String mainHexagram = chartSnapshot == null || chartSnapshot.getMainHexagram() == null || chartSnapshot.getMainHexagram().isBlank()
                ? "未知本卦"
                : chartSnapshot.getMainHexagram();
        String levelText = resolveLevelText(resultLevel);
        String tagText = tags.isEmpty() ? "暂无明显标签" : String.join("、", tags);
        String conflictText = conflictCount <= 0 ? "当前未见明显规则冲突" : "当前存在" + conflictCount + "组规则冲突";
        String effectiveText = conflictCount <= 0
                ? "当前以原始评分为准"
                : String.format("冲突裁剪后有效评分%d，%s", effectiveScore, resolveLevelText(effectiveResultLevel));
        return String.format("本卦%s，围绕用神%s共命中%d条规则，当前评分%d，%s，当前标签：%s。%s。%s。",
                mainHexagram,
                useGod,
                hitCount,
                score,
                levelText,
                tagText,
                conflictText,
                effectiveText);
    }

    private String resolveLevelText(String resultLevel) {
        return switch (resultLevel) {
            case "GOOD" -> "整体偏吉";
            case "BAD" -> "整体偏弱";
            default -> "整体中性，存在反复";
        };
    }

    private Set<String> collectRuleCodes(List<Map<String, Object>> conflictSummaries, String key) {
        Set<String> codes = new LinkedHashSet<>();
        for (Map<String, Object> conflict : conflictSummaries) {
            Object value = conflict.get(key);
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    codes.add(String.valueOf(item));
                }
            }
        }
        return codes;
    }

    private Set<String> collectEffectiveRuleCodes(List<RuleHit> hits, List<Map<String, Object>> conflictSummaries) {
        Set<String> codes = new LinkedHashSet<>();
        Set<String> conflictRuleCodes = new LinkedHashSet<>();
        for (Map<String, Object> conflict : conflictSummaries) {
            conflictRuleCodes.addAll(asStringSet(conflict.get("positiveRules")));
            conflictRuleCodes.addAll(asStringSet(conflict.get("negativeRules")));
            codes.addAll(asStringSet(conflict.get("effectiveRules")));
        }
        for (RuleHit hit : hits) {
            if (!conflictRuleCodes.contains(hit.getRuleCode())) {
                codes.add(hit.getRuleCode());
            }
        }
        return codes;
    }

    private Set<String> asStringSet(Object value) {
        Set<String> items = new LinkedHashSet<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                items.add(String.valueOf(item));
            }
        }
        return items;
    }
}
