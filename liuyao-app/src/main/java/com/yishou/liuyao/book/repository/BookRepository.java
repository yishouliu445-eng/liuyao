package com.yishou.liuyao.book.repository;

import com.yishou.liuyao.book.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findTop20ByOrderByIdDesc();
}
