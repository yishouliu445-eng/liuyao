package com.yishou.liuyao.task.controller;

import com.yishou.liuyao.common.dto.ApiResponse;
import com.yishou.liuyao.task.dto.TaskExecutionResponse;
import com.yishou.liuyao.task.dto.TaskSummaryDTO;
import com.yishou.liuyao.task.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("task-module-ready");
    }

    @GetMapping("/doc-process")
    public ApiResponse<List<TaskSummaryDTO>> listDocProcessTasks() {
        return ApiResponse.success(taskService.listRecentDocProcessTasks());
    }

    @PostMapping("/doc-process/{taskId}/execute")
    public ApiResponse<TaskExecutionResponse> executeDocProcessTask(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.executeDocProcessTask(taskId));
    }
}
