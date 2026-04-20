package com.yishou.liuyao.knowledge.controller;

import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRepository;
import com.yishou.liuyao.knowledge.repository.BookChunkVectorSearchRow;
import com.yishou.liuyao.knowledge.service.KnowledgeQueryEmbeddingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:knowledge-semantic;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class KnowledgeSemanticSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeQueryEmbeddingService knowledgeQueryEmbeddingService;

    @MockBean
    private BookChunkVectorSearchRepository bookChunkVectorSearchRepository;

    @Test
    void shouldReturnSemanticChunkMatchesWhenVectorSearchIsAvailable() throws Exception {
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(true);
        when(knowledgeQueryEmbeddingService.embed("用神怎么判断")).thenReturn(List.of(0.1, 0.2));
        when(bookChunkVectorSearchRepository.search(eq(3L), eq("用神"), eq("[0.1,0.2]"), eq(2), eq(5)))
                .thenReturn(List.of(new BookChunkVectorSearchRow(
                        99L,
                        3L,
                        3L,
                        "用神总论",
                        12,
                        "用神宜旺相，不宜休囚。",
                        "rule",
                        "用神",
                        "RULE",
                        false,
                        "[\"用神\"]",
                        "{\"split_reason\":\"trigger_split\"}",
                        12,
                        1,
                        "text-embedding-v4",
                        "dashscope",
                        0.9123
                )));

        mockMvc.perform(get("/api/knowledge/chunks/semantic")
                        .param("queryText", "用神怎么判断")
                        .param("bookId", "3")
                        .param("topicTag", "用神")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].bookId").value(3))
                .andExpect(jsonPath("$.data.items[0].chapterTitle").value("用神总论"))
                .andExpect(jsonPath("$.data.items[0].embeddingProvider").value("dashscope"))
                .andExpect(jsonPath("$.data.items[0].similarityScore").value(0.9123))
                .andExpect(jsonPath("$.data.items[0].metadata.splitReason").value("trigger_split"));
    }

    @Test
    void shouldReturnEmptySemanticChunkMatchesWhenVectorSearchIsUnavailable() throws Exception {
        when(bookChunkVectorSearchRepository.supportsVectorSearch()).thenReturn(false);

        mockMvc.perform(get("/api/knowledge/chunks/semantic")
                        .param("queryText", "用神怎么判断"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }
}
