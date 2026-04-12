package com.yishou.liuyao.session.controller;

import com.yishou.liuyao.common.dto.ApiResponse;
import com.yishou.liuyao.common.dto.PageResult;
import com.yishou.liuyao.session.domain.ChatMessage;
import com.yishou.liuyao.session.domain.ChatSession;
import com.yishou.liuyao.session.dto.*;
import com.yishou.liuyao.session.service.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Session API 控制器。
 *
 * <p>提供起卦建立会话、多轮追问、历史查询等端点。</p>
 */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * POST /api/sessions
     * 起卦，创建新会话，返回排盘+分析结果。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SessionCreateResponse> createSession(
            @RequestBody SessionCreateRequest request) {
        SessionCreateResponse response = sessionService.createSession(request);
        return ApiResponse.success(response);
    }

    /**
     * POST /api/sessions/{sessionId}/messages
     * 追问，在已有会话中新增用户消息并获取AI回复。
     */
    @PostMapping("/{sessionId}/messages")
    public ApiResponse<MessageResponse> addMessage(
            @PathVariable UUID sessionId,
            @RequestBody MessageRequest request) {
        MessageResponse response = sessionService.addMessage(sessionId, request);
        return ApiResponse.success(response);
    }

    /**
     * GET /api/sessions/{sessionId}
     * 获取会话详情。
     */
    @GetMapping("/{sessionId}")
    public ApiResponse<SessionDetailResponse> getSession(@PathVariable UUID sessionId) {
        SessionDetailResponse session = sessionService.getSession(sessionId);
        return ApiResponse.success(session);
    }

    /**
     * GET /api/sessions/{sessionId}/messages
     * 获取会话的消息历史列表。
     */
    @GetMapping("/{sessionId}/messages")
    public ApiResponse<List<ChatMessage>> getMessages(@PathVariable UUID sessionId) {
        return ApiResponse.success(sessionService.getMessages(sessionId));
    }

    /**
     * GET /api/sessions?userId=xxx&page=0&size=10
     * 分页查询用户的历史会话列表。
     */
    @GetMapping
    public ApiResponse<PageResult<ChatSession>> listSessions(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResult<ChatSession> result = sessionService.listSessions(userId, page, size);
        return ApiResponse.success(result);
    }

    /**
     * DELETE /api/sessions/{sessionId}
     * 关闭会话。
     */
    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> closeSession(@PathVariable UUID sessionId) {
        sessionService.closeSession(sessionId);
        return ApiResponse.success(null);
    }
}
