package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalysisSectionComposer {

    private final AnalysisKnowledgeEvidenceService knowledgeEvidenceService;
    private final AnalysisCategoryTextResolver categoryTextResolver;
    private final AnalysisOutcomeTextResolver outcomeTextResolver;
    private final AnalysisPhaseTwoSignalFormatter phaseTwoSignalFormatter;

    public AnalysisSectionComposer(AnalysisKnowledgeEvidenceService knowledgeEvidenceService,
                                   AnalysisCategoryTextResolver categoryTextResolver,
                                   AnalysisOutcomeTextResolver outcomeTextResolver,
                                   AnalysisPhaseTwoSignalFormatter phaseTwoSignalFormatter) {
        this.knowledgeEvidenceService = knowledgeEvidenceService;
        this.categoryTextResolver = categoryTextResolver;
        this.outcomeTextResolver = outcomeTextResolver;
        this.phaseTwoSignalFormatter = phaseTwoSignalFormatter;
    }

    public String compose(AnalysisContextDTO context) {
        if (context == null) {
            return "分析模块骨架已就绪，后续可接入受约束的 LLM 编排。";
        }
        int ruleCount = context.getRuleCount() == null ? 0 : context.getRuleCount();
        return String.join(" ", List.of(
                        buildHexagramOverview(context),
                        buildUseGodSection(context, ruleCount),
                        buildCategoryObservation(context),
                        buildMovingObservation(context),
                        buildPhaseTwoObservation(context),
                        buildRiskObservation(context),
                        buildConclusion(context),
                        buildActionSuggestion(context),
                        buildKnowledgeHint(context.getKnowledgeSnippets()))
                .stream()
                .filter(item -> item != null && !item.isBlank())
                .toList());
    }

    private String buildHexagramOverview(AnalysisContextDTO context) {
        String questionLead = resolveQuestionLead(context.getQuestionCategory());
        String mainHexagram = context.getMainHexagram() == null || context.getMainHexagram().isBlank()
                ? "未知本卦"
                : context.getMainHexagram();
        String changedHexagram = context.getChangedHexagram() == null || context.getChangedHexagram().isBlank()
                ? "未见明显变卦"
                : context.getChangedHexagram();
        ChartSnapshotDTO chartSnapshot = context.getChartSnapshot();
        String derivedHexagrams = phaseTwoSignalFormatter.renderDerivedHexagrams(chartSnapshot);
        String suffix = derivedHexagrams.isBlank() ? "" : " " + derivedHexagrams + "。";
        return String.format("卦象概览：%s，本卦%s，变卦%s。", questionLead, mainHexagram, changedHexagram) + suffix;
    }

    private String buildUseGodSection(AnalysisContextDTO context, int ruleCount) {
        String useGod = context.getUseGod() == null || context.getUseGod().isBlank() ? "未定用神" : context.getUseGod();
        return String.format("用神判断：本次以%s为用神。%s %s",
                useGod,
                categoryTextResolver.renderUseGodFocus(context.getQuestionCategory(), useGod),
                buildRuleSummary(context, ruleCount));
    }

    private String buildRuleSummary(AnalysisContextDTO context, int ruleCount) {
        if (context.getStructuredResult() == null) {
            return String.format("当前共命中%d条规则。", ruleCount);
        }
        Integer effectiveScore = context.getStructuredResult().getEffectiveScore();
        String effectiveResultLevel = context.getStructuredResult().getEffectiveResultLevel();
        String summary = context.getStructuredResult().getSummary();
        String effectiveText = effectiveScore == null
                ? ""
                : String.format("冲突裁剪后有效评分%d，%s。", effectiveScore, categoryTextResolver.renderLevelText(effectiveResultLevel));
        String summaryText = summary == null || summary.isBlank() ? "" : summary;
        return String.format("当前共命中%d条规则。%s%s", ruleCount, effectiveText, summaryText);
    }

    private String buildCategoryObservation(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null) {
            return "";
        }
        String questionCategory = context.getQuestionCategory() == null ? "" : context.getQuestionCategory();
        List<String> tags = context.getStructuredResult().getTags();
        List<String> effectiveRuleCodes = context.getStructuredResult().getEffectiveRuleCodes();
        List<String> suppressedRuleCodes = context.getStructuredResult().getSuppressedRuleCodes();
        String categoryText = resolveCategorySummaryText(context);
        String tagText = tags == null || tags.isEmpty() ? "暂无明显标签" : String.join("、", tags);
        String effectiveRuleText = effectiveRuleCodes == null || effectiveRuleCodes.isEmpty()
                ? "当前没有明确保留下来的主导规则"
                : "当前主导规则为" + String.join("、", effectiveRuleCodes);
        String suppressedText = suppressedRuleCodes == null || suppressedRuleCodes.isEmpty()
                ? ""
                : "，被压制规则为" + String.join("、", suppressedRuleCodes);
        return String.format("关系判断：%s。当前标签为%s；%s%s。", categoryTextResolver.renderCategoryObservationLead(questionCategory), tagText, effectiveRuleText, suppressedText)
                + (categoryText.isBlank() ? "" : " " + categoryText);
    }

    private String buildMovingObservation(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null || context.getStructuredResult().getConflictSummaries() == null) {
            return "";
        }
        long movingConflicts = context.getStructuredResult().getConflictSummaries().stream()
                .filter(item -> "MOVING_CHANGE".equals(item.getCategory()))
                .count();
        String movingRuleText = context.getRuleCodes() != null && context.getRuleCodes().contains("R010")
                ? categoryTextResolver.renderMovingSignal(context.getQuestionCategory(), true)
                : categoryTextResolver.renderMovingSignal(context.getQuestionCategory(), false);
        if (movingConflicts > 0) {
            return "动爻影响：" + movingRuleText + "，但动变层也存在相互牵制，节奏上会有反复。";
        }
        return "动爻影响：" + movingRuleText + "。";
    }

    private String buildRiskObservation(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null) {
            return "";
        }
        String level = context.getStructuredResult().getEffectiveResultLevel();
        String riskText = outcomeTextResolver.renderRiskText(context.getQuestionCategory(), level);
        return "风险提示：" + knowledgeEvidenceService.appendKnowledgeEvidence(riskText, context, "risk") + "。";
    }

    private String buildConclusion(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null) {
            return "";
        }
        Integer effectiveScore = context.getStructuredResult().getEffectiveScore();
        String levelText = categoryTextResolver.renderLevelText(context.getStructuredResult().getEffectiveResultLevel());
        String dominantSignal = categoryTextResolver.renderDominantSignalText(context);
        String direction = outcomeTextResolver.renderConclusionDirection(context.getQuestionCategory());
        return String.format("结论建议：当前有效评分%s，%s；%s%s。",
                effectiveScore == null ? "未定" : String.valueOf(effectiveScore),
                levelText,
                dominantSignal,
                direction);
    }

    private String buildActionSuggestion(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null) {
            return "";
        }
        String level = context.getStructuredResult().getEffectiveResultLevel();
        String suggestion = outcomeTextResolver.renderActionSuggestion(context.getQuestionCategory(), level);
        return "下一步建议：" + knowledgeEvidenceService.appendKnowledgeEvidence(suggestion, context, "action");
    }

    private String buildKnowledgeHint(List<String> knowledgeSnippets) {
        String knowledgeHint = firstKnowledgeHint(knowledgeSnippets);
        if (knowledgeHint == null) {
            return "可参考资料：当前暂未检索到直接匹配资料，先以盘面结构和规则信号为主。";
        }
        return "可参考资料：" + knowledgeHint;
    }

    private String buildPhaseTwoObservation(AnalysisContextDTO context) {
        String phaseTwoSignals = phaseTwoSignalFormatter.renderPhaseTwoSignals(context);
        if (phaseTwoSignals.isBlank()) {
            return "";
        }
        return "象义补充：" + phaseTwoSignals + "。";
    }

    private String firstKnowledgeHint(List<String> knowledgeSnippets) {
        if (knowledgeSnippets == null || knowledgeSnippets.isEmpty()) {
            return null;
        }
        String first = knowledgeSnippets.get(0);
        if (first == null || first.isBlank()) {
            return null;
        }
        String normalized = first.replace('\n', ' ').trim();
        if (normalized.length() <= 48) {
            return normalized;
        }
        return normalized.substring(0, 48) + "...";
    }

    private String resolveQuestionLead(String questionCategory) {
        if (questionCategory == null || questionCategory.isBlank()) {
            return "本次所问";
        }
        return "问" + questionCategory;
    }

    private String resolveCategorySummaryText(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null || context.getStructuredResult().getCategorySummaries() == null) {
            return "";
        }
        return context.getStructuredResult().getCategorySummaries().stream()
                .filter(item -> item.getEffectiveHitCount() != null && item.getEffectiveHitCount() > 0)
                .sorted((left, right) -> Integer.compare(
                        right.getEffectiveScore() == null ? 0 : right.getEffectiveScore(),
                        left.getEffectiveScore() == null ? 0 : left.getEffectiveScore()))
                .limit(2)
                .map(item -> String.format("%s阶段保留%d条有效信号，合计%d分",
                        item.getCategory(),
                        item.getEffectiveHitCount(),
                        item.getEffectiveScore() == null ? 0 : item.getEffectiveScore()))
                .reduce((left, right) -> left + "；" + right)
                .orElse("");
    }
}
