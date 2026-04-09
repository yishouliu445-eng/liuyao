package com.yishou.liuyao.rule.controller;

import com.yishou.liuyao.rule.dto.RuleCandidateDTO;
import com.yishou.liuyao.rule.dto.RuleCandidateSearchRequest;
import com.yishou.liuyao.rule.service.RuleCandidateService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules/candidates")
public class RuleCandidateController {

    private final RuleCandidateService ruleCandidateService;

    public RuleCandidateController(RuleCandidateService ruleCandidateService) {
        this.ruleCandidateService = ruleCandidateService;
    }

    @GetMapping
    public Page<RuleCandidateDTO> searchCandidates(RuleCandidateSearchRequest request) {
        return ruleCandidateService.searchCandidates(request);
    }

    @PostMapping("/{id}/review")
    public RuleCandidateDTO reviewCandidate(@PathVariable("id") Long id, @RequestParam("action") String action) {
        return ruleCandidateService.reviewCandidate(id, action);
    }
}
