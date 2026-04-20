package com.yishou.liuyao.common.exception;

public enum ErrorCode {
    BAD_REQUEST,
    NOT_FOUND,
    INTERNAL_ERROR,

    // Session 模块 (2xxxx)
    SESSION_NOT_FOUND,
    SESSION_ALREADY_CLOSED,
    SESSION_MESSAGE_LIMIT_EXCEEDED,
    DIRECTION_CONFIRMATION_REQUIRED,

    // Calendar 模块 (3xxxx)
    VERIFICATION_EVENT_NOT_FOUND,
    FEEDBACK_ALREADY_SUBMITTED,

    // 限流
    RATE_LIMIT_EXCEEDED
}
