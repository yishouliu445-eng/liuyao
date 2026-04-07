package com.yishou.liuyao.knowledge.repository;

import com.yishou.liuyao.knowledge.domain.KnowledgeReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeReferenceRepository extends JpaRepository<KnowledgeReference, Long> {

    List<KnowledgeReference> findTop20ByOrderByIdDesc();

    List<KnowledgeReference> findTop20ByTopicTagOrderByIdDesc(String topicTag);

    void deleteByBookId(Long bookId);
}
