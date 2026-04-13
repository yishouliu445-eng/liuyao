package com.yishou.liuyao.knowledge.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookChunkHybridSearchRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyScopeFiltersInsideCandidateCtes() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        BookChunkHybridSearchRepository repository = new BookChunkHybridSearchRepository(jdbcTemplate);

        repository.hybridSearch("合作 用神", "[0.1,0.2]", 2, 7L, "RULE", 5);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        String sql = sqlCaptor.getValue();

        assertEquals(2, countOccurrences(sql, "(:bookId IS NULL OR book_id = :bookId)"));
        assertEquals(2, countOccurrences(sql, "(:knowledgeType IS NULL OR knowledge_type = :knowledgeType)"));
        assertTrue(sql.contains("WHERE :ftsQueryText IS NOT NULL"));
    }

    @Test
    void shouldNormalizeChineseQueryBeforeFullTextSearch() {
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        BookChunkHybridSearchRepository repository = new BookChunkHybridSearchRepository(jdbcTemplate);

        repository.hybridSearch("用神怎么判断", "[0.1,0.2]", 2, null, null, 5);

        ArgumentCaptor<MapSqlParameterSource> paramsCaptor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).query(anyString(), paramsCaptor.capture(), any(RowMapper.class));
        MapSqlParameterSource params = paramsCaptor.getValue();

        assertEquals("用 神 怎 么 判 断", params.getValue("ftsQueryText"));
        assertEquals("用 神 怎 么 判 断", BookChunkHybridSearchRepository.normalizeFullTextQuery("用神怎么判断"));
    }

    private int countOccurrences(String content, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(pattern, index)) >= 0) {
            count++;
            index += pattern.length();
        }
        return count;
    }
}
