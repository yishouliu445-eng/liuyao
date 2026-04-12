package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 上下文窗口构建器。
 *
 * <p>职责：将所有 Prompt 组件（System Prompt / 排盘锁存 / 规则摘要 / 对话历史 / RAG结果）
 * 按 Token 预算拼装为最终的 {@code List<LlmClient.ChatMessage>}，
 * 确保总 Token 数不超过上限（约 9000 tokens）。</p>
 *
 * <p>Token 预算分配：
 * <ul>
 *   <li>System Prompt  ≈ 800 tokens（固定）</li>
 *   <li>排盘JSON锁存   ≈ 1500 tokens（固定，精简后）</li>
 *   <li>规则摘要锁存   ≈ 500 tokens（固定）</li>
 *   <li>RAG 知识片段   ≈ 1000 tokens（每轮重新检索）</li>
 *   <li>对话历史       ≈ 4000 tokens（滑动窗口，最近5轮完整）</li>
 *   <li>输出预留       ≈ 1200 tokens</li>
 * </ul>
 * </p>
 */
@Component
public class ContextWindowBuilder {

    /** 保留最近几轮完整对话（超出的轮次压缩为摘要） */
    private static final int MAX_FULL_ROUNDS = 5;

    /** 保守的总上下文预算，和文档中的约 9000 tokens 对齐 */
    private static final int MAX_CONTEXT_TOKENS = 9000;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ---- Public API ----

    /**
     * 构建首次分析的消息数组。
     * 结构：[system(analyst + chart + rules + knowledge), user(question)]
     */
    public List<LlmClient.ChatMessage> buildInitialContext(
            String systemPromptTemplate,
            ChartSnapshot chart,
            List<RuleHit> ruleHits,
            int effectiveScore,
            String resultLevel,
            List<String> knowledgeSnippets,
            String question) {

        String systemContent = buildSystemContent(systemPromptTemplate, chart,
                ruleHits, effectiveScore, resultLevel, knowledgeSnippets);

        return List.of(
                LlmClient.ChatMessage.system(systemContent),
                LlmClient.ChatMessage.user(question)
        );
    }

    /**
     * 构建追问分析的消息数组。
     * 结构：[system(analyst + chart + rules), ...历史对话(裁剪), user(knowledge + 追问)]
     */
    public List<LlmClient.ChatMessage> buildFollowUpContext(
            String systemPromptTemplate,
            ChartSnapshot chart,
            List<RuleHit> ruleHits,
            int effectiveScore,
            String resultLevel,
            List<com.yishou.liuyao.session.domain.ChatMessage> history,
            List<String> knowledgeSnippets,
            String followUpQuestion) {

        // System 消息（不含 knowledge，knowledge 跟 user 追问一起放）
        String systemContent = buildSystemContent(systemPromptTemplate, chart,
                ruleHits, effectiveScore, resultLevel, Collections.emptyList());

        List<LlmClient.ChatMessage> messages = new ArrayList<>();
        messages.add(LlmClient.ChatMessage.system(systemContent));

        // 历史对话（裁剪）
        List<LlmClient.ChatMessage> historyMsgs = buildHistoryMessages(history);
        messages.addAll(historyMsgs);

        // 本轮 user 消息（含 knowledge）
        StringBuilder userContent = new StringBuilder();
        if (!knowledgeSnippets.isEmpty()) {
            userContent.append("## 本轮相关古籍片段\n");
            for (int i = 0; i < knowledgeSnippets.size(); i++) {
                userContent.append(i + 1).append(". ").append(knowledgeSnippets.get(i)).append("\n");
            }
            userContent.append("\n");
        }
        userContent.append(followUpQuestion);

        messages.add(LlmClient.ChatMessage.user(userContent.toString()));
        trimToBudget(messages);
        return messages;
    }

    // ---- Private Helpers ----

