package com.yishou.liuyao.book.controller;

import com.yishou.liuyao.book.dto.BookDTO;
import com.yishou.liuyao.book.dto.BookImportRequest;
import com.yishou.liuyao.book.dto.BookImportResponse;
import com.yishou.liuyao.book.service.BookService;
import com.yishou.liuyao.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("book-module-ready");
    }

    @GetMapping
    public ApiResponse<List<BookDTO>> listBooks() {
        return ApiResponse.success(bookService.listRecentBooks());
    }

    @PostMapping("/import-requests")
    public ApiResponse<BookImportResponse> createImportRequest(@Valid @RequestBody BookImportRequest request) {
        return ApiResponse.success(bookService.createImportRequest(request));
    }
}
