package com.yishou.liuyao.knowledge.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.evaluation.service.EvaluationRunService;
import com.yishou.liuyao.knowledge.domain.BookChunk;
import com.yishou.liuyao.knowledge.dto.BookChunkQueryResponse;
import com.yishou.liuyao.knowledge.mapper.KnowledgeMapper;
import com.yishou.liuyao.knowledge.repository.BookChunkHybridSearchRepository;
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
import static org.mockito.Mockito.atLeastOnce;
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

    @Mock
    private BookChunkHybridSearchRepository bookChunkHybridSearchRepository;

    @Mock
    private EvaluationRunService evaluationRunService;

    @Test
    void shouldPreferSemanticAndTxtSnippetsWhenAvailable() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper(),
                evaluationRunService
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
        when(bookChunkHybridSearchRepository.hybridSearch(eq("问类:收入 关注:工资 收益 回款 用神:妻财 规则:R003"), eq("[0.1,0.2]"), eq(2), eq(null), eq(null), eq(4)))
                .thenReturn(List.of(
                        new BookChunkVectorSearchRow(
                                21L, 1L, 1L, "用神总论", 1,
                                "用神宜旺相，不宜休囚。",
                                "rule", "用神", "RULE", false, "[\"用神\"]", "{}",
                                12, 1, "text-embedding-v4", "dashscope", 0.97
                        ),
                        new BookChunkVectorSearchRow(
                                22L, 2L, 1L, "PDF片段", 2,
                                "世应宜分彼我。",
                                "rule", "世应", "RULE", false, "[\"世应\"]", "{}",
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
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper(),
                evaluationRunService
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
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper(),
                evaluationRunService
        );
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(true);
        when(knowledgeQueryEmbeddingService.embed(contains("房屋 手续 文书 成交"))).thenReturn(List.of(0.3D, 0.4D));
        when(bookChunkHybridSearchRepository.hybridSearch(eq("问类:房产 关注:房屋 手续 文书 成交 用神:父母 规则:R010"), eq("[0.3,0.4]"), eq(2), eq(null), eq(null), eq(3)))
                .thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("用神")).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc("世应")).thenReturn(List.of());

        List<String> snippets = knowledgeSearchService.suggestKnowledgeSnippets("房产", "父母", List.of("R010"), 3);

        assertTrue(snippets.isEmpty());
        verify(knowledgeQueryEmbeddingService).embed(contains("房屋 手续 文书 成交"));
    }

    @Test
    void shouldFilterOutSemanticMatchesBelowSimilarityThreshold() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper(),
                evaluationRunService
        );
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(true);
        when(knowledgeQueryEmbeddingService.embed("用神判断")).thenReturn(List.of(0.1D, 0.2D));
        when(bookChunkVectorSearchRepository.search(eq(null), eq("用神"), eq("[0.1,0.2]"), eq(2), eq(5)))
                .thenReturn(List.of(
                        new BookChunkVectorSearchRow(
                                21L, 1L, 1L, "命中片段", 1,
                                "用神宜旺相。", "rule", "用神", "RULE", false,
                                "[\"用神\"]", "{}", 8, 1, "text-embedding-v4", "dashscope", 0.82
                        ),
                        new BookChunkVectorSearchRow(
                                22L, 1L, 1L, "应被过滤", 2,
                                "低相似度噪声片段。", "rule", "用神", "RULE", false,
                                "[\"用神\"]", "{}", 9, 1, "text-embedding-v4", "dashscope", 0.64
                        )
                ));

        BookChunkQueryResponse response = knowledgeSearchService.semanticSearchChunks("用神判断", null, "用神", 5);

        assertEquals(1, response.getItems().size());
        assertEquals("命中片段", response.getItems().get(0).getChapterTitle());
        assertEquals(0.82, response.getItems().get(0).getSimilarityScore());
    }

    @Test
    void shouldExposeExpandedCoreRuleTopicsInPreview() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper(),
                evaluationRunService
        );

        List<String> topics = knowledgeSearchService.buildImportTopicsPreview().getTopics();

        assertTrue(topics.contains("伏神"));
        assertTrue(topics.contains("飞神"));
        assertTrue(topics.contains("旬空"));
        assertTrue(topics.contains("化进"));
        assertTrue(topics.contains("化退"));
        assertTrue(topics.contains("伏吟"));
        assertTrue(topics.contains("反吟"));
        assertTrue(topics.contains("应期"));
        assertTrue(topics.contains("神煞"));
        assertTrue(topics.contains("驿马"));
        assertTrue(topics.contains("桃花"));
        assertTrue(topics.contains("贵人"));
        assertTrue(topics.contains("文昌"));
        assertTrue(topics.contains("将星"));
        assertTrue(topics.contains("劫煞"));
        assertTrue(topics.contains("灾煞"));
    }

    @Test
    void shouldRecallNewRuleTopicsWhenRuleCodesExpand() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper(),
                evaluationRunService
        );
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(false);
        when(bookRepository.findAllById(any())).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(any())).thenReturn(List.of());

        List<String> snippets = knowledgeSearchService.suggestKnowledgeSnippets(
                "收入",
                "妻财",
                List.of("FU_SHEN_FLY_SHEN", "USE_GOD_EMPTY"),
                4
        );

        assertTrue(snippets.isEmpty());
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("伏神");
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("旬空");
    }

    @Test
    void shouldRecallPhaseTwoTimingTopicsWhenRuleCodesExpand() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper(),
                evaluationRunService
        );
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(false);
        when(bookRepository.findAllById(any())).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(any())).thenReturn(List.of());

        List<String> snippets = knowledgeSearchService.suggestKnowledgeSnippets(
                "收入",
                "妻财",
                List.of("FAN_FU_YIN", "TIMING_SIGNAL"),
                4
        );

        assertTrue(snippets.isEmpty());
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("伏吟");
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("反吟");
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("应期");
    }

    @Test
    void shouldRecallShenShaTopicsWhenRuleCodesExpand() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper(),
                evaluationRunService
        );
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(false);
        when(bookRepository.findAllById(any())).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(any())).thenReturn(List.of());

        List<String> snippets = knowledgeSearchService.suggestKnowledgeSnippets(
                "合作",
                "妻财",
                List.of("SHEN_SHA", "R205", "R206", "R207"),
                4
        );

        assertTrue(snippets.isEmpty());
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("神煞");
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("贵人");
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("驿马");
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("桃花");
    }

    @Test
    void shouldRecallExpandedShenShaTopicsWhenRuleCodesExpand() {
        KnowledgeSearchService knowledgeSearchService = new KnowledgeSearchService(
                bookChunkRepository,
                bookRepository,
                knowledgeImportService,
                new KnowledgeMapper(),
                knowledgeQueryEmbeddingService,
                bookChunkVectorSearchRepository,
                bookChunkHybridSearchRepository,
                new QuestionCategoryNormalizer(),
                new ObjectMapper(),
                evaluationRunService
        );
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(false);
        when(bookRepository.findAllById(any())).thenReturn(List.of());
        when(bookChunkRepository.findTop20ByFocusTopicOrderByIdDesc(any())).thenReturn(List.of());

        List<String> snippets = knowledgeSearchService.suggestKnowledgeSnippets(
                "考试",
                "妻财",
                List.of("R208", "R209", "R211", "R212"),
                4
        );

        assertTrue(snippets.isEmpty());
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("文昌");
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("将星");
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("灾煞");
        verify(bookChunkRepository, atLeastOnce()).findTop20ByFocusTopicOrderByIdDesc("劫煞");
    }
}
