package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.casecenter.repository.CaseAnalysisResultRepository;
import com.yishou.liuyao.casecenter.repository.CaseChartSnapshotRepository;
import com.yishou.liuyao.casecenter.repository.CaseRuleHitRepository;
import com.yishou.liuyao.casecenter.repository.DivinationCaseRepository;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.casecenter.domain.CaseChartSnapshot;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeResponse;
import com.yishou.liuyao.knowledge.domain.BookChunk;
import com.yishou.liuyao.knowledge.repository.BookChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=true")
class DivinationServiceIntegrationTest {

    @Autowired
    private DivinationService divinationService;

    @Autowired
    private DivinationCaseRepository divinationCaseRepository;

    @Autowired
    private CaseChartSnapshotRepository caseChartSnapshotRepository;

    @Autowired
    private CaseRuleHitRepository caseRuleHitRepository;

    @Autowired
    private CaseAnalysisResultRepository caseAnalysisResultRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookChunkRepository bookChunkRepository;

    @Test
    void shouldAnalyzeAndPersistMinimalFlow() {
        long initialCaseCount = divinationCaseRepository.count();
        long initialSnapshotCount = caseChartSnapshotRepository.count();
        long initialRuleHitCount = caseRuleHitRepository.count();
        long initialAnalysisCount = caseAnalysisResultRepository.count();

        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("我下个月工资会不会上涨");
        request.setQuestionCategory("收入");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        DivinationAnalyzeResponse response = divinationService.analyze(request);

        assertNotNull(response.getChartSnapshot());
        assertEquals("收入", response.getChartSnapshot().getQuestionCategory());
        assertEquals("妻财", response.getChartSnapshot().getUseGod());
        assertNotNull(response.getChartSnapshot().getMainHexagramCode());
        assertNotNull(response.getChartSnapshot().getMainUpperTrigram());
        assertNotNull(response.getChartSnapshot().getMainLowerTrigram());
        assertNotNull(response.getChartSnapshot().getPalace());
        assertFalse(response.getRuleHits().isEmpty());
        assertEquals("妻财", response.getRuleHits().get(0).getEvidence().get("useGod"));
        assertEquals("HIGH", response.getRuleHits().get(0).getImpactLevel());
        assertNotNull(response.getAnalysis());
        assertNotNull(response.getAnalysisContext());
        assertEquals("v1", response.getAnalysisContext().getContextVersion());
        assertEquals("妻财", response.getAnalysisContext().getUseGod());
        assertEquals(response.getChartSnapshot().getMainHexagram(), response.getAnalysisContext().getMainHexagram());
        assertNotNull(response.getAnalysisContext().getChartSnapshot());
        assertEquals(response.getChartSnapshot().getMainHexagram(), response.getAnalysisContext().getChartSnapshot().getMainHexagram());
        assertEquals("辰", response.getAnalysisContext().getChartSnapshot().getLines().get(0).getChangeBranch());
        org.junit.jupiter.api.Assertions.assertTrue(response.getAnalysis().contains("妻财"));
        org.junit.jupiter.api.Assertions.assertTrue(response.getAnalysis().contains("结构化上下文"));
        assertEquals(initialCaseCount + 1, divinationCaseRepository.count());
        assertEquals(initialSnapshotCount + 1, caseChartSnapshotRepository.count());
        assertEquals(initialRuleHitCount + response.getRuleHits().size(), caseRuleHitRepository.count());
        assertEquals(initialAnalysisCount + 1, caseAnalysisResultRepository.count());

        CaseChartSnapshot savedSnapshot = caseChartSnapshotRepository.findAll().stream()
                .max((left, right) -> Long.compare(left.getId(), right.getId()))
                .orElseThrow();
        assertEquals(response.getChartSnapshot().getMainHexagram(), savedSnapshot.getMainHexagram());
        assertEquals(response.getChartSnapshot().getChangedHexagram(), savedSnapshot.getChangedHexagram());
        assertEquals(response.getChartSnapshot().getPalace(), savedSnapshot.getPalace());
        assertEquals(response.getChartSnapshot().getUseGod(), savedSnapshot.getUseGod());
    }

    @Test
    void shouldAttachKnowledgeSnippetsIntoAnalysisContext() {
        Book book = new Book();
        book.setTitle("导入知识测试");
        book.setAuthor("测试作者");
        book.setSourceType("TXT");
        book.setFilePath("/tmp/knowledge.txt");
        book.setFileSize(128L);
        book.setParseStatus("COMPLETED");
        book = bookRepository.save(book);

        BookChunk useGodChunk = new BookChunk();
        useGodChunk.setBookId(book.getId());
        useGodChunk.setTaskId(1L);
        useGodChunk.setChunkIndex(1);
        useGodChunk.setChapterTitle("用神总论");
        useGodChunk.setContent("用神宜旺相，不宜休囚。");
        useGodChunk.setContentType("rule");
        useGodChunk.setFocusTopic("用神");
        useGodChunk.setTopicTagsJson("[\"用神\"]");
        useGodChunk.setMetadataJson("{}");
        useGodChunk.setCharCount(12);
        useGodChunk.setSentenceCount(1);
        bookChunkRepository.save(useGodChunk);

        BookChunk monthBreakChunk = new BookChunk();
        monthBreakChunk.setBookId(book.getId());
        monthBreakChunk.setTaskId(1L);
        monthBreakChunk.setChunkIndex(2);
        monthBreakChunk.setChapterTitle("月破");
        monthBreakChunk.setContent("月破之爻，生扶亦弱。");
        monthBreakChunk.setContentType("rule");
        monthBreakChunk.setFocusTopic("月破");
        monthBreakChunk.setTopicTagsJson("[\"月破\"]");
        monthBreakChunk.setMetadataJson("{}");
        monthBreakChunk.setCharCount(11);
        monthBreakChunk.setSentenceCount(1);
        bookChunkRepository.save(monthBreakChunk);

        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("我下个月工资会不会上涨");
        request.setQuestionCategory("收入");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        DivinationAnalyzeResponse response = divinationService.analyze(request);

        assertFalse(response.getAnalysisContext().getKnowledgeSnippets().isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(
                response.getAnalysisContext().getKnowledgeSnippets().stream()
                        .anyMatch(item -> item.contains("用神宜旺相"))
        );
        org.junit.jupiter.api.Assertions.assertTrue(response.getAnalysis().contains("用神宜旺相"));
    }
}