    private String buildSystemContent(String systemTemplate,
                                       ChartSnapshot chart,
                                       List<RuleHit> ruleHits,
                                       int effectiveScore,
                                       String resultLevel,
                                       List<String> knowledgeSnippets) {
        StringBuilder sb = new StringBuilder(systemTemplate);
        sb.append("\n\n");
        sb.append(buildChartSection(chart));
        sb.append("\n\n");
        sb.append(buildRuleSection(ruleHits, effectiveScore, resultLevel));
        if (!knowledgeSnippets.isEmpty()) {
            sb.append("\n\n");
            sb.append(buildKnowledgeSection(knowledgeSnippets));
        }
        return sb.toString();
    }

    private String buildChartSection(ChartSnapshot chart) {
        StringBuilder sb = new StringBuilder();
        sb.append("<chart_data>\n");
        sb.append("问题：").append(nullSafe(chart.getQuestion())).append("\n");
        sb.append("类别：").append(nullSafe(chart.getQuestionCategory())).append("\n");
        if (chart.getDivinationTime() != null) {
            sb.append("起卦时间：").append(chart.getDivinationTime().format(TIME_FMT)).append("\n");
        }
        sb.append("本卦：").append(nullSafe(chart.getMainHexagram()))
          .append("（").append(nullSafe(chart.getPalace())).append("宫，")
          .append(nullSafe(chart.getPalaceWuXing())).append("）\n");
        sb.append("变卦：").append(nullSafe(chart.getChangedHexagram())).append("\n");
        sb.append("世爻：第").append(nullSafe(chart.getShi())).append("爻，")
          .append("应爻：第").append(nullSafe(chart.getYing())).append("爻\n");
        sb.append("日辰：").append(nullSafe(chart.getRiChen())).append("，")
          .append("月建：").append(nullSafe(chart.getYueJian())).append("\n");
        if (chart.getKongWang() != null && !chart.getKongWang().isEmpty()) {
            sb.append("旬空：").append(String.join("、", chart.getKongWang())).append("\n");
        }
        sb.append("用神：").append(nullSafe(chart.getUseGod())).append("\n");
        sb.append("\n爻位详情：\n");
        if (chart.getLines() != null) {
            for (LineInfo line : chart.getLines()) {
                sb.append("  第").append(line.getIndex()).append("爻 ")
                  .append(nullSafe(line.getYinYang())).append(" ")
                  .append(nullSafe(line.getBranch())).append(" ")
                  .append(nullSafe(line.getLiuQin())).append(" ")
                  .append(nullSafe(line.getLiuShen()));
                if (Boolean.TRUE.equals(line.getMoving())) {
                    sb.append(" [动]");
                    if (line.getChangeBranch() != null) {
                        sb.append("→").append(line.getChangeBranch());
                    }
                }
                sb.append("\n");
            }
        }
        sb.append("</chart_data>");
        return sb.toString();
    }

    private String buildRuleSection(List<RuleHit> ruleHits, int effectiveScore, String resultLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("<rule_hits>\n");
        sb.append("综合评分：").append(effectiveScore).append("\n");
        sb.append("结论等级：").append(nullSafe(resultLevel)).append("\n");
        sb.append("命中规则：\n");
        if (ruleHits != null) {
            for (RuleHit hit : ruleHits) {
                if (!Boolean.TRUE.equals(hit.getHit())) continue;
                sb.append("  [").append(nullSafe(hit.getImpactLevel())).append("] ")
                  .append(nullSafe(hit.getRuleCode())).append(" — ")
                  .append(nullSafe(hit.getHitReason()));
                if (hit.getScoreDelta() != null) {
                    sb.append("（").append(hit.getScoreDelta() > 0 ? "+" : "").append(hit.getScoreDelta()).append("）");
                }
                sb.append("\n");
            }
        }
        sb.append("</rule_hits>");
        return sb.toString();
    }

    private String buildKnowledgeSection(List<String> snippets) {
        StringBuilder sb = new StringBuilder();
        sb.append("<knowledge_snippets>\n");
        for (int i = 0; i < snippets.size(); i++) {
            sb.append(i + 1).append(". ").append(snippets.get(i)).append("\n");
        }
        sb.append("</knowledge_snippets>");
        return sb.toString();
    }

