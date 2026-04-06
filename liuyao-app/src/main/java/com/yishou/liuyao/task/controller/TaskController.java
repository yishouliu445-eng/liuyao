package com.yishou.liuyao.task.controller;

import com.yishou.liuyao.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("task-module-ready");
    }
}
