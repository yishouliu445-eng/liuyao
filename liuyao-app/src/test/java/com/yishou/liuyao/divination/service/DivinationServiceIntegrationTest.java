package com.yishou.liuyao.divination.service;

import com.yishou.liuyao.casecenter.repository.CaseAnalysisResultRepository;
import com.yishou.liuyao.casecenter.repository.CaseChartSnapshotRepository;
import com.yishou.liuyao.casecenter.repository.CaseRuleHitRepository;
import com.yishou.liuyao.casecenter.repository.DivinationCaseRepository;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.casecenter.domain.CaseAnalysisResult;
import com.yishou.liuyao.casecenter.domain.CaseChartSnapshot;
import com.yishou.liuyao.casecenter.domain.CaseRuleHit;
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
        assertNotNull(response.getStructuredResult());
        assertNotNull(response.getStructuredResult().getScore());
        assertNotNull(response.getStructuredResult().getResultLevel());
        assertNotNull(response.getStructuredResult().getEffectiveScore());
        assertNotNull(response.getStructuredResult().getEffectiveResultLevel());
        assertFalse(response.getStructuredResult().getCategorySummaries().isEmpty());
        assertEquals("YONGSHEN_STATE", response.getStructuredResult().getCategorySummaries().get(0).getCategory());
        assertNotNull(response.getStructuredResult().getConflictSummaries());
        assertNotNull(response.getStructuredResult().getEffectiveRuleCodes());
        assertNotNull(response.getStructuredResult().getSuppressedRuleCodes());
        assertEquals("妻财", response.getRuleHits().get(0).getEvidence().get("useGod"));
        assertEquals("HIGH", response.getRuleHits().get(0).getImpactLevel());
        assertNotNull(response.getRuleHits().get(0).getRuleId());
        assertNotNull(response.getAnalysis());
        assertNotNull(response.getAnalysisContext());
        assertEquals("v1", response.getAnalysisContext().getContextVersion());
        assertEquals("妻财", response.getAnalysisContext().getUseGod());
        assertEquals(response.getChartSnapshot().getMainHexagram(), response.getAnalysisContext().getMainHexagram());
        assertNotNull(response.getAnalysisContext().getChartSnapshot());
        assertEquals(response.getChartSnapshot().getMainHexagram(), response.getAnalysisContext().getChartSnapshot().getMainHexagram());
        assertEquals("辰", response.getAnalysisContext().getChartSnapshot().getLines().get(0).getChangeBranch());
        org.junit.jupiter.api.Assertions.assertTrue(response.getAnalysis().contains("妻财"));
        org.junit.jupiter.api.Assertions.assertTrue(response.getAnalysis().contains("有效评分"));
        assertEquals(initialCaseCount + 1, divinationCaseRepository.count());
        assertEquals(initialSnapshotCount + 1, caseChartSnapshotRepository.count());
        assertEquals(initialRuleHitCount + response.getRuleHits().size(), caseRuleHitRepository.count());
        assertEquals(initialAnalysisCount + 1, caseAnalysisResultRepository.count());

        CaseChartSnapshot savedSnapshot = caseChartSnapshotRepository.findAll().stream()
                .max((left, right) -> Long.compare(left.getId(), right.getId()))
                .orElseThrow();
        CaseAnalysisResult savedAnalysis = caseAnalysisResultRepository.findAll().stream()
                .max((left, right) -> Long.compare(left.getId(), right.getId()))
                .orElseThrow();
        CaseRuleHit savedRuleHit = caseRuleHitRepository.findAll().stream()
                .max((left, right) -> Long.compare(left.getId(), right.getId()))
                .orElseThrow();
        assertEquals(response.getChartSnapshot().getMainHexagram(), savedSnapshot.getMainHexagram());
        assertEquals(response.getChartSnapshot().getChangedHexagram(), savedSnapshot.getChangedHexagram());
        assertEquals(response.getChartSnapshot().getPalace(), savedSnapshot.getPalace());
        assertEquals(response.getChartSnapshot().getUseGod(), savedSnapshot.getUseGod());
        assertEquals(response.getStructuredResult().getScore(), savedAnalysis.getScore());
        assertEquals(response.getStructuredResult().getResultLevel(), savedAnalysis.getResultLevel());
        assertNotNull(savedAnalysis.getStructuredResultJson());
        assertNotNull(savedRuleHit.getRuleId());
        assertNotNull(savedRuleHit.getCategory());
        assertNotNull(savedRuleHit.getScoreDelta());
        assertNotNull(savedRuleHit.getTagsJson());
    }

    @Test
    void shouldAttachKnowledgeSnippetsIntoAnalysisContext() {
        Book txtBook = new Book();
        txtBook.setTitle("增删卜易");
        txtBook.setAuthor("测试作者");
        txtBook.setSourceType("TXT");
        txtBook.setFilePath("/tmp/knowledge.txt");
        txtBook.setFileSize(128L);
        txtBook.setParseStatus("COMPLETED");
        txtBook = bookRepository.save(txtBook);

        Book pdfBook = new Book();
        pdfBook.setTitle("扫描版六爻资料");
        pdfBook.setAuthor("测试作者");
        pdfBook.setSourceType("PDF");
        pdfBook.setFilePath("/tmp/knowledge.pdf");
        pdfBook.setFileSize(256L);
        pdfBook.setParseStatus("COMPLETED");
        pdfBook = bookRepository.save(pdfBook);

        BookChunk useGodChunk = new BookChunk();
        useGodChunk.setBookId(txtBook.getId());
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

        BookChunk pdfChunk = new BookChunk();
        pdfChunk.setBookId(pdfBook.getId());
        pdfChunk.setTaskId(2L);
        pdfChunk.setChunkIndex(1);
        pdfChunk.setChapterTitle("用神概述");
        pdfChunk.setContent("扫描版资料中的用神说明。");
        pdfChunk.setContentType("rule");
        pdfChunk.setFocusTopic("用神");
        pdfChunk.setTopicTagsJson("[\"用神\"]");
        pdfChunk.setMetadataJson("{}");
        pdfChunk.setCharCount(12);
        pdfChunk.setSentenceCount(1);
        bookChunkRepository.save(pdfChunk);

        BookChunk monthBreakChunk = new BookChunk();
        monthBreakChunk.setBookId(txtBook.getId());
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
        org.junit.jupiter.api.Assertions.assertTrue(response.getAnalysisContext().getKnowledgeSnippets().get(0).contains("《"));
        org.junit.jupiter.api.Assertions.assertTrue(response.getAnalysis().contains("用神宜旺相"));
    }

    @Test
    void shouldNormalizeRealEstateAliasAndSelectFuMu() {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次买房手续能办好吗");
        request.setQuestionCategory("买房");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 6, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        DivinationAnalyzeResponse response = divinationService.analyze(request);

        assertEquals("房产", response.getChartSnapshot().getQuestionCategory());
        assertEquals("父母", response.getChartSnapshot().getUseGod());
        assertEquals("房产", response.getAnalysisContext().getQuestionCategory());
        org.junit.jupiter.api.Assertions.assertTrue(response.getAnalysis().contains("问房产"));
        org.junit.jupiter.api.Assertions.assertTrue(response.getAnalysis().contains("以父母为用神"));
    }
}
