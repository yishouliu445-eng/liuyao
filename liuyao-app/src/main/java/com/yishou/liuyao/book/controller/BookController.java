package com.yishou.liuyao.book.controller;

import com.yishou.liuyao.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("book-module-ready");
    }
}
