package com.yishou.liuyao.casecenter.controller;

import com.yishou.liuyao.casecenter.dto.CaseDetailDTO;
import com.yishou.liuyao.casecenter.dto.CaseListResponseDTO;
import com.yishou.liuyao.casecenter.dto.CaseSummaryDTO;
import com.yishou.liuyao.casecenter.service.CaseCenterService;
import com.yishou.liuyao.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cases")
public class CaseCenterController {

    private final CaseCenterService caseCenterService;

    public CaseCenterController(CaseCenterService caseCenterService) {
        this.caseCenterService = caseCenterService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("casecenter-module-ready");
    }

    @GetMapping
    public ApiResponse<List<CaseSummaryDTO>> listRecentCases() {
        return ApiResponse.success(caseCenterService.listRecentCases());
    }

    @GetMapping("/search")
    public ApiResponse<CaseListResponseDTO> listCases(@RequestParam(required = false) String questionCategory,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(caseCenterService.listCases(questionCategory, page, size));
    }

    @GetMapping("/{caseId}")
    public ApiResponse<CaseDetailDTO> getCaseDetail(@PathVariable Long caseId) {
        return ApiResponse.success(caseCenterService.getCaseDetail(caseId));
    }
}
