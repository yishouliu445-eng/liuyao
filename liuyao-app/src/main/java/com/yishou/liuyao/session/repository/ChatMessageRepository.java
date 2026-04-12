package com.yishou.liuyao.session.repository;

import com.yishou.liuyao.session.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    /** 最近 N 条消息，用于构建上下文窗口 */
    @Query("SELECT m FROM ChatMessage m WHERE m.sessionId = :sessionId " +
           "ORDER BY m.createdAt DESC LIMIT :limit")
    List<ChatMessage> findRecentMessages(@Param("sessionId") UUID sessionId,
                                         @Param("limit") int limit);

    long countBySessionId(UUID sessionId);
}
