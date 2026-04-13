package com.yishou.liuyao.analysis.service;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.LineInfo;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.session.domain.ChatMessage;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextWindowBuilderTest {

    private final ContextWindowBuilder builder = new ContextWindowBuilder();

    @Test
    void shouldKeepLatestFiveRoundsAndSummarizeOlderHistory() {
        List<LlmClient.ChatMessage> messages = builder.buildFollowUpContext(
                "SYSTEM",
                sampleChart(),
                sampleRuleHits(),
                8,
                "POSITIVE",
                buildHistory(6, 30),
                List.of("片段1"),
                "最后一个追问"
        );

        List<LlmClient.ChatMessage> historyMessages = messages.subList(1, messages.size() - 1);
        assertEquals(12, historyMessages.size());
        assertTrue(historyMessages.get(0).content().startsWith("[历史摘要]"));
        assertEquals("已收到", historyMessages.get(1).content());
        assertEquals("第2轮用户提问-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", historyMessages.get(2).content());
        assertEquals("最后一个追问", messages.get(messages.size() - 1).content().substring(messages.get(messages.size() - 1).content().length() - 6));
    }

    @Test
    void shouldRespectApproximateTokenBudgetForLongHistory() {
        List<LlmClient.ChatMessage> messages = builder.buildFollowUpContext(
                "SYSTEM",
                sampleChart(),
                sampleRuleHits(),
                8,
                "POSITIVE",
                buildHistory(10, 4000),
                List.of("知识片段".repeat(300)),
                "请继续分析"
        );

        int approxTokens = messages.stream()
                .map(LlmClient.ChatMessage::content)
                .mapToInt(this::estimateTokens)
                .sum();

        assertTrue(approxTokens <= 9000, "上下文预算应控制在 9000 token 以内，实际约为 " + approxTokens);
    }

    private List<ChatMessage> buildHistory(int rounds, int repeatLength) {
        List<ChatMessage> history = new ArrayList<>();
        UUID sessionId = UUID.randomUUID();
        for (int round = 1; round <= rounds; round++) {
            ChatMessage user = ChatMessage.userMessage(sessionId, "第" + round + "轮用户提问-" + "x".repeat(repeatLength));
            ChatMessage assistant = ChatMessage.assistantMessage(sessionId, "第" + round + "轮AI回答-" + "y".repeat(repeatLength), null, "mock", 10, 10);
            history.add(user);
            history.add(assistant);
        }
        return history;
    }

    private ChartSnapshot sampleChart() {
        ChartSnapshot chart = new ChartSnapshot();
        chart.setQuestion("合作能否推进");
        chart.setQuestionCategory("合作");
        chart.setDivinationTime(LocalDateTime.of(2026, 4, 12, 10, 0));
        chart.setMainHexagram("山火贲");
        chart.setChangedHexagram("风山渐");
        chart.setPalace("艮");
        chart.setPalaceWuXing("土");
        chart.setShi(1);
        chart.setYing(4);
        chart.setRiChen("甲子");
        chart.setYueJian("辰月");
        chart.setUseGod("应爻");
        chart.setKongWang(List.of("子", "丑"));
        chart.setLines(List.of(line(1, true), line(2, false), line(3, false), line(4, false), line(5, true), line(6, false)));
        return chart;
    }

    private LineInfo line(int index, boolean moving) {
        LineInfo line = new LineInfo();
        line.setIndex(index);
        line.setYinYang(index % 2 == 0 ? "阴" : "阳");
        line.setBranch("子");
        line.setLiuQin("兄弟");
        line.setLiuShen("青龙");
        line.setMoving(moving);
        line.setChangeBranch("丑");
        return line;
    }

    private List<RuleHit> sampleRuleHits() {
        RuleHit hit = new RuleHit();
        hit.setHit(true);
        hit.setImpactLevel("MEDIUM");
        hit.setRuleCode("R001");
        hit.setHitReason("测试命中");
        hit.setScoreDelta(2);
        return List.of(hit);
    }

    private int estimateTokens(String content) {
        return Math.max(1, content.length() / 4);
    }
}
