package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalysisService {

    public String analyze(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        AnalysisContextDTO context = buildContext(question, chartSnapshot, ruleHits);
        return analyze(context);
    }

    public String analyze(AnalysisContextDTO context) {
        if (context == null) {
            return "分析模块骨架已就绪，后续可接入受约束的 LLM 编排。";
        }
        int ruleCount = context.getRuleCount() == null ? 0 : context.getRuleCount();
        return String.join(" ", List.of(
                        buildHexagramOverview(context),
                        buildUseGodSection(context, ruleCount),
                        buildCategoryObservation(context),
                        buildMovingObservation(context),
                        buildConclusion(context),
                        buildKnowledgeHint(context.getKnowledgeSnippets()))
                .stream()
                .filter(item -> item != null && !item.isBlank())
                .toList());
    }

    private AnalysisContextDTO buildContext(String question, ChartSnapshot chartSnapshot, List<RuleHit> ruleHits) {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setContextVersion("v1");
        context.setQuestion(question);
        if (chartSnapshot != null) {
            context.setQuestionCategory(chartSnapshot.getQuestionCategory());
            context.setUseGod(chartSnapshot.getUseGod());
            context.setMainHexagram(chartSnapshot.getMainHexagram());
            context.setChangedHexagram(chartSnapshot.getChangedHexagram());
        }
        context.setRuleCount(ruleHits == null ? 0 : ruleHits.size());
        context.setRuleCodes(ruleHits == null ? List.of() : ruleHits.stream().map(RuleHit::getRuleCode).toList());
        return context;
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

    private String buildHexagramOverview(AnalysisContextDTO context) {
        String questionLead = resolveQuestionLead(context.getQuestionCategory());
        String mainHexagram = context.getMainHexagram() == null || context.getMainHexagram().isBlank()
                ? "未知本卦"
                : context.getMainHexagram();
        String changedHexagram = context.getChangedHexagram() == null || context.getChangedHexagram().isBlank()
                ? "未见明显变卦"
                : context.getChangedHexagram();
        return String.format("卦象概览：%s，本卦%s，变卦%s。", questionLead, mainHexagram, changedHexagram);
    }

    private String buildUseGodSection(AnalysisContextDTO context, int ruleCount) {
        String useGod = context.getUseGod() == null || context.getUseGod().isBlank() ? "未定用神" : context.getUseGod();
        return String.format("用神判断：本次以%s为用神。%s", useGod, buildRuleSummary(context, ruleCount));
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
                : String.format("冲突裁剪后有效评分%d，%s。", effectiveScore, renderLevelText(effectiveResultLevel));
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
        return String.format("关系判断：%s当前标签为%s；%s%s。", renderQuestionCategoryHint(questionCategory), tagText, effectiveRuleText, suppressedText)
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
                ? "世爻或关键爻有发动信号，事情不会完全静止"
                : "当前动爻信号不算特别强烈";
        if (movingConflicts > 0) {
            return movingRuleText + "，但动变层也存在相互牵制，节奏上会有反复。";
        }
        return "动爻影响：" + movingRuleText + "。";
    }

    private String buildConclusion(AnalysisContextDTO context) {
        if (context.getStructuredResult() == null) {
            return "";
        }
        Integer effectiveScore = context.getStructuredResult().getEffectiveScore();
        String levelText = renderLevelText(context.getStructuredResult().getEffectiveResultLevel());
        String direction = switch (context.getQuestionCategory() == null ? "" : context.getQuestionCategory()) {
            case "收入" -> "财务层面先看落实和兑现节奏";
            case "感情" -> "关系层面先看互动与回应";
            case "工作" -> "工作层面先看岗位推进和外部反馈";
            case "健康" -> "健康层面先看恢复节奏与反复点";
            case "出行" -> "出行层面先看行程是否受阻";
            case "合作" -> "合作层面先看对方配合度与条件变动";
            default -> "后续以关键节点是否兑现为主";
        };
        return String.format("结论建议：当前有效评分%s，%s；%s。", effectiveScore == null ? "未定" : String.valueOf(effectiveScore), levelText, direction);
    }

    private String buildKnowledgeHint(List<String> knowledgeSnippets) {
        String knowledgeHint = firstKnowledgeHint(knowledgeSnippets);
        if (knowledgeHint == null) {
            return "";
        }
        return "可参考资料：" + knowledgeHint;
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

    private String renderQuestionCategoryHint(String questionCategory) {
        return switch (questionCategory) {
            case "收入" -> "收入层面";
            case "感情" -> "感情层面";
            case "工作" -> "工作层面";
            case "健康" -> "健康层面";
            case "出行" -> "出行层面";
            case "合作" -> "合作层面";
            default -> "综合层面";
        };
    }

    private String renderLevelText(String resultLevel) {
        if (resultLevel == null || resultLevel.isBlank()) {
            return "整体仍需继续观察";
        }
        return switch (resultLevel) {
            case "GOOD" -> "整体偏吉";
            case "BAD" -> "整体偏弱";
            default -> "整体中性，存在反复";
        };
    }
}
