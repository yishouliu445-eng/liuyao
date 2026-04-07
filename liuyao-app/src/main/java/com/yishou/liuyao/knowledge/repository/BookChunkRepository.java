package com.yishou.liuyao.knowledge.repository;

import com.yishou.liuyao.knowledge.domain.BookChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookChunkRepository extends JpaRepository<BookChunk, Long> {

    List<BookChunk> findTop20ByOrderByIdDesc();

    List<BookChunk> findTop20ByFocusTopicOrderByIdDesc(String focusTopic);

    List<BookChunk> findTop50ByBookIdOrderByChunkIndexAsc(Long bookId);

    List<BookChunk> findTop50ByBookIdAndFocusTopicOrderByChunkIndexAsc(Long bookId, String focusTopic);
}
