package com.yishou.liuyao.analysis.controller;

import com.yishou.liuyao.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("analysis-module-ready");
    }
}
