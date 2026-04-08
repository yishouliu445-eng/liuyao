package com.yishou.liuyao.divination.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.knowledge.domain.BookChunk;
import com.yishou.liuyao.knowledge.repository.BookChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.flyway.enabled=true")
class DivinationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookChunkRepository bookChunkRepository;

    @Test
    void shouldAnalyzeThroughHttpEndpoint() throws Exception {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次出行会不会顺利");
        request.setQuestionCategory("出行");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 9, 9, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        mockMvc.perform(post("/api/divinations/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chartSnapshot.questionCategory").value("出行"))
                .andExpect(jsonPath("$.data.chartSnapshot.useGod").value("父母"))
                .andExpect(jsonPath("$.data.chartSnapshot.mainHexagram").value("山火贲"))
                .andExpect(jsonPath("$.data.chartSnapshot.changedHexagram").value("风山渐"))
                .andExpect(jsonPath("$.data.chartSnapshot.palace").value("艮"))
                .andExpect(jsonPath("$.data.chartSnapshot.shi").value(1))
                .andExpect(jsonPath("$.data.chartSnapshot.ying").value(4))
                .andExpect(jsonPath("$.data.chartSnapshot.snapshotVersion").value("v1"))
                .andExpect(jsonPath("$.data.chartSnapshot.calendarVersion").value("v1"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[0].branch").value("卯"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[0].liuQin").value("官鬼"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[0].changeBranch").value("辰"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[0].changeWuXing").value("土"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[0].changeLiuQin").value("兄弟"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[4].branch").value("子"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[4].liuQin").value("妻财"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[4].changeBranch").value("巳"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[4].changeLiuQin").value("父母"))
                .andExpect(jsonPath("$.data.chartSnapshot.mainHexagramCode").isNotEmpty())
                .andExpect(jsonPath("$.data.chartSnapshot.mainUpperTrigram").isNotEmpty())
                .andExpect(jsonPath("$.data.chartSnapshot.changedLowerTrigram").isNotEmpty())
                .andExpect(jsonPath("$.data.ruleHits[0].ruleCode").value("USE_GOD_SELECTION"))
                .andExpect(jsonPath("$.data.ruleHits[?(@.ruleCode=='SHI_YING_RELATION')].evidence.mainUpperTrigram").isNotEmpty())
                .andExpect(jsonPath("$.data.ruleHits[?(@.ruleCode=='SHI_YING_RELATION')].evidence.targetCount").value(org.hamcrest.Matchers.hasItem(2)))
                .andExpect(jsonPath("$.data.ruleHits[?(@.ruleCode=='SHI_YING_RELATION')].evidence.targetSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.analysisContext.contextVersion").value("v1"))
                .andExpect(jsonPath("$.data.analysisContext.useGod").value("父母"))
                .andExpect(jsonPath("$.data.analysisContext.mainHexagram").value("山火贲"))
                .andExpect(jsonPath("$.data.analysisContext.chartSnapshot.mainHexagram").value("山火贲"))
                .andExpect(jsonPath("$.data.analysisContext.chartSnapshot.lines[0].changeBranch").value("辰"))
                .andExpect(jsonPath("$.data.analysisContext.ruleCodes").isArray())
                .andExpect(jsonPath("$.data.analysisContext.structuredResult").exists())
                .andExpect(jsonPath("$.data.structuredResult").exists())
                .andExpect(jsonPath("$.data.structuredResult.score").isNumber())
                .andExpect(jsonPath("$.data.structuredResult.resultLevel").isNotEmpty())
                .andExpect(jsonPath("$.data.structuredResult.effectiveScore").isNumber())
                .andExpect(jsonPath("$.data.structuredResult.effectiveResultLevel").isNotEmpty())
                .andExpect(jsonPath("$.data.structuredResult.tags").isArray())
                .andExpect(jsonPath("$.data.structuredResult.effectiveRuleCodes").isArray())
                .andExpect(jsonPath("$.data.structuredResult.suppressedRuleCodes").isArray())
                .andExpect(jsonPath("$.data.structuredResult.categorySummaries").isArray())
                .andExpect(jsonPath("$.data.structuredResult.categorySummaries[0].category").value("YONGSHEN_STATE"))
                .andExpect(jsonPath("$.data.structuredResult.categorySummaries[0].effectiveHitCount").isNumber())
                .andExpect(jsonPath("$.data.structuredResult.categorySummaries[0].effectiveScore").isNumber())
                .andExpect(jsonPath("$.data.structuredResult.conflictSummaries").isArray())
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("卦象概览")))
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("用神判断")))
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("问出行")))
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("以父母为用神")));
    }

    @Test
    void shouldExposeStructuredRuleEvidenceForBreakAndEmptyRules() throws Exception {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("我下个月工资会不会上涨");
        request.setQuestionCategory("收入");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        mockMvc.perform(post("/api/divinations/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ruleHits[*].ruleCode").isArray())
                .andExpect(jsonPath("$.data.ruleHits[?(@.ruleCode=='SHI_YING_RELATION')].evidence.targetCount").value(org.hamcrest.Matchers.hasItem(2)))
                .andExpect(jsonPath("$.data.ruleHits[?(@.ruleCode=='SHI_YING_RELATION')].evidence.mainUpperTrigram").isNotEmpty())
                .andExpect(jsonPath("$.data.ruleHits[?(@.ruleCode=='USE_GOD_STRENGTH')].evidence.targetSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.ruleHits[?(@.ruleCode=='USE_GOD_STRENGTH')].evidence.bestLevel").isNotEmpty())
                .andExpect(jsonPath("$.data.analysisContext.useGod").value("妻财"))
                .andExpect(jsonPath("$.data.analysisContext.ruleCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$.data.analysisContext.ruleCodes").value(org.hamcrest.Matchers.hasItems("USE_GOD_STRENGTH", "R010", "R011")))
                .andExpect(jsonPath("$.data.structuredResult.conflictSummaries").isArray());
    }

    @Test
    void shouldExposeKnowledgeSnippetsInHttpResponse() throws Exception {
        Book book = new Book();
        book.setTitle("HTTP知识片段测试");
        book.setAuthor("测试作者");
        book.setSourceType("TXT");
        book.setFilePath("/tmp/http-knowledge.txt");
        book.setFileSize(64L);
        book.setParseStatus("COMPLETED");
        book = bookRepository.save(book);

        BookChunk chunk = new BookChunk();
        chunk.setBookId(book.getId());
        chunk.setTaskId(1L);
        chunk.setChunkIndex(1);
        chunk.setChapterTitle("用神总论");
        chunk.setContent("用神宜旺相，不宜休囚。");
        chunk.setContentType("rule");
        chunk.setFocusTopic("用神");
        chunk.setTopicTagsJson("[\"用神\"]");
        chunk.setMetadataJson("{}");
        chunk.setCharCount(12);
        chunk.setSentenceCount(1);
        bookChunkRepository.save(chunk);

        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("我下个月工资会不会上涨");
        request.setQuestionCategory("收入");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        mockMvc.perform(post("/api/divinations/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysisContext.knowledgeSnippets").isArray())
                .andExpect(jsonPath("$.data.analysisContext.knowledgeSnippets[0]").value(org.hamcrest.Matchers.containsString("《")))
                .andExpect(jsonPath("$.data.analysisContext.knowledgeSnippets[0]").value(org.hamcrest.Matchers.containsString("用神宜旺相")))
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("用神宜旺相")));
    }

    @Test
    void shouldNormalizeSecondBatchCategoryThroughHttpEndpoint() throws Exception {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次买房手续能顺利办下来吗");
        request.setQuestionCategory("买房");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 9, 9, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        mockMvc.perform(post("/api/divinations/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chartSnapshot.questionCategory").value("房产"))
                .andExpect(jsonPath("$.data.chartSnapshot.useGod").value("父母"))
                .andExpect(jsonPath("$.data.analysisContext.questionCategory").value("房产"))
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("问房产")))
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("以父母为用神")));
    }
}
