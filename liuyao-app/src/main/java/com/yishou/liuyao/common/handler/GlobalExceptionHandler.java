package com.yishou.liuyao.common.handler;

import com.yishou.liuyao.common.dto.ApiResponse;
import com.yishou.liuyao.common.exception.BusinessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.failure(exception.getErrorCode().name(), exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.failure("INTERNAL_ERROR", exception.getMessage());
    }
}
