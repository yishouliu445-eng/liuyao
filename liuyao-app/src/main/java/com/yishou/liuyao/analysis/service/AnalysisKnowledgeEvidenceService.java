package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;

import java.util.List;

import static java.lang.Integer.MIN_VALUE;
import org.springframework.stereotype.Component;

@Component
public class AnalysisKnowledgeEvidenceService {

    public String appendKnowledgeEvidence(String baseText, AnalysisContextDTO context, String purpose) {
        if (baseText == null || baseText.isBlank()) {
            return "";
        }
        String evidence = summarizeKnowledgeEvidence(selectKnowledgeSnippet(context, purpose));
        if (evidence == null) {
            return baseText;
        }
        return baseText + " 可结合" + evidence + "继续判断";
    }

    String selectKnowledgeSnippet(AnalysisContextDTO context, String purpose) {
        if (context == null || context.getKnowledgeSnippets() == null || context.getKnowledgeSnippets().isEmpty()) {
            return null;
        }
        String bestSnippet = null;
        int bestScore = MIN_VALUE;
        for (String snippet : context.getKnowledgeSnippets()) {
            int score = scoreKnowledgeSnippet(snippet, context, purpose);
            if (score > bestScore) {
                bestScore = score;
                bestSnippet = snippet;
            }
        }
        return bestSnippet;
    }

    int scoreKnowledgeSnippet(String snippet, AnalysisContextDTO context, String purpose) {
        if (snippet == null || snippet.isBlank()) {
            return MIN_VALUE;
        }
        String normalized = snippet.replace(" ", "");
        String category = context.getQuestionCategory() == null ? "" : context.getQuestionCategory();
        List<String> ruleCodes = resolvePreferredRuleCodes(context);
        int score = "risk".equals(purpose)
                ? (containsAny(normalized, "风险", "空亡", "月破", "日破", "受克", "拖延", "阻", "反复") ? 6 : 0)
                : (containsAny(normalized, "宜", "可", "利", "回查", "继续", "推进", "准备", "观察") ? 6 : 0);
        score += switch (category) {
            case "收入", "财运" -> containsAny(normalized, "财", "回款", "收益", "用神") ? 4 : 0;
            case "求职", "工作", "升职", "调岗" -> containsAny(normalized, "官", "岗位", "录用", "机会", "求职", "文书", "回访", "回应") ? 4 : 0;
            case "感情", "婚姻", "复合", "合作", "人际" -> containsAny(normalized, "世应", "应爻", "关系", "对方") ? 4 : 0;
            case "健康" -> containsAny(normalized, "病", "官鬼", "恢复", "子孙") ? 4 : 0;
            case "出行", "搬家", "房产" -> containsAny(normalized, "父母", "文书", "手续", "行程", "住处") ? 4 : 0;
            case "官司" -> containsAny(normalized, "官司", "诉讼", "证据", "官鬼", "牵制") ? 4 : 0;
            case "寻物" -> containsAny(normalized, "失物", "寻物", "回查", "线索", "妻财") ? 4 : 0;
            default -> 0;
        };
        score += scoreByRuleCodes(normalized, ruleCodes);
        if ("action".equals(purpose) && containsAny(normalized, "空亡", "月破", "日破", "拖延", "落空", "反复")) {
            score -= 8;
        }
        if (ruleCodes.contains("R011") || ruleCodes.contains("USE_GOD_EMPTY")) {
            score += containsAny(normalized, "空亡", "旬空") ? 3 : 0;
        }
        if (ruleCodes.contains("USE_GOD_MONTH_BREAK")) {
            score += containsAny(normalized, "月破") ? 3 : 0;
        }
        if (ruleCodes.contains("USE_GOD_DAY_BREAK") || ruleCodes.contains("R009")) {
            score += containsAny(normalized, "日破", "受克") ? 3 : 0;
        }
        if (ruleCodes.contains("R028") || ruleCodes.contains("R029") || ruleCodes.contains("R032")) {
            score += containsAny(normalized, "相冲", "冲散", "冲克", "受冲") ? 3 : 0;
        }
        if (ruleCodes.contains("R030")) {
            score += containsAny(normalized, "入墓", "墓库", "受困") ? 4 : 0;
        }
        if (ruleCodes.contains("R031")) {
            score += containsAny(normalized, "冲开", "冲起", "冲出", "冲墓") ? 4 : 0;
        }
        if (ruleCodes.contains("R010") || ruleCodes.contains("MOVING_LINE_EXISTS") || ruleCodes.contains("MOVING_LINE_AFFECT_USE_GOD")) {
            score += containsAny(normalized, "动爻", "发动", "动化", "变化") ? 3 : 0;
        }
        return score;
    }

    List<String> resolvePreferredRuleCodes(AnalysisContextDTO context) {
        if (context == null) {
            return List.of();
        }
        if (context.getStructuredResult() != null
                && context.getStructuredResult().getEffectiveRuleCodes() != null
                && !context.getStructuredResult().getEffectiveRuleCodes().isEmpty()) {
            return context.getStructuredResult().getEffectiveRuleCodes();
        }
        return context.getRuleCodes() == null ? List.of() : context.getRuleCodes();
    }

    int scoreByRuleCodes(String normalizedSnippet, List<String> ruleCodes) {
        int score = 0;
        for (String ruleCode : ruleCodes) {
            score += switch (ruleCode) {
                case "USE_GOD_STRENGTH", "R003", "R004", "R007", "R008", "R018" ->
                        containsAny(normalizedSnippet, "旺相", "休囚", "生扶", "合", "强弱", "化退") ? 3 : 0;
                case "USE_GOD_EMPTY", "R011" ->
                        containsAny(normalizedSnippet, "空亡", "旬空") ? 3 : 0;
                case "USE_GOD_MONTH_BREAK" ->
                        containsAny(normalizedSnippet, "月破") ? 3 : 0;
                case "USE_GOD_DAY_BREAK", "R009" ->
                        containsAny(normalizedSnippet, "日破", "受克") ? 3 : 0;
                case "R028", "R029", "R032" ->
                        containsAny(normalizedSnippet, "相冲", "冲散", "冲克", "受冲") ? 3 : 0;
                case "R030" ->
                        containsAny(normalizedSnippet, "入墓", "墓库", "受困") ? 3 : 0;
                case "R031" ->
                        containsAny(normalizedSnippet, "冲开", "冲起", "冲出", "冲墓") ? 3 : 0;
                case "MOVING_LINE_EXISTS", "MOVING_LINE_AFFECT_USE_GOD", "R010", "R021" ->
                        containsAny(normalizedSnippet, "动爻", "发动", "动化", "变化") ? 3 : 0;
                case "SHI_YING_RELATION", "R013", "R014", "R015" ->
                        containsAny(normalizedSnippet, "世应", "应爻", "对方", "关系") ? 3 : 0;
                default -> 0;
            };
        }
        return score;
    }

    boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    String summarizeKnowledgeEvidence(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return null;
        }
        if (snippet.startsWith("[") && snippet.contains("]")) {
            int closingIndex = snippet.indexOf(']');
            String source = snippet.substring(1, closingIndex).trim();
            String content = snippet.substring(closingIndex + 1).trim();
            if (!source.isBlank() && !content.isBlank()) {
                return source + "中的“" + trimKnowledgeSentence(content) + "”";
            }
        }
        return "资料片段中的“" + trimKnowledgeSentence(snippet) + "”";
    }

    String trimKnowledgeSentence(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace('\n', ' ').trim();
        if (normalized.length() <= 24) {
            return normalized;
        }
        return normalized.substring(0, 24) + "...";
    }
}
