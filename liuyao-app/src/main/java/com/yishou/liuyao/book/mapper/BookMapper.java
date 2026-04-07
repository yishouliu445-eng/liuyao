package com.yishou.liuyao.book.mapper;

import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.dto.BookDTO;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookDTO toDto(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setAuthor(book.getAuthor());
        dto.setSourceType(book.getSourceType());
        dto.setFilePath(book.getFilePath());
        dto.setFileSize(book.getFileSize());
        dto.setParseStatus(book.getParseStatus());
        dto.setRemark(book.getRemark());
        dto.setCreatedAt(book.getCreatedAt());
        return dto;
    }
}