    /**
     * 将数据库历史消息转为 LLM ChatMessage 列表，并做 Token 裁剪。
     *
     * <p>策略：保留最近 MAX_FULL_ROUNDS 轮完整对话；
     * 更早的消息以 [摘要] 形式保留关键信息。</p>
     */
    private List<LlmClient.ChatMessage> buildHistoryMessages(
            List<com.yishou.liuyao.session.domain.ChatMessage> history) {

        List<LlmClient.ChatMessage> result = new ArrayList<>();

        // 只取 USER + ASSISTANT 交替的消息
        List<com.yishou.liuyao.session.domain.ChatMessage> dialogMsgs = history.stream()
                .filter(m -> "USER".equals(m.getRole()) || "ASSISTANT".equals(m.getRole()))
                .toList();

        // 成对分组（每2条为一轮）
        int totalRounds = dialogMsgs.size() / 2;
        int skipRounds = Math.max(0, totalRounds - MAX_FULL_ROUNDS);

        for (int round = 0; round < totalRounds; round++) {
            int userIdx = round * 2;
            int assistantIdx = round * 2 + 1;
            if (assistantIdx >= dialogMsgs.size()) break;

            var userMsg = dialogMsgs.get(userIdx);
            var assistantMsg = dialogMsgs.get(assistantIdx);

            if (round < skipRounds) {
                // 早期消息压缩为摘要
                String summary = "[历史摘要] 用户问：" + truncate(userMsg.getContent(), 32) +
                                 " | AI答：" + truncate(extractConclusion(assistantMsg), 48);
                result.add(LlmClient.ChatMessage.user(summary));
                result.add(LlmClient.ChatMessage.assistant("已收到"));
            } else {
                // 近期消息完整保留
                result.add(LlmClient.ChatMessage.user(userMsg.getContent()));
                result.add(LlmClient.ChatMessage.assistant(assistantMsg.getContent()));
            }
        }

        return result;
    }

    /** 从 ASSISTANT 消息中提取结论字段（有 structured_json 用结论，否则截断 content） */
    private String extractConclusion(com.yishou.liuyao.session.domain.ChatMessage msg) {
        return truncate(msg.getContent(), 80);
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    /**
     * 保守估算当前消息数组的上下文开销。
     *
     * <p>这里不追求 tokenizer 级精度，而是用字符长度做上界近似，便于在测试和运行时
     * 统一判断“是否超过预算”。</p>
     */
    int estimateTokens(List<LlmClient.ChatMessage> messages) {
        int total = 0;
        for (LlmClient.ChatMessage message : messages) {
            total += estimateTokens(message.content());
            total += 8; // 角色与消息边界的保守开销
        }
        return total;
    }

    int estimateTokens(String content) {
        return content == null ? 0 : Math.max(1, content.length() / 4);
    }

    private void trimToBudget(List<LlmClient.ChatMessage> messages) {
        if (messages.size() <= 2) {
            return;
        }

        int historyStart = 1;
        int historyEndExclusive = messages.size() - 1;
        int historyRounds = Math.max(0, (historyEndExclusive - historyStart) / 2);

        while (estimateTokens(messages) > MAX_CONTEXT_TOKENS && historyRounds > MAX_FULL_ROUNDS) {
            // 优先移除最早的“摘要轮次”，确保最近 5 轮完整保留。
            messages.remove(historyStart);
            messages.remove(historyStart);
            historyRounds--;
        }

        int cursor = historyStart;
        while (estimateTokens(messages) > MAX_CONTEXT_TOKENS && cursor < historyEndExclusive) {
            LlmClient.ChatMessage message = messages.get(cursor);
            String content = message.content();
            if (content == null || content.length() <= 512) {
                cursor++;
                continue;
            }

            int overflowTokens = estimateTokens(messages) - MAX_CONTEXT_TOKENS;
            int trimChars = Math.max(256, overflowTokens * 4);
            int nextLength = Math.max(512, content.length() - trimChars);
            messages.set(cursor, new LlmClient.ChatMessage(message.role(), truncate(content, nextLength)));
            cursor++;
        }
    }

    private String nullSafe(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}
