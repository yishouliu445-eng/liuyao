package com.yishou.liuyao.session.repository;

import com.yishou.liuyao.session.domain.ChatMessage;
import com.yishou.liuyao.session.domain.ChatSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestPropertySource(properties = "spring.flyway.enabled=true")
class ChatSessionRepositoryTest {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldFindAndCloseInactiveSessions() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 12, 22, 0);
        ChatSession activeRecent = saveSession("最近活跃", "ACTIVE", now.minusHours(1));
        ChatSession activeExpired = saveSession("超时会话", "ACTIVE", now.minusHours(30));
        saveSession("已关闭会话", "CLOSED", now.minusHours(30));

        List<ChatSession> inactiveSessions = chatSessionRepository.findInactiveSessions(now.minusHours(24));

        assertEquals(1, inactiveSessions.size());
        assertEquals(activeExpired.getId(), inactiveSessions.get(0).getId());

        int closedCount = chatSessionRepository.closeInactiveSessions(now.minusHours(24), now);

        assertEquals(1, closedCount);
        entityManager.flush();
        entityManager.clear();
        ChatSession reloadedExpired = chatSessionRepository.findById(activeExpired.getId()).orElseThrow();
        ChatSession reloadedRecent = chatSessionRepository.findById(activeRecent.getId()).orElseThrow();
        assertEquals("CLOSED", reloadedExpired.getStatus());
        assertNotNull(reloadedExpired.getClosedAt());
        assertEquals("ACTIVE", reloadedRecent.getStatus());
    }

    @Test
    void shouldQueryMessagesBySessionInExpectedOrder() {
        ChatSession session = saveSession("消息排序测试", "ACTIVE", LocalDateTime.of(2026, 4, 12, 10, 0));
        ChatMessage first = chatMessageRepository.save(ChatMessage.userMessage(session.getId(), "第一条"));
        ChatMessage second = chatMessageRepository.save(ChatMessage.assistantMessage(
                session.getId(), "第二条", null, "mock", 0, 0));
        ChatMessage third = chatMessageRepository.save(ChatMessage.userMessage(session.getId(), "第三条"));

        updateMessageCreatedAt(first, LocalDateTime.of(2026, 4, 12, 10, 0, 1));
        updateMessageCreatedAt(second, LocalDateTime.of(2026, 4, 12, 10, 0, 2));
        updateMessageCreatedAt(third, LocalDateTime.of(2026, 4, 12, 10, 0, 3));

        List<ChatMessage> orderedMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessages(session.getId(), 2);

        assertEquals(3, orderedMessages.size());
        assertEquals("第一条", orderedMessages.get(0).getContent());
        assertEquals("第二条", orderedMessages.get(1).getContent());
        assertEquals("第三条", orderedMessages.get(2).getContent());

        assertEquals(2, recentMessages.size());
        assertEquals("第三条", recentMessages.get(0).getContent());
        assertEquals("第二条", recentMessages.get(1).getContent());
        assertEquals(3L, chatMessageRepository.countBySessionId(session.getId()));
    }

    private ChatSession saveSession(String question, String status, LocalDateTime lastActiveAt) {
        ChatSession session = ChatSession.create(1001L, null, null, question, "测试");
        session.setStatus(status);
        session = chatSessionRepository.save(session);
        session.setLastActiveAt(lastActiveAt);
        return chatSessionRepository.save(session);
    }

    private void updateMessageCreatedAt(ChatMessage message, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE chat_message SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(createdAt),
                message.getId()
        );
    }
}
