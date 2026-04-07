package com.yishou.liuyao.knowledge.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Repository
public class BookChunkVectorSearchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private volatile Boolean vectorSearchSupported;

    public BookChunkVectorSearchRepository(NamedParameterJdbcTemplate jdbcTemplate,
                                           DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public boolean supportsVectorSearch() {
        if (vectorSearchSupported != null) {
            return vectorSearchSupported;
        }
        synchronized (this) {
            if (vectorSearchSupported != null) {
                return vectorSearchSupported;
            }
            vectorSearchSupported = detectVectorSearchSupport();
            return vectorSearchSupported;
        }
    }

    public List<BookChunkVectorSearchRow> search(Long bookId,
                                                 String topicTag,
                                                 String queryVector,
                                                 int queryDim,
                                                 int limit) {
        int resolvedQueryDim = requirePositiveDimension(queryDim);
        String vectorCast = "vector(" + resolvedQueryDim + ")";
        String sql = """
                SELECT
                    id,
                    book_id,
                    task_id,
                    chapter_title,
                    chunk_index,
                    content,
                    content_type,
                    focus_topic,
                    topic_tags_json,
                    metadata_json,
                    char_count,
                    sentence_count,
                    embedding_model,
                    embedding_provider,
                    1 - ((embedding_vector::%s) <=> CAST(CAST(:queryVector AS text) AS %s)) AS similarity_score
                FROM book_chunk
                WHERE embedding_vector IS NOT NULL
                  AND embedding_dim = :queryDim
                  AND (:bookId IS NULL OR book_id = :bookId)
                  AND (:topicTag IS NULL OR focus_topic = :topicTag)
                ORDER BY (embedding_vector::%s) <=> CAST(CAST(:queryVector AS text) AS %s), id DESC
                LIMIT :limit
                """.formatted(vectorCast, vectorCast, vectorCast, vectorCast);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("bookId", bookId, Types.BIGINT)
                .addValue("topicTag", topicTag == null || topicTag.isBlank() ? null : topicTag, Types.VARCHAR)
                .addValue("queryVector", queryVector, Types.VARCHAR)
                .addValue("queryDim", resolvedQueryDim, Types.INTEGER)
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
                rs.getString("topic_tags_json"),
                rs.getString("metadata_json"),
                rs.getInt("char_count"),
                rs.getInt("sentence_count"),
                rs.getString("embedding_model"),
                rs.getString("embedding_provider"),
                rs.getDouble("similarity_score")
        ));
    }

    private int requirePositiveDimension(int queryDim) {
        if (queryDim <= 0) {
            throw new IllegalArgumentException("queryDim must be positive");
        }
        return queryDim;
    }

    private boolean detectVectorSearchSupport() {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(readDatabaseProduct(connection))) {
                return false;
            }
            try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, "book_chunk", "embedding_vector")) {
                while (resultSet.next()) {
                    String typeName = resultSet.getString("TYPE_NAME");
                    if ("vector".equalsIgnoreCase(typeName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (SQLException exception) {
            return false;
        }
    }

    private String readDatabaseProduct(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        return metadata == null ? "" : metadata.getDatabaseProductName();
    }
}
