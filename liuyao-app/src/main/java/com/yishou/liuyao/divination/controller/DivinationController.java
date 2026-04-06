package com.yishou.liuyao.divination.controller;

import com.yishou.liuyao.common.dto.ApiResponse;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeResponse;
import com.yishou.liuyao.divination.service.DivinationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/divinations")
public class DivinationController {

    private final DivinationService divinationService;

    public DivinationController(DivinationService divinationService) {
        this.divinationService = divinationService;
    }

    @PostMapping("/analyze")
    public ApiResponse<DivinationAnalyzeResponse> analyze(@Valid @RequestBody DivinationAnalyzeRequest request) {
        return ApiResponse.success(divinationService.analyze(request));
    }
}
