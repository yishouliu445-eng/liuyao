package com.yishou.liuyao.knowledge.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Locale;

@Repository
public class BookChunkHybridSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BookChunkHybridSearchRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BookChunkVectorSearchRow> hybridSearch(String queryText,
                                                       String queryVector,
                                                       int queryDim,
                                                       Long bookId,
                                                       String knowledgeType,
                                                       int limit) {
        int resolvedQueryDim = requirePositiveDimension(queryDim);
        String vectorCast = "vector(" + resolvedQueryDim + ")";

        // RRF (Reciprocal Rank Fusion) parameters
        int k = 60;
        String ftsQueryText = normalizeFullTextQuery(queryText);

        String sql = """
                WITH vector_results AS (
                    SELECT
                        id,
                        1 - ((embedding_vector::%s) <=> CAST(CAST(:queryVector AS text) AS %s)) AS similarity_score,
                        row_number() OVER (ORDER BY (embedding_vector::%s) <=> CAST(CAST(:queryVector AS text) AS %s)) as rank
                    FROM book_chunk
                    WHERE embedding_vector IS NOT NULL
                      AND embedding_dim = :queryDim
                      AND (:bookId IS NULL OR book_id = :bookId)
                      AND (:knowledgeType IS NULL OR knowledge_type = :knowledgeType)
                    LIMIT 200
                ),
                text_results AS (
                    SELECT id, row_number() OVER (ORDER BY ts_rank_cd(content_tsv, plainto_tsquery('simple', :ftsQueryText)) DESC) as rank
                    FROM book_chunk
                    WHERE :ftsQueryText IS NOT NULL
                      AND content_tsv @@ plainto_tsquery('simple', :ftsQueryText)
                      AND (:bookId IS NULL OR book_id = :bookId)
                      AND (:knowledgeType IS NULL OR knowledge_type = :knowledgeType)
                    LIMIT 200
                )
                SELECT 
                    bc.id,
                    bc.book_id,
                    bc.task_id,
                    bc.chapter_title,
                    bc.chunk_index,
                    bc.content,
                    bc.content_type,
                    bc.focus_topic,
                    bc.knowledge_type,
                    bc.has_timing_prediction,
                    bc.topic_tags_json,
                    bc.metadata_json,
                    bc.char_count,
                    bc.sentence_count,
                    bc.embedding_model,
                    bc.embedding_provider,
                    COALESCE(v.similarity_score, 0.0) as similarity_score,
                    (COALESCE(1.0 / (:k + v.rank), 0.0) + COALESCE(1.0 / (:k + t.rank), 0.0)) as hybrid_score
                FROM book_chunk bc
                LEFT JOIN vector_results v ON bc.id = v.id
                LEFT JOIN text_results t ON bc.id = t.id
                WHERE (v.id IS NOT NULL OR t.id IS NOT NULL)
                  AND (:bookId IS NULL OR bc.book_id = :bookId)
                  AND (:knowledgeType IS NULL OR bc.knowledge_type = :knowledgeType)
                ORDER BY hybrid_score DESC
                LIMIT :limit
                """.formatted(vectorCast, vectorCast, vectorCast, vectorCast);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("queryText", queryText, Types.VARCHAR)
                .addValue("ftsQueryText", ftsQueryText, Types.VARCHAR)
                .addValue("queryVector", queryVector, Types.VARCHAR)
                .addValue("queryDim", resolvedQueryDim, Types.INTEGER)
                .addValue("k", k, Types.INTEGER)
                .addValue("bookId", bookId, Types.BIGINT)
                .addValue("knowledgeType", knowledgeType, Types.VARCHAR)
                .addValue("limit", limit, Types.INTEGER);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new BookChunkVectorSearchRow(
                rs.getLong("id"),
                rs.getLong("book_id"),
                rs.getLong("task_id"),
                rs.getString("chapter_title"),
                rs.getInt("chunk_index"),
                rs.getString("content"),
                rs.getString("content_type"),
                rs.getString("focus_topic"),
                rs.getString("knowledge_type"),
                rs.getBoolean("has_timing_prediction"),
                rs.getString("topic_tags_json"),
                rs.getString("metadata_json"),
                rs.getInt("char_count"),
                rs.getInt("sentence_count"),
                rs.getString("embedding_model"),
                rs.getString("embedding_provider"),
                rs.getDouble("similarity_score")
        ));
    }

    static String normalizeFullTextQuery(String queryText) {
        if (queryText == null || queryText.isBlank()) {
            return null;
        }

        StringBuilder normalized = new StringBuilder();
        StringBuilder latinToken = new StringBuilder();
        queryText.codePoints().forEach(codePoint -> {
            if (Character.isWhitespace(codePoint) || isSeparator(codePoint)) {
                flushLatinToken(normalized, latinToken);
                return;
            }
            if (isHanCharacter(codePoint)) {
                flushLatinToken(normalized, latinToken);
                appendToken(normalized, new String(Character.toChars(codePoint)));
                return;
            }
            latinToken.appendCodePoint(Character.toLowerCase(codePoint));
        });
        flushLatinToken(normalized, latinToken);
        return normalized.length() == 0 ? null : normalized.toString();
    }

    private static boolean isSeparator(int codePoint) {
        return switch (codePoint) {
            case ',', '.', ';', ':', '!', '?', '"', '\'', '(', ')', '[', ']', '{', '}', '-', '_', '/', '\\',
                    '，', '。', '；', '：', '！', '？', '“', '”', '‘', '’', '（', '）', '《', '》', '、', '…' -> true;
            default -> false;
        };
    }

    private static boolean isHanCharacter(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }

    private static void flushLatinToken(StringBuilder normalized, StringBuilder latinToken) {
        if (latinToken.length() == 0) {
            return;
        }
        appendToken(normalized, latinToken.toString().toLowerCase(Locale.ROOT));
        latinToken.setLength(0);
    }

    private static void appendToken(StringBuilder normalized, String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        if (normalized.length() > 0) {
            normalized.append(' ');
        }
        normalized.append(token);
    }

    private int requirePositiveDimension(int queryDim) {
        if (queryDim <= 0) {
            throw new IllegalArgumentException("queryDim must be positive");
        }
        return queryDim;
    }
}
