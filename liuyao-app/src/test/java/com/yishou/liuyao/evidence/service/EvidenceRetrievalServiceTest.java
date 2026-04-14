package com.yishou.liuyao.evidence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import com.yishou.liuyao.evidence.dto.EvidenceSelectionResult;
import com.yishou.liuyao.knowledge.repository.BookChunkHybridSearchRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRow;
import com.yishou.liuyao.knowledge.service.KnowledgeQueryEmbeddingService;
import com.yishou.liuyao.rule.usegod.QuestionCategoryNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceRetrievalServiceTest {

    @Mock
    private BookChunkRepository bookChunkRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private KnowledgeQueryEmbeddingService knowledgeQueryEmbeddingService;
    @Mock
    private BookChunkVectorSearchRepository bookChunkVectorSearchRepository;
    @Mock
    private BookChunkHybridSearchRepository bookChunkHybridSearchRepository;

    @Test
    void shouldReturnStructuredEvidenceHitsFromHybridSearch() {
        EvidenceRetrievalService service = new EvidenceRetrievalService(
                bookChunkRepository,
                bookRepository,
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper()
        );

        Book txtBook = new Book();
        txtBook.setId(1L);
        txtBook.setTitle("增删卜易");
        txtBook.setSourceType("TXT");

        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(true);
        when(knowledgeQueryEmbeddingService.embed("问类:收入 关注:工资 收益 回款 用神:妻财 规则:R003"))
                .thenReturn(List.of(0.1D, 0.2D));
        when(bookChunkHybridSearchRepository.hybridSearch(
                eq("问类:收入 关注:工资 收益 回款 用神:妻财 规则:R003"),
                eq("[0.1,0.2]"),
                eq(2),
                eq(null),
                eq(null),
                eq(4)))
                .thenReturn(List.of(
                        new BookChunkVectorSearchRow(
                                21L, 1L, 1L, "用神总论", 1,
                                "用神旺相则事可成。",
                                "rule", "用神", "RULE", false, "[\"用神\"]", "{}",
                                10, 1, "text-embedding-v4", "dashscope", 0.96
                        )
                ));
        when(bookRepository.findAllById(any())).thenReturn(List.of(txtBook));

        EvidenceSelectionResult result = service.retrieveInitial("收入", "妻财", List.of("R003"), 4);

        assertEquals(1, result.getHits().size());
        EvidenceHit hit = result.getHits().get(0);
        assertEquals(21L, hit.getChunkId());
        assertEquals(1L, hit.getBookId());
        assertEquals("增删卜易", hit.getSourceTitle());
        assertEquals("用神总论", hit.getChapterTitle());
        assertEquals("RULE", hit.getKnowledgeType());
        assertEquals("用神旺相则事可成。", hit.getContent());
        assertEquals(1, hit.getRank());
        assertEquals("chunk:21", hit.getCitationId());
        assertTrue(result.toPromptSnippets().get(0).contains("《增删卜易》"));
    }

    @Test
    void shouldUseFollowUpQuestionInFollowUpSemanticRetrieval() {
        EvidenceRetrievalService service = new EvidenceRetrievalService(
                bookChunkRepository,
                bookRepository,
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper()
        );

        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(true);
        when(knowledgeQueryEmbeddingService.embed(contains("对方迟迟不回消息")))
                .thenReturn(List.of(0.3D, 0.4D));
        when(bookChunkHybridSearchRepository.hybridSearch(
                eq("问类:合作 关注:对方 配合 履约 用神:应爻 追问:对方迟迟不回消息怎么办"),
                eq("[0.3,0.4]"),
                eq(2),
                eq(null),
                eq(null),
                eq(3)))
                .thenReturn(List.of());

        EvidenceSelectionResult result = service.retrieveFollowUp("合作", "应爻", "对方迟迟不回消息怎么办", 3);

        assertTrue(result.getHits().isEmpty());
        verify(knowledgeQueryEmbeddingService).embed(contains("对方迟迟不回消息怎么办"));
    }
}
