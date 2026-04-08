package com.yishou.liuyao.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.knowledge.domain.BookChunk;
import com.yishou.liuyao.knowledge.mapper.KnowledgeMapper;
import com.yishou.liuyao.knowledge.repository.BookChunkRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRow;
import com.yishou.liuyao.rule.usegod.QuestionCategoryNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeSearchServiceTest {

    @Mock
    private BookChunkRepository bookChunkRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private KnowledgeImportService knowledgeImportService;

    @Mock
    private KnowledgeQueryEmbeddingService knowledgeQueryEmbeddingService;

    @Mock
    private BookChunkVectorSearchRepository bookChunkVectorSearchRepository;

    @Test
    void shouldPreferSemanticAndTxtSnippetsWhenAvailable() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper()
        );
        Book txtBook = new Book();
        txtBook.setTitle("增删卜易");
        txtBook.setSourceType("TXT");
        txtBook.setId(1L);

        Book pdfBook = new Book();
        pdfBook.setTitle("扫描版资料");
        pdfBook.setSourceType("PDF");
        pdfBook.setId(2L);

        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(true);
        when(knowledgeQueryEmbeddingService.embed(any())).thenReturn(List.of(0.1D, 0.2D));
        when(bookChunkVectorSearchRepository.search(eq(null), eq(null), eq("[0.1,0.2]"), eq(2), eq(4)))
                .thenReturn(List.of(
                        new BookChunkVectorSearchRow(
                                21L, 1L, 1L, "用神总论", 1,
                                "用神宜旺相，不宜休囚。",
                                "rule", "用神", "[\"用神\"]", "{}",
                                12, 1, "text-embedding-v4", "dashscope", 0.97
                        ),
                        new BookChunkVectorSearchRow(
                                22L, 2L, 1L, "PDF片段", 2,
                                "世应宜分彼我。",
                                "rule", "世应", "[\"世应\"]", "{}",
                                10, 1, "text-embedding-v4", "dashscope", 0.86
                        )
                ));
        when(bookRepository.findAllById(any())).thenReturn(List.of(txtBook, pdfBook));

        BookChunk txtChunk = new BookChunk();
        txtChunk.setId(31L);
        txtChunk.setBookId(1L);
        txtChunk.setChapterTitle("用神总论");
        txtChunk.setContent("用神宜旺相，不宜休囚。");
        txtChunk.setFocusTopic("用神");
        txtChunk.setCharCount(12);

        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("用神")).thenReturn(List.of(txtChunk));
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("动爻")).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("月破")).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("世应")).thenReturn(List.of());

        List<String> snippets = knowledgeSearchService.suggestKnowledgeSnippets("收入", "妻财", List.of("R003"), 4);

        assertEquals(2, snippets.size());
        assertTrue(snippets.get(0).contains("《增删卜易》"));
        assertTrue(snippets.get(0).contains("用神宜旺相"));
    }

    @Test
    void shouldMapSecondBatchCategoriesIntoExistingRecallTopics() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper()
        );
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(false);
        when(bookRepository.findAllById(any())).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("用神")).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("动爻")).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("月破")).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("世应")).thenReturn(List.of());
        List<String> snippets = knowledgeSearchService.suggestKnowledgeSnippets("投资", "妻财", List.of("R003"), 4);

        assertTrue(snippets.isEmpty());
    }

    @Test
    void shouldAddCategorySpecificHintsIntoSemanticQuery() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper()
        );
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(true);
        when(knowledgeQueryEmbeddingService.embed(contains("房屋 手续 文书 成交"))).thenReturn(List.of(0.3D, 0.4D));
        when(bookChunkVectorSearchRepository.search(eq(null), eq(null), eq("[0.3,0.4]"), eq(2), eq(3)))
                .thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("用神")).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("世应")).thenReturn(List.of());

        List<String> snippets = knowledgeSearchService.suggestKnowledgeSnippets("房产", "父母", List.of("R010"), 3);

        assertTrue(snippets.isEmpty());
        verify(knowledgeQueryEmbeddingService).embed(contains("房屋 手续 文书 成交"));
    }
}
