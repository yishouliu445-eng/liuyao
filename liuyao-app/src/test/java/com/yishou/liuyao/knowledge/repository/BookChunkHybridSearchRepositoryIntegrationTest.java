package com.yishou.liuyao.knowledge.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class BookChunkHybridSearchRepositoryIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg15")
    )
            .withDatabaseName("liuyao")
            .withUsername("test")
            .withPassword("test");

    @BeforeAll
    static void startContainer() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookChunkHybridSearchRepository hybridSearchRepository;

    @Autowired
    private BookChunkVectorSearchRepository vectorSearchRepository;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.update("DELETE FROM book_chunk");
    }

    @Test
    void shouldUsePgvectorContainerForHybridSearchIntegration() {
        assertTrue(vectorSearchRepository.supportsVectorSearch());
    }

    @Test
    void shouldKeepScopedMatchesWhenGlobalCorpusExceedsTop200Candidates() {
        insertChunk(1L, 101L, 1, "目标章节", "合作用神判断要结合世应和动爻", "rule", "用神",
                "RULE", true, "[0.95,0.05]");

        for (int i = 0; i < 205; i++) {
            insertChunk(2L, 2000L + i, i, "干扰章节" + i, "合作用神判断示例 " + i, "rule", "用神",
                    "RULE", false, "[1.0,0.0]");
        }

        List<BookChunkVectorSearchRow> rows = hybridSearchRepository.hybridSearch(
                "合作用神",
                "[1.0,0.0]",
                2,
                1L,
                "RULE",
                5
        );

        assertEquals(1, rows.size());
        assertEquals(1L, rows.get(0).bookId());
        assertEquals("RULE", rows.get(0).knowledgeType());
        assertTrue(rows.get(0).content().contains("世应"));
    }

    @Test
    void shouldRecallChineseQueryThroughFullTextNormalization() {
        insertChunk(1L, 301L, 1, "目标章节", "用神判断要结合世应和动爻", "rule", "用神",
                "RULE", true, "[0.9,0.1]");
        insertChunk(1L, 302L, 2, "干扰章节", "合作推进节奏仍需观察", "rule", "合作",
                "RULE", false, "[1.0,0.0]");

        List<BookChunkVectorSearchRow> rows = hybridSearchRepository.hybridSearch(
                "用神判断",
                "[1.0,0.0]",
                2,
                null,
                "RULE",
                5
        );

        assertFalse(rows.isEmpty());
        assertEquals("用神", rows.get(0).focusTopic());
        assertTrue(rows.get(0).content().contains("用神判断"));
    }

    private void insertChunk(Long bookId,
                             Long taskId,
                             int chunkIndex,
                             String chapterTitle,
                             String content,
                             String contentType,
                             String focusTopic,
                             String knowledgeType,
                             boolean hasTimingPrediction,
                             String embeddingVector) {
        jdbcTemplate.update("""
                INSERT INTO book_chunk (
                    book_id,
                    task_id,
                    chapter_title,
                    chunk_index,
                    content,
                    content_type,
                    focus_topic,
                    knowledge_type,
                    has_timing_prediction,
                    topic_tags_json,
                    metadata_json,
                    char_count,
                    sentence_count,
                    embedding_json,
                    embedding_vector,
                    embedding_model,
                    embedding_provider,
                    embedding_dim,
                    embedding_version
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    '[]', '{}', ?, ?,
                    ?, CAST(CAST(? AS text) AS vector(2)),
                    'test-model', 'test-provider', 2, 'v1'
                )
                """,
                bookId,
                taskId,
                chapterTitle,
                chunkIndex,
                content,
                contentType,
                focusTopic,
                knowledgeType,
                hasTimingPrediction,
                content.length(),
                1,
                embeddingVector,
                embeddingVector
        );
    }
}
