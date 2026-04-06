package com.yishou.liuyao.knowledge.controller;

import com.yishou.liuyao.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("knowledge-module-ready");
    }
}
