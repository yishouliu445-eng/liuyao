package com.yishou.liuyao.knowledge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.knowledge.domain.BookChunk;
import com.yishou.liuyao.knowledge.repository.BookChunkRepository;
import com.yishou.liuyao.task.domain.DocProcessTask;
import com.yishou.liuyao.task.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.flyway.enabled=true")
class KnowledgeImportExecutionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BookChunkRepository bookChunkRepository;

    @Test
    void shouldRequeueTaskForPythonWorkerInsteadOfProcessingInJava() throws Exception {
        long taskId = createImportRequest("TXT", "/tmp/liuyao-rules.txt", List.of("用神", "世应"));
        DocProcessTask task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus("FAILED");
        task.setErrorMessage("old error");
        task.setProcessorType("PYTHON_WORKER");
        task.setLockedBy("worker-x");
        task.setLockedAt(LocalDateTime.now());
        task.setStartedAt(LocalDateTime.now());
        task.setFinishedAt(LocalDateTime.now());
        taskRepository.save(task);

        mockMvc.perform(post("/api/tasks/doc-process/{taskId}/execute", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdReferenceCount").doesNotExist());

        mockMvc.perform(get("/api/tasks/doc-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].processorType").isEmpty())
                .andExpect(jsonPath("$.data[0].lockedBy").isEmpty())
                .andExpect(jsonPath("$.data[0].lockedAt").isEmpty())
                .andExpect(jsonPath("$.data[0].startedAt").isEmpty())
                .andExpect(jsonPath("$.data[0].finishedAt").isEmpty());
    }

    @Test
    void shouldReadKnowledgeReferencesFromBookChunkCompatibilityView() throws Exception {
        long taskId = createImportRequest("TXT", "/tmp/liuyao-rules.txt", List.of("用神", "世应"));
        DocProcessTask task = taskRepository.findById(taskId).orElseThrow();
        Book book = bookRepository.findById(task.getRefId()).orElseThrow();

        BookChunk chunk = new BookChunk();
        chunk.setBookId(book.getId());
        chunk.setTaskId(task.getId());
        chunk.setChapterTitle("用神总论");
        chunk.setChunkIndex(1);
        chunk.setContent("用神宜旺相，不宜休囚。世应宜分看。");
        chunk.setContentType("rule");
        chunk.setFocusTopic("用神");
        chunk.setTopicTagsJson("[\"用神\",\"世应\"]");
        chunk.setMetadataJson("{\"source\":\"worker\",\"splitReason\":\"trigger_split\",\"splitStrategy\":\"trigger\",\"splitTrigger\":\"断曰\"}");
        chunk.setCharCount(18);
        chunk.setSentenceCount(2);
        chunk.setEmbeddingJson("[0.1,0.2]");
        chunk.setEmbeddingModel("mock-8d-v1");
        chunk.setEmbeddingProvider("mock");
        chunk.setEmbeddingDim(2);
        chunk.setEmbeddingVersion("v1");
        bookChunkRepository.save(chunk);

        mockMvc.perform(get("/api/knowledge/references")
                        .param("topicTag", "用神"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].bookId").value(book.getId()))
                .andExpect(jsonPath("$.data.items[0].taskId").value(task.getId()))
                .andExpect(jsonPath("$.data.items[0].title").value("用神总论"))
                .andExpect(jsonPath("$.data.items[0].topicTag").value("用神"))
                .andExpect(jsonPath("$.data.items[0].sourceType").value("TXT"))
                .andExpect(jsonPath("$.data.items[0].segmentIndex").value(1))
                .andExpect(jsonPath("$.data.items[0].keywordSummary").value("用神、世应"))
                .andExpect(jsonPath("$.data.items[0].content").value(org.hamcrest.Matchers.containsString("用神宜旺相")));
    }

    @Test
    void shouldExposeWorkerTimingDiagnosticsInTaskList() throws Exception {
        long taskId = createImportRequest("TXT", "/tmp/liuyao-rules.txt", List.of("用神"));
        DocProcessTask task = taskRepository.findById(taskId).orElseThrow();
        LocalDateTime lockedAt = LocalDateTime.of(2026, 4, 6, 18, 0, 0);
        LocalDateTime startedAt = LocalDateTime.of(2026, 4, 6, 18, 0, 1);
        LocalDateTime finishedAt = LocalDateTime.of(2026, 4, 6, 18, 0, 2);
        task.setStatus("COMPLETED");
        task.setProcessorType("PYTHON_WORKER");
        task.setLockedBy("python-worker-1");
        task.setLockedAt(lockedAt);
        task.setStartedAt(startedAt);
        task.setFinishedAt(finishedAt);
        taskRepository.save(task);

        mockMvc.perform(get("/api/tasks/doc-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].processorType").value("PYTHON_WORKER"))
                .andExpect(jsonPath("$.data[0].lockedBy").value("python-worker-1"))
                .andExpect(jsonPath("$.data[0].lockedAt").value("2026-04-06T18:00:00"))
                .andExpect(jsonPath("$.data[0].startedAt").value("2026-04-06T18:00:01"))
                .andExpect(jsonPath("$.data[0].finishedAt").value("2026-04-06T18:00:02"));
    }

    @Test
    void shouldExposeDedicatedBookChunkQueryEndpoint() throws Exception {
        long taskId = createImportRequest("TXT", "/tmp/liuyao-rules.txt", List.of("用神", "世应"));
        DocProcessTask task = taskRepository.findById(taskId).orElseThrow();
        Book book = bookRepository.findById(task.getRefId()).orElseThrow();

        BookChunk chunk = new BookChunk();
        chunk.setBookId(book.getId());
        chunk.setTaskId(task.getId());
        chunk.setChapterTitle("世应要诀");
        chunk.setChunkIndex(2);
        chunk.setContent("世为自己，应为对方，宜分开判断。");
        chunk.setContentType("concept");
        chunk.setFocusTopic("世应");
        chunk.setTopicTagsJson("[\"世应\"]");
        chunk.setMetadataJson("{\"source\":\"worker\",\"split_reason\":\"coarse_block\",\"split_strategy\":\"none\",\"split_trigger\":null,\"source_block_index\":3}");
        chunk.setCharCount(16);
        chunk.setSentenceCount(1);
        chunk.setEmbeddingJson("[0.3,0.4]");
        chunk.setEmbeddingModel("mock-8d-v1");
        chunk.setEmbeddingProvider("mock");
        chunk.setEmbeddingDim(2);
        chunk.setEmbeddingVersion("v1");
        bookChunkRepository.save(chunk);

        mockMvc.perform(get("/api/knowledge/chunks")
                        .param("bookId", book.getId().toString())
                        .param("topicTag", "世应"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].bookId").value(book.getId()))
                .andExpect(jsonPath("$.data.items[0].taskId").value(task.getId()))
                .andExpect(jsonPath("$.data.items[0].chapterTitle").value("世应要诀"))
                .andExpect(jsonPath("$.data.items[0].contentType").value("concept"))
                .andExpect(jsonPath("$.data.items[0].focusTopic").value("世应"))
                .andExpect(jsonPath("$.data.items[0].embeddingProvider").value("mock"))
                .andExpect(jsonPath("$.data.items[0].topicTags[0]").value("世应"))
                .andExpect(jsonPath("$.data.items[0].metadata.source").value("worker"))
                .andExpect(jsonPath("$.data.items[0].metadata.splitReason").value("coarse_block"))
                .andExpect(jsonPath("$.data.items[0].metadata.splitStrategy").value("none"))
                .andExpect(jsonPath("$.data.items[0].metadata.sourceBlockIndex").value(3))
                .andExpect(jsonPath("$.data.items[0].metadata.splitTrigger").doesNotExist());
    }

    @Test
    void shouldExposeTaskTimingDiagnosticsWhenPresent() throws Exception {
        long taskId = createImportRequest("TXT", "/tmp/liuyao-rules.txt", List.of("用神"));
        DocProcessTask task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus("COMPLETED");
        task.setProcessorType("PYTHON_WORKER");
        task.setLockedBy("python-worker-1");
        task.setLockedAt(LocalDateTime.of(2026, 4, 6, 16, 30, 0));
        task.setStartedAt(LocalDateTime.of(2026, 4, 6, 16, 30, 1));
        task.setFinishedAt(LocalDateTime.of(2026, 4, 6, 16, 30, 4));
        taskRepository.save(task);

        mockMvc.perform(get("/api/tasks/doc-process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].processorType").value("PYTHON_WORKER"))
                .andExpect(jsonPath("$.data[0].lockedBy").value("python-worker-1"))
                .andExpect(jsonPath("$.data[0].lockedAt").value("2026-04-06T16:30:00"))
                .andExpect(jsonPath("$.data[0].startedAt").value("2026-04-06T16:30:01"))
                .andExpect(jsonPath("$.data[0].finishedAt").value("2026-04-06T16:30:04"));
    }

    private long createImportRequest(String sourceType, String filePath, List<String> topicTags) throws Exception {
        Map<String, Object> request = Map.of(
                "title", "导入测试",
                "author", "测试作者",
                "sourceType", sourceType,
                "filePath", filePath,
                "fileSize", 128,
                "remark", "导入测试",
                "topicTags", topicTags
        );

        String response = mockMvc.perform(post("/api/books/import-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).path("data").path("taskId").asLong();
    }
}
