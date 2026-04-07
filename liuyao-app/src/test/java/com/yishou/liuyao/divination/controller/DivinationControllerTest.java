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
                .andExpect(jsonPath("$.data.ruleHits[3].evidence.mainUpperTrigram").isNotEmpty())
                .andExpect(jsonPath("$.data.ruleHits[3].evidence.targetCount").value(2))
                .andExpect(jsonPath("$.data.ruleHits[3].evidence.targetSummary").isArray())
                .andExpect(jsonPath("$.data.analysisContext.contextVersion").value("v1"))
                .andExpect(jsonPath("$.data.analysisContext.useGod").value("父母"))
                .andExpect(jsonPath("$.data.analysisContext.mainHexagram").value("山火贲"))
                .andExpect(jsonPath("$.data.analysisContext.chartSnapshot.mainHexagram").value("山火贲"))
                .andExpect(jsonPath("$.data.analysisContext.chartSnapshot.lines[0].changeBranch").value("辰"))
                .andExpect(jsonPath("$.data.analysisContext.ruleCodes").isArray())
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("结构化上下文")));
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
                .andExpect(jsonPath("$.data.analysisContext.ruleCount").value(6));
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
                .andExpect(jsonPath("$.data.analysisContext.knowledgeSnippets[0]").value(org.hamcrest.Matchers.containsString("用神宜旺相")))
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("用神宜旺相")));
    }
}
