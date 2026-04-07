package com.yishou.liuyao.book.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.flyway.enabled=true")
class BookImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateBookImportRequestAndExposeImportPreparationEndpoints() throws Exception {
        Map<String, Object> request = Map.of(
                "title", "增删卜易摘录",
                "author", "野鹤老人",
                "sourceType", "DOCX",
                "filePath", "/data/liuyao/books/zengshanbuyi.docx",
                "fileSize", 40960,
                "remark", "首批导入：用神、世应、六亲",
                "topicTags", List.of("用神", "世应", "六亲")
        );

        mockMvc.perform(post("/api/books/import-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookId").isNumber())
                .andExpect(jsonPath("$.data.taskId").isNumber())
                .andExpect(jsonPath("$.data.parseStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.taskStatus").value("PENDING"));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("增删卜易摘录"))
                .andExpect(jsonPath("$.data[0].parseStatus").value("PENDING"))
                .andExpect(jsonPath("$.data[0].sourceType").value("DOCX"));

        mockMvc.perform(get("/api/tasks/doc-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].taskType").value("BOOK_PARSE"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].payloadJson").isNotEmpty())
                .andExpect(jsonPath("$.data[0].lockedAt").isEmpty())
                .andExpect(jsonPath("$.data[0].startedAt").isEmpty())
                .andExpect(jsonPath("$.data[0].finishedAt").isEmpty());

        mockMvc.perform(get("/api/knowledge/import-topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.topics[0]").value("用神"))
                .andExpect(jsonPath("$.data.topics[1]").value("世应"))
                .andExpect(jsonPath("$.data.moduleResponsibilities.book").isNotEmpty())
                .andExpect(jsonPath("$.data.moduleResponsibilities.knowledge").isNotEmpty())
                .andExpect(jsonPath("$.data.moduleResponsibilities.task").isNotEmpty());
    }
}
