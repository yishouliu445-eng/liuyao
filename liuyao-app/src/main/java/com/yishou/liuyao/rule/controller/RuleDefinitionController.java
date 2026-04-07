package com.yishou.liuyao.rule.controller;

import com.yishou.liuyao.common.dto.ApiResponse;
import com.yishou.liuyao.rule.dto.RuleDefinitionListResponse;
import com.yishou.liuyao.rule.service.RuleDefinitionCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
public class RuleDefinitionController {

    private final RuleDefinitionCatalogService ruleDefinitionCatalogService;

    public RuleDefinitionController(RuleDefinitionCatalogService ruleDefinitionCatalogService) {
        this.ruleDefinitionCatalogService = ruleDefinitionCatalogService;
    }

    @GetMapping("/definitions")
    public ApiResponse<RuleDefinitionListResponse> listDefinitions() {
        return ApiResponse.success(ruleDefinitionCatalogService.listDefinitions());
    }
}
