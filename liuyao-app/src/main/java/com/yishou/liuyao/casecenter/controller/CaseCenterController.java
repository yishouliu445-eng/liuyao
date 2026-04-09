package com.yishou.liuyao.casecenter.controller;

import com.yishou.liuyao.casecenter.dto.CaseDetailDTO;
import com.yishou.liuyao.casecenter.dto.CaseListResponseDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayAssessmentListDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunListDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayRunStatsDTO;
import com.yishou.liuyao.casecenter.dto.CaseSummaryDTO;
import com.yishou.liuyao.casecenter.service.CaseCenterService;
import com.yishou.liuyao.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/{caseId}/replay")
    public ApiResponse<CaseReplayDTO> replayCase(@PathVariable Long caseId) {
        return ApiResponse.success(caseCenterService.replayCase(caseId));
    }

    @PostMapping("/{caseId}/replay-runs")
    public ApiResponse<CaseReplayRunDTO> createReplayRun(@PathVariable Long caseId) {
        return ApiResponse.success(caseCenterService.createReplayRun(caseId));
    }

    @GetMapping("/{caseId}/replay-runs")
    public ApiResponse<List<CaseReplayRunDTO>> listReplayRuns(@PathVariable Long caseId) {
        return ApiResponse.success(caseCenterService.listReplayRuns(caseId));
    }

    @GetMapping("/replay-runs/search")
    public ApiResponse<CaseReplayRunListDTO> searchReplayRuns(@RequestParam(required = false) String questionCategory,
                                                              @RequestParam(required = false) Boolean recommendPersistReplay,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(caseCenterService.listReplayRuns(questionCategory, recommendPersistReplay, page, size));
    }

    @GetMapping("/replay-runs/stats")
    public ApiResponse<CaseReplayRunStatsDTO> getReplayRunStats() {
        return ApiResponse.success(caseCenterService.getReplayRunStats());
    }

    @GetMapping("/replay-assessments")
    public ApiResponse<CaseReplayAssessmentListDTO> listReplayAssessments(@RequestParam(required = false) String questionCategory,
                                                                          @RequestParam(defaultValue = "1") int page,
                                                                          @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(caseCenterService.listReplayPersistenceAssessments(questionCategory, page, size));
    }
}
