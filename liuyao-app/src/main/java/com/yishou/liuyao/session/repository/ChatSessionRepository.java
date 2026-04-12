package com.yishou.liuyao.session.repository;

import com.yishou.liuyao.session.domain.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Page<ChatSession> findByUserIdOrderByLastActiveAtDesc(Long userId, Pageable pageable);

    Page<ChatSession> findByUserIdAndStatusOrderByLastActiveAtDesc(
            Long userId, String status, Pageable pageable);

    /** 查找超过指定时间未活跃的 ACTIVE Session，用于定时关闭 */
    @Query("SELECT s FROM ChatSession s WHERE s.status = 'ACTIVE' AND s.lastActiveAt < :cutoff")
    List<ChatSession> findInactiveSessions(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE ChatSession s SET s.status = 'CLOSED', s.closedAt = :now " +
           "WHERE s.status = 'ACTIVE' AND s.lastActiveAt < :cutoff")
    int closeInactiveSessions(@Param("cutoff") LocalDateTime cutoff, @Param("now") LocalDateTime now);
}
