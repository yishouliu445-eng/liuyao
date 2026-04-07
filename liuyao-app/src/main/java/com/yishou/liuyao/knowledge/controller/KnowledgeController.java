package com.yishou.liuyao.knowledge.controller;

import com.yishou.liuyao.common.dto.ApiResponse;
import com.yishou.liuyao.knowledge.dto.BookChunkQueryResponse;
import com.yishou.liuyao.knowledge.dto.KnowledgeReferenceQueryResponse;
import com.yishou.liuyao.knowledge.dto.KnowledgeSearchResponse;
import com.yishou.liuyao.knowledge.service.KnowledgeSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeSearchService knowledgeSearchService;

    public KnowledgeController(KnowledgeSearchService knowledgeSearchService) {
        this.knowledgeSearchService = knowledgeSearchService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("knowledge-module-ready");
    }

    @GetMapping("/import-topics")
    public ApiResponse<KnowledgeSearchResponse> importTopics() {
        return ApiResponse.success(knowledgeSearchService.buildImportTopicsPreview());
    }

    @GetMapping("/references")
    public ApiResponse<KnowledgeReferenceQueryResponse> listReferences(@RequestParam(required = false) String topicTag) {
        return ApiResponse.success(knowledgeSearchService.listReferences(topicTag));
    }

    @GetMapping("/chunks")
    public ApiResponse<BookChunkQueryResponse> listChunks(@RequestParam(required = false) Long bookId,
                                                          @RequestParam(required = false) String topicTag) {
        return ApiResponse.success(knowledgeSearchService.listChunks(bookId, topicTag));
    }

    @GetMapping("/chunks/semantic")
    public ApiResponse<BookChunkQueryResponse> semanticSearchChunks(@RequestParam String queryText,
                                                                    @RequestParam(required = false) Long bookId,
                                                                    @RequestParam(required = false) String topicTag,
                                                                    @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(knowledgeSearchService.semanticSearchChunks(queryText, bookId, topicTag, limit));
    }
}
