package com.yishou.liuyao.common.handler;

import com.yishou.liuyao.common.dto.ApiResponse;
import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(resolveStatus(exception.getErrorCode()))
                .body(ApiResponse.failure(exception.getErrorCode().name(), exception.getMessage(), exception.getData()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("INTERNAL_ERROR", exception.getMessage()));
    }

    private HttpStatus resolveStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND, SESSION_NOT_FOUND, VERIFICATION_EVENT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case SESSION_ALREADY_CLOSED, SESSION_MESSAGE_LIMIT_EXCEEDED, FEEDBACK_ALREADY_SUBMITTED,
                 DIRECTION_CONFIRMATION_REQUIRED -> HttpStatus.CONFLICT;
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
