package com.yishou.liuyao.task.service;

import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.service.BookService;
import com.yishou.liuyao.task.domain.DocProcessTask;
import com.yishou.liuyao.task.dto.TaskExecutionResponse;
import com.yishou.liuyao.task.dto.TaskSummaryDTO;
import com.yishou.liuyao.task.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final BookService bookService;

    public TaskService(TaskRepository taskRepository,
                       BookService bookService) {
        this.taskRepository = taskRepository;
        this.bookService = bookService;
    }

    @Transactional(readOnly = true)
    public List<TaskSummaryDTO> listRecentDocProcessTasks() {
        return taskRepository.findTop20ByOrderByIdDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public TaskExecutionResponse executeDocProcessTask(Long taskId) {
        DocProcessTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("任务不存在: " + taskId));
        Book book = bookService.getBook(task.getRefId());

        task.setStatus("PENDING");
        task.setErrorMessage(null);
        task.setProcessorType(null);
        task.setLockedBy(null);
        task.setLockedAt(null);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        taskRepository.save(task);
        bookService.updateParseStatus(book.getId(), "PENDING");
        log.info("文档解析任务已重新入队，等待 Python worker 处理: taskId={}, bookId={}",
                task.getId(),
                book.getId());

        TaskExecutionResponse response = new TaskExecutionResponse();
        response.setTaskId(task.getId());
        response.setStatus(task.getStatus());
        response.setCreatedReferenceCount(null);
        return response;
    }

    private TaskSummaryDTO toDto(DocProcessTask task) {
        TaskSummaryDTO dto = new TaskSummaryDTO();
        dto.setId(task.getId());
        dto.setTaskType(task.getTaskType());
        dto.setRefId(task.getRefId());
        dto.setStatus(task.getStatus());
        dto.setRetryCount(task.getRetryCount());
        dto.setErrorMessage(task.getErrorMessage());
        dto.setPayloadJson(task.getPayloadJson());
        dto.setProcessorType(task.getProcessorType());
        dto.setLockedBy(task.getLockedBy());
        dto.setLockedAt(task.getLockedAt());
        dto.setStartedAt(task.getStartedAt());
        dto.setFinishedAt(task.getFinishedAt());
        return dto;
    }
}
